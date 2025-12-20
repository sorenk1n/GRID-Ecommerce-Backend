package com.khomsi.backend.main.checkout.model.enums;

import jakarta.servlet.http.HttpServletRequest;

public enum PaymentEndpoints {
    STRIPE_SUCCESS("/stripe/success"),
    STRIPE_FAILED("/stripe/failed"),
    PAYPAL_SUCCESS("/paypal/success"),
    PAYPAL_CANCEL("/payment/cancel"),
    ALIPAY_RETURN("/alipay/return"),
    ALIPAY_NOTIFY("/api/v1/checkout/alipay/notify", false);
    private final String path;
    private final boolean useOrigin;

    PaymentEndpoints(String path) {
        this(path, true);
    }

    PaymentEndpoints(String path, boolean useOrigin) {
        this.path = path;
        this.useOrigin = useOrigin;
    }

    public static String createUrl(HttpServletRequest baseUrl, PaymentEndpoints endpoint) {
        if (!endpoint.useOrigin) {
            String requestUrl = baseUrl.getRequestURL().toString();
            String base = requestUrl.replace(baseUrl.getRequestURI(), "");
            return base + endpoint.path;
        }
        String origin = baseUrl.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            String port = baseUrl.getServerPort() == 80 || baseUrl.getServerPort() == 443 ? "" :
                    ":" + baseUrl.getServerPort();
            origin = baseUrl.getScheme() + "://" + baseUrl.getServerName() + port;
        }
        return origin + endpoint.path;
    }
}
