package com.example.testproject.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private final String token;

    public JwtAuthenticationToken(String username, String token) {
        super(Collections.emptyList()); // ✅ 권한 리스트를 빈 리스트로 설정하여 오류 방지
        this.username = username;
        this.token = token;
        setAuthenticated(true); // ✅ 인증된 상태로 설정
    }

    @Override
    public Object getCredentials() {
        return token; // ✅ JWT 토큰 반환
    }

    @Override
    public Object getPrincipal() {
        return username; // ✅ 사용자 이름 반환
    }

    @Override
    public String getName() {
        return username; // ✅ 인증 객체의 이름 반환
    }
}