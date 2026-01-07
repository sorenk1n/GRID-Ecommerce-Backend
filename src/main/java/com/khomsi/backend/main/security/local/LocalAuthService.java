package com.khomsi.backend.main.security.local;

import com.khomsi.backend.main.handler.exception.GlobalServiceException;
import com.khomsi.backend.main.security.keycloak.JwtAuthConverterProperties;
import com.khomsi.backend.main.security.local.model.dto.AuthLoginRequest;
import com.khomsi.backend.main.security.local.model.dto.AuthRegisterRequest;
import com.khomsi.backend.main.security.local.model.dto.AuthResponse;
import com.khomsi.backend.main.user.model.entity.Role;
import com.khomsi.backend.main.user.model.entity.UserCredential;
import com.khomsi.backend.main.user.model.entity.UserInfo;
import com.khomsi.backend.main.user.repository.UserCredentialRepository;
import com.khomsi.backend.main.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "auth.local", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LocalAuthService {
    private final UserInfoRepository userInfoRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final LocalAuthProperties localAuthProperties;
    private final JwtAuthConverterProperties jwtAuthConverterProperties;

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        if (userInfoRepository.findByUsername(request.username()).isPresent()) {
            throw new GlobalServiceException(HttpStatus.BAD_REQUEST, "Username already exists.");
        }
        if (userInfoRepository.findByEmail(request.email()).isPresent()) {
            throw new GlobalServiceException(HttpStatus.BAD_REQUEST, "Email already exists.");
        }

        UserInfo user = new UserInfo();
        user.setExternalId(UUID.randomUUID().toString());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setBalance(BigDecimal.ZERO);
        userInfoRepository.save(user);

        UserCredential credential = new UserCredential(
                user.getExternalId(),
                passwordEncoder.encode(request.password())
        );
        userCredentialRepository.save(credential);

        return buildAuthResponse(user);
    }

    public AuthResponse login(AuthLoginRequest request) {
        UserInfo user = userInfoRepository.findByUsername(request.username())
                .orElseThrow(() -> new GlobalServiceException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
        UserCredential credential = userCredentialRepository.findById(user.getExternalId())
                .orElseThrow(() -> new GlobalServiceException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new GlobalServiceException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserInfo user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(localAuthProperties.getTokenTtl());
        String resourceId = jwtAuthConverterProperties.getResourceId();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(localAuthProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getExternalId())
                .claim("preferred_username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("resource_access", Map.of(
                        resourceId, Map.of("roles", List.of(Role.USER.name()))
                ))
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new AuthResponse(token, "Bearer", localAuthProperties.getTokenTtl().toSeconds());
    }
}






