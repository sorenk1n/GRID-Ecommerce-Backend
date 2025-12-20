package com.khomsi.backend.main.checkout.model.dto.alipay;

import lombok.Builder;

@Builder
public record AlipayCapturePaymentResponse(String outTradeNo, String tradeNo, String tradeStatus) {
}
