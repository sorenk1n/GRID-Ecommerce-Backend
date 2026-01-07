package com.khomsi.backend.main.security.local;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;

public class CompositeJwtDecoder implements JwtDecoder {
    private final List<JwtDecoder> delegates;

    public CompositeJwtDecoder(List<JwtDecoder> delegates) {
        this.delegates = delegates;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        JwtException lastException = null;
        for (JwtDecoder decoder : delegates) {
            try {
                return decoder.decode(token);
            } catch (JwtException ex) {
                lastException = ex;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new JwtException("No JwtDecoder configured.");
    }
}
