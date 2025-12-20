package com.khomsi.backend.main.checkout.model.dto.alipay;

import lombok.Builder;

@Builder
public record AlipayCreatePaymentResponse(String outTradeNo, String qrCode, String subject) {
}
