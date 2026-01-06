package com.khomsi.backend.main.checkout.apis.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.khomsi.backend.additional.cart.model.dto.CartDTO;
import com.khomsi.backend.additional.cart.model.dto.CartItemDto;
import com.khomsi.backend.additional.cart.service.CartService;
import com.khomsi.backend.main.checkout.apis.AlipayService;
import com.khomsi.backend.main.checkout.apis.config.AlipayProperties;
import com.khomsi.backend.main.checkout.model.dto.alipay.AlipayCapturePaymentResponse;
import com.khomsi.backend.main.checkout.model.dto.alipay.AlipayCreatePaymentResponse;
import com.khomsi.backend.main.checkout.model.dto.stripe.PaymentResponse;
import com.khomsi.backend.main.checkout.model.enums.BalanceAction;
import com.khomsi.backend.main.checkout.model.enums.PaymentEndpoints;
import com.khomsi.backend.main.checkout.model.enums.PaymentMethod;
import com.khomsi.backend.main.checkout.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.khomsi.backend.main.checkout.apis.impl.ApiResponseBuilder.buildFailureResponse;
import static com.khomsi.backend.main.checkout.apis.impl.ApiResponseBuilder.buildResponse;
import static com.khomsi.backend.main.checkout.model.enums.PaymentEndpoints.createUrl;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlipayServiceImpl implements AlipayService {
    private static final Set<String> SUCCESS_STATUSES = Set.of("TRADE_SUCCESS", "TRADE_FINISHED");
    private final TransactionService transactionService;
    private final CartService cartService;
    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Override
    public PaymentResponse createBalanceRecharge(BigDecimal amount, HttpServletRequest request) {
        return createPreOrder(amount, BalanceAction.BALANCE_RECHARGE, request, null);
    }

    @Override
    public PaymentResponse createPayment(BalanceAction balanceAction, HttpServletRequest request) {
        CartDTO cartDto = cartService.cartItems();
        List<CartItemDto> cartItemDtoList = cartDto.cartItems();
        if (cartItemDtoList.isEmpty() || balanceAction == BalanceAction.BALANCE_RECHARGE) {
            return buildFailureResponse("购物车为空或支付方式不正确", HttpStatus.BAD_REQUEST);
        }
        return createPreOrder(null, balanceAction, request, cartItemDtoList);
    }

    private PaymentResponse createPreOrder(BigDecimal amount, BalanceAction balanceAction,
                                           HttpServletRequest request, List<CartItemDto> cartItemDtoList) {
        if (balanceAction == BalanceAction.BALANCE_RECHARGE &&
                (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)) {
            return buildFailureResponse("充值金额不能为空或小于等于0", HttpStatus.BAD_REQUEST);
        }
        try {
            BigDecimal totalAmount = transactionService.calculateTotalAmount(amount, balanceAction, cartItemDtoList)
                    .setScale(2, RoundingMode.HALF_UP);
            String outTradeNo = UUID.randomUUID().toString().replace("-", "");
            String subject = buildSubject(balanceAction, cartItemDtoList);

            String qrCode = placeProviderPreOrder(outTradeNo, subject, totalAmount, request);
            if (!StringUtils.hasText(qrCode)) {
                return buildFailureResponse("服务商预下单失败，请重试", HttpStatus.BAD_REQUEST);
            }

            AlipayCreatePaymentResponse responseData = AlipayCreatePaymentResponse.builder()
                    .outTradeNo(outTradeNo)
                    .qrCode(qrCode)
                    .subject(subject)
                    .build();
            transactionService.placeTemporaryTransaction(totalAmount, outTradeNo, qrCode,
                    balanceAction, PaymentMethod.ALIPAY);
            return buildResponse(responseData, "支付宝预下单成功");
        } catch (Exception e) {
            log.error("支付宝预下单异常", e);
            return buildFailureResponse("支付宝预下单失败，服务器异常", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public PaymentResponse capturePayment(String outTradeNo) {
        try {
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            queryRequest.setBizContent(objectMapper.writeValueAsString(Map.of("out_trade_no", outTradeNo)));

            AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);
            if (response == null || !response.isSuccess()) {
                log.error("支付宝查单失败，outTradeNo={}, response={}", outTradeNo, response);
                return buildFailureResponse("支付宝查询支付状态失败", HttpStatus.BAD_REQUEST);
            }
            String tradeStatus = response.getTradeStatus();
            AlipayCapturePaymentResponse responseData = AlipayCapturePaymentResponse.builder()
                    .outTradeNo(outTradeNo)
                    .tradeNo(response.getTradeNo())
                    .tradeStatus(tradeStatus)
                    .build();
            if (SUCCESS_STATUSES.contains(tradeStatus)) {
                transactionService.completeTransaction(outTradeNo);
                return buildResponse(responseData, "支付宝支付成功");
            }
            return buildFailureResponse("支付宝未完成支付，当前状态：" + tradeStatus,
                    HttpStatus.BAD_REQUEST, responseData);
        } catch (Exception e) {
            log.error("支付宝查单异常", e);
            return buildFailureResponse("支付宝查询失败，服务器异常", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean handleAsyncNotify(Map<String, String> notifyParams) {
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    notifyParams,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType());
            if (!signVerified) {
                log.warn("支付宝回调验签失败，payload={}", notifyParams);
                return false;
            }
            String tradeStatus = notifyParams.get("trade_status");
            String outTradeNo = notifyParams.get("out_trade_no");
            if (SUCCESS_STATUSES.contains(tradeStatus)) {
                transactionService.completeTransaction(outTradeNo);
            } else {
                log.info("收到支付宝异步通知，状态={}，outTradeNo={}", tradeStatus, outTradeNo);
            }
            return true;
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知验签异常", e);
            return false;
        }
    }

    private String buildSubject(BalanceAction balanceAction, List<CartItemDto> cartItemDtoList) {
        if (balanceAction == BalanceAction.BALANCE_RECHARGE) {
            return "GRID余额充值";
        }
        if (cartItemDtoList == null || cartItemDtoList.isEmpty()) {
            return "GRID订单支付";
        }
        return cartItemDtoList.stream()
                .map(cartItem -> cartItem.game().title())
                .collect(Collectors.joining(", "));
    }

    private String placeProviderPreOrder(String outTradeNo, String subject,
                                         BigDecimal totalAmount, HttpServletRequest request) {
        String validationError = validateProviderConfig();
        if (validationError != null) {
            log.error("服务商支付配置缺失：{}", validationError);
            return null;
        }
        Map<String, String> formPayload = buildProviderForm(outTradeNo, subject, totalAmount, request);
        MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
        formBody.setAll(formPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                alipayProperties.getServiceProviderUrl(),
                entity,
                String.class
        );
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            log.error("服务商预下单失败，status={}, body={}", response.getStatusCode(), response.getBody());
            return null;
        }
        try {
            Map<String, Object> responseBody = objectMapper.readValue(
                    response.getBody(), new TypeReference<>() {});
            String qrCode = extractPaymentUrl(responseBody);
            if (!StringUtils.hasText(qrCode)) {
                log.error("服务商响应未包含支付二维码/链接，payload={}", responseBody);
                return null;
            }
            return qrCode;
        } catch (Exception e) {
            log.error("解析服务商响应失败，body={}", response.getBody(), e);
            return null;
        }
    }

    private String resolveNotifyUrl(HttpServletRequest request) {
        if (StringUtils.hasText(alipayProperties.getNotifyUrl())) {
            return alipayProperties.getNotifyUrl();
        }
        return createUrl(request, PaymentEndpoints.ALIPAY_NOTIFY);
    }

    private Map<String, String> buildProviderForm(String outTradeNo, String subject,
                                                  BigDecimal totalAmount, HttpServletRequest request) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("payChannel", String.valueOf(alipayProperties.getPayChannel()));
        form.put("typeIndex", String.valueOf(alipayProperties.getTypeIndex()));
        form.put("externalId", alipayProperties.getExternalId());
        form.put("merchantTradeNo", outTradeNo);
        form.put("totalAmount", totalAmount.toPlainString());
        form.put("merchantSubject", subject);
        form.put("externalGoodsType", String.valueOf(alipayProperties.getExternalGoodsType()));
        form.put("merchantPayNotifyUrl", resolveNotifyUrl(request));
        form.put("quitUrl", resolveQuitUrl(request));
        form.put("returnUrl", resolveReturnUrl(request));
        form.put("clientIp", resolveClientIp(request));
        form.put("riskControlNotifyUrl", resolveRiskControlNotifyUrl(request));
        return form;
    }

    private String resolveQuitUrl(HttpServletRequest request) {
        if (StringUtils.hasText(alipayProperties.getQuitUrl())) {
            return alipayProperties.getQuitUrl();
        }
        return resolveReturnUrl(request);
    }

    private String resolveReturnUrl(HttpServletRequest request) {
        if (StringUtils.hasText(alipayProperties.getReturnUrl())) {
            return alipayProperties.getReturnUrl();
        }
        return createUrl(request, PaymentEndpoints.ALIPAY_RETURN);
    }

    private String resolveRiskControlNotifyUrl(HttpServletRequest request) {
        if (StringUtils.hasText(alipayProperties.getRiskControlNotifyUrl())) {
            return alipayProperties.getRiskControlNotifyUrl();
        }
        return resolveNotifyUrl(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String validateProviderConfig() {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(alipayProperties.getServiceProviderUrl())) {
            missing.add("serviceProviderUrl");
        }
        if (!StringUtils.hasText(alipayProperties.getExternalId())) {
            missing.add("externalId");
        }
        if (alipayProperties.getPayChannel() == null) {
            missing.add("payChannel");
        }
        if (alipayProperties.getTypeIndex() == null) {
            missing.add("typeIndex");
        }
        if (alipayProperties.getExternalGoodsType() == null) {
            missing.add("externalGoodsType");
        }
        return missing.isEmpty() ? null : String.join(", ", missing);
    }

    private String extractPaymentUrl(Map<String, Object> responseBody) {
        String direct = findFirstValue(responseBody);
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        Object data = responseBody.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return findFirstValue(castToStringObjectMap(dataMap));
        }
        return null;
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result;
    }

    private String findFirstValue(Map<String, Object> payload) {
        String[] keys = new String[] {
                "qrCode", "qr_code", "codeUrl", "code_url",
                "payUrl", "pay_url", "paymentUrl", "payment_url"
        };
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }
}
