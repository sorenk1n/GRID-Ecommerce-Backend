package com.khomsi.backend.main.checkout.apis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
/**
 * 支付宝支付配置属性类
 * 用于绑定配置文件中以"app.payment.alipay"为前缀的配置项
 * 通过@ConfigurationProperties注解将外部配置映射到该类的属性上
 */
@ConfigurationProperties(prefix = "app.payment.alipay")

public class AlipayProperties {
    private String externalId ;
    /**
     * 支付宝开放平台网关地址，例如：https://openapi.alipay.com/gateway.do
     */
    private String gatewayUrl;
    /**
     * 服务商支付通道地址（表单提交）。
     */
    private String serviceProviderUrl;
    /**
     * 支付通道类型。
     */
    private Integer payChannel;
    /**
     * 支付类型索引。
     */
    private Integer typeIndex;
    /**
     * 外部商品类型。
     */
    private Integer externalGoodsType;
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
     * 风控回调地址（服务商需要时使用）。
     */
    private String riskControlNotifyUrl;
    /**
     * 取消支付跳转地址（服务商需要时使用）。
     */
    private String quitUrl;
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
