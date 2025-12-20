package com.khomsi.backend.main.checkout.apis.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

            AlipayTradePrecreateRequest precreateRequest = new AlipayTradePrecreateRequest();
            precreateRequest.setNotifyUrl(resolveNotifyUrl(request));
            precreateRequest.setBizContent(buildPrecreateBizContent(outTradeNo, subject, totalAmount));

            AlipayTradePrecreateResponse response = alipayClient.execute(precreateRequest);
            if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getQrCode())) {
                log.error("支付宝预下单失败，response={}", response);
                return buildFailureResponse("支付宝预下单失败，请重试", HttpStatus.BAD_REQUEST);
            }

            AlipayCreatePaymentResponse responseData = AlipayCreatePaymentResponse.builder()
                    .outTradeNo(outTradeNo)
                    .qrCode(response.getQrCode())
                    .subject(subject)
                    .build();
            transactionService.placeTemporaryTransaction(totalAmount, outTradeNo, response.getQrCode(),
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

    private String buildPrecreateBizContent(String outTradeNo, String subject, BigDecimal totalAmount) throws Exception {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("subject", subject);
        bizContent.put("total_amount", totalAmount.toPlainString());
        bizContent.put("timeout_express", alipayProperties.getTimeoutExpress());
        bizContent.put("product_code", alipayProperties.getProductCode());
        if (StringUtils.hasText(alipayProperties.getSubMerchantId())) {
            Map<String, String> subMerchant = new LinkedHashMap<>();
            subMerchant.put("merchant_id", alipayProperties.getSubMerchantId());
            subMerchant.put("merchant_type", "SMID");
            bizContent.put("sub_merchant", subMerchant);
        }
        return objectMapper.writeValueAsString(bizContent);
    }

    private String resolveNotifyUrl(HttpServletRequest request) {
        if (StringUtils.hasText(alipayProperties.getNotifyUrl())) {
            return alipayProperties.getNotifyUrl();
        }
        return createUrl(request, PaymentEndpoints.ALIPAY_NOTIFY);
    }
}
