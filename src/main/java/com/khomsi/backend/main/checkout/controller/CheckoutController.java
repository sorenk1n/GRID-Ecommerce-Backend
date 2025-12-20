package com.khomsi.backend.main.checkout.controller;

import com.khomsi.backend.main.checkout.apis.AlipayService;
import com.khomsi.backend.main.checkout.apis.impl.LocalPaymentService;
import com.khomsi.backend.main.checkout.model.dto.stripe.PaymentResponse;
import com.khomsi.backend.main.checkout.model.enums.BalanceAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import static com.khomsi.backend.config.ApplicationConfig.BEARER_KEY_SECURITY_SCHEME;

@RestController
@Tag(name = "Checkout", description = "CRUD operation for Checkout Controller")
@RequestMapping("/api/v1/checkout")
@Validated
@RequiredArgsConstructor
public class CheckoutController {
    private final AlipayService alipayService;
    private final LocalPaymentService localPaymentService;

    @PostMapping("/balance/create-payment")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)},
            summary = "Local endpoint to create payment")
    public ResponseEntity<PaymentResponse> checkoutLocal() {
        PaymentResponse paymentResponse = localPaymentService.createPayment();
        return ResponseEntity
                .status(paymentResponse.httpStatus())
                .body(paymentResponse);
    }

    @PostMapping("/balance/capture-payment")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)},
            summary = "Local endpoint to capture payment")
    public ResponseEntity<PaymentResponse> captureLocal(@RequestParam("sessionId") String sessionId) {
        PaymentResponse paymentResponse = localPaymentService.capturePayment(sessionId);
        return ResponseEntity
                .status(paymentResponse.httpStatus())
                .body(paymentResponse);
    }

    @PostMapping("/recharge/alipay/create-payment")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)},
            summary = "支付宝余额充值预下单")
    public ResponseEntity<PaymentResponse> balanceRechargeAlipay(@RequestParam("amount") BigDecimal amount,
                                                                 HttpServletRequest request) {
        PaymentResponse paymentResponse = alipayService.createBalanceRecharge(amount, request);
        return ResponseEntity
                .status(paymentResponse.httpStatus())
                .body(paymentResponse);
    }

    @PostMapping("/alipay/create-payment")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)},
            summary = "支付宝创建订单并返回支付二维码/链接")
    public ResponseEntity<PaymentResponse> checkoutAlipay(@RequestParam("balanceAction") BalanceAction balanceAction,
                                                          HttpServletRequest request) {
        PaymentResponse paymentResponse = alipayService.createPayment(balanceAction, request);
        return ResponseEntity
                .status(paymentResponse.httpStatus())
                .body(paymentResponse);
    }

    // Check and place the order if success
    @PostMapping("/alipay/capture-payment")
    @Operation(security = {@SecurityRequirement(name = BEARER_KEY_SECURITY_SCHEME)},
            summary = "支付宝查单并确认支付状态")
    public ResponseEntity<PaymentResponse> placeAlipayOrder(@RequestParam("sessionId") String sessionId) {
        PaymentResponse paymentResponse = alipayService.capturePayment(sessionId);
        return ResponseEntity
                .status(paymentResponse.httpStatus())
                .body(paymentResponse);
    }

    @PostMapping("/alipay/notify")
    @Operation(summary = "支付宝异步通知回调，无需鉴权")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        boolean handled = alipayService.handleAsyncNotify(params);
        return handled ? "success" : "failure";
    }
}
