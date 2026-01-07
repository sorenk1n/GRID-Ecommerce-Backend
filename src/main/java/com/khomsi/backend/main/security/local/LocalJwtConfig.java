package com.khomsi.backend.main.security.local;

import com.khomsi.backend.main.security.config.KeycloakAuthProperties;
import com.khomsi.backend.main.security.config.ResourceServerJwtProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableConfigurationProperties({LocalAuthProperties.class, KeycloakAuthProperties.class, ResourceServerJwtProperties.class})
public class LocalJwtConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth.local", name = "enabled", havingValue = "true")
    public JwtEncoder localJwtEncoder(LocalAuthProperties localAuthProperties) {
        SecretKey secretKey = buildSecretKey(localAuthProperties);
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth.local", name = "enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(LocalAuthProperties localAuthProperties,
                                 KeycloakAuthProperties keycloakAuthProperties,
                                 ResourceServerJwtProperties resourceServerJwtProperties) {
        JwtDecoder localDecoder = NimbusJwtDecoder.withSecretKey(buildSecretKey(localAuthProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        if (!keycloakAuthProperties.isEnabled()) {
            return localDecoder;
        }

        JwtDecoder keycloakDecoder = buildKeycloakDecoder(resourceServerJwtProperties);
        return new CompositeJwtDecoder(List.of(localDecoder, keycloakDecoder));
    }

    private JwtDecoder buildKeycloakDecoder(ResourceServerJwtProperties resourceServerJwtProperties) {
        if (StringUtils.hasText(resourceServerJwtProperties.getIssuerUri())) {
            return JwtDecoders.fromIssuerLocation(resourceServerJwtProperties.getIssuerUri());
        }
        if (StringUtils.hasText(resourceServerJwtProperties.getJwkSetUri())) {
            return NimbusJwtDecoder.withJwkSetUri(resourceServerJwtProperties.getJwkSetUri()).build();
        }
        throw new IllegalStateException("Keycloak JWT configuration is missing issuer-uri or jwk-set-uri.");
    }

    private SecretKey buildSecretKey(LocalAuthProperties localAuthProperties) {
        String secret = localAuthProperties.getHmacSecret();
        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException("auth.local.hmac-secret must be at least 32 characters.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}

