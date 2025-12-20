package com.khomsi.backend.main.checkout.apis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.alipay")
public class AlipayProperties {
    /**
     * 支付宝开放平台网关地址，例如：https://openapi.alipay.com/gateway.do
     */
    private String gatewayUrl;
    /**
     * 服务商应用的 AppId。
     */
    private String appId;
    /**
     * 服务商应用的私钥（PKCS8）。
     */
    private String privateKey;
    /**
     * 支付宝公钥，用于验签回调。
     */
    private String alipayPublicKey;
    /**
     * 回调签名使用的字符集，默认 UTF-8。
     */
    private String charset = "UTF-8";
    /**
     * 报文格式，默认 json。
     */
    private String format = "json";
    /**
     * 签名算法，默认 RSA2。
     */
    private String signType = "RSA2";
    /**
     * 支付宝异步通知地址，必须为公网可访问地址。
     */
    private String notifyUrl;
    /**
     * 用户支付完成后跳转的前端地址（可选）。
     */
    private String returnUrl;
    /**
     * 直付通二级商户号（SMID）。
     */
    private String subMerchantId;
    /**
     * 订单过期时间，默认 15m。
     */
    private String timeoutExpress = "15m";
    /**
     * 产品码，不同支付场景可调整，默认 FACE_TO_FACE_PAYMENT（扫码预下单）。
     */
    private String productCode = "FACE_TO_FACE_PAYMENT";
}
