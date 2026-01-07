package com.khomsi.backend.main.security.local.controller;

import com.khomsi.backend.main.security.local.LocalAuthService;
import com.khomsi.backend.main.security.local.model.dto.AuthLoginRequest;
import com.khomsi.backend.main.security.local.model.dto.AuthRegisterRequest;
import com.khomsi.backend.main.security.local.model.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.local", name = "enabled", havingValue = "true")
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LocalAuthService localAuthService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return localAuthService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return localAuthService.login(request);
    }
}
