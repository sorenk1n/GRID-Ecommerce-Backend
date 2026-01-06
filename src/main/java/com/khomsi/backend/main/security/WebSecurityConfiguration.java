package com.khomsi.backend.main.security;

import com.khomsi.backend.main.security.keycloak.JwtAuthConverter;
import com.khomsi.backend.main.user.model.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@RequiredArgsConstructor
public class WebSecurityConfiguration {
    private final JwtAuthConverter jwtAuthConverter;

    @Bean
    public SecurityFilterChain securityFilterChainConfig(HttpSecurity http) throws Exception {
        return http
                // 定义公开、用户、管理员接口的授权规则。
                .authorizeHttpRequests(auth -> auth
                        // Swagger/OpenAPI 接口无需登录，便于接口文档访问。
                        .requestMatchers("/swagger", "/swagger-ui/**",
                                "/v3/api-docs/**", "/error").permitAll()

                        // 支付平台回调必须允许匿名访问。
                        .requestMatchers("/api/v1/checkout/alipay/notify").permitAll()

                        // 公共展示/只读接口，无需认证。
                        .requestMatchers("/api/v1/games", "/api/v1/games/**",
                                "/api/v1/genres/**", "/api/v1/genres",
                                "/api/v1/platforms", "/api/v1/developers",
                                "/api/v1/publishers", "/api/v1/tags", "/api/v1/reviews/**",
                                "/chat", "/chat/**"
                        ).permitAll()

                        // 管理员接口需要 ADMIN 角色。
                        .requestMatchers("/api/v1/admin/**", "/api/v1/admin")
                        .hasAnyRole(Role.ADMIN.name())

                        // 用户相关接口需要 USER 或 ADMIN 角色。
                        .requestMatchers("/api/v1/users/**", "/api/v1/users",
                                "/api/v1/cart/**", "/api/v1/cart",
                                "/api/v1/checkout", "/api/v1/checkout/**",
                                "/api/v1/transactions", "/api/v1/transactions/**",
                                "/api/v1/wishlist", "/api/v1/wishlist/**",
                                "/api/v1/reviews/*/**"
                        ).hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                )
                // 使用 Keycloak 的 token converter 配置 JWT 认证。
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                // API 采用无状态会话。
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 启用默认 CORS 配置（若存在 CorsConfigurationSource）。
                .cors(Customizer.withDefaults())
                .build();
    }
}
