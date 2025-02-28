package com.example.wakeupAPI.service;

import com.example.wakeupAPI.entity.Token;
import com.example.wakeupAPI.repository.TokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // ✅ 특정 사용자의 Refresh Token 조회
    public Optional<Token> getTokenByUserIdx(int userIdx) {
        return tokenRepository.findByUserIdx(userIdx);
    }

    // ✅ Refresh Token 조회
    public Optional<Token> getTokenByToken(String token) {
        return tokenRepository.findByToken(token); // ✅ Refresh Token 값으로 DB에서 조회
    }

    // ✅ Refresh Token 저장 (새로운 토큰 생성 시 사용)
    public Token saveToken(String token, int userIdx) {
        Token newToken = new Token();
        newToken.setToken(token);
        newToken.setUserIdx(userIdx);
        newToken.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7일 유효
        return tokenRepository.save(newToken);
    }

    // ✅ Refresh Token 삭제 (로그아웃 시 호출)
    @Transactional
    public void deleteToken(String token) {
        tokenRepository.deleteByToken(token);
    }

    // ✅ 만료된 Refresh Token 자동 삭제 (매일 00:00 실행)
    @Scheduled(cron = "0 0 0 * * ?")
    public void deleteExpiredTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
        System.out.println("🗑️ Expired Tokens Deleted: " + LocalDateTime.now());
    }
}