package com.khomsi.backend.main.checkout.apis;

import com.khomsi.backend.main.checkout.model.dto.stripe.PaymentResponse;
import com.khomsi.backend.main.checkout.model.enums.BalanceAction;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;

public interface AlipayService {
    PaymentResponse createBalanceRecharge(BigDecimal amount, HttpServletRequest request);

    PaymentResponse createPayment(BalanceAction balanceAction, HttpServletRequest request);

    PaymentResponse capturePayment(String outTradeNo);

    boolean handleAsyncNotify(Map<String, String> notifyParams);
}
