package com.example.wakeupAPI.service;

import com.example.wakeupAPI.entity.Member;
import com.example.wakeupAPI.entity.Token;
import com.example.wakeupAPI.repository.MemberRepository;
import com.example.wakeupAPI.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final TokenService tokenService; // ✅ TokenService 사용

    public AccountService(JwtUtil jwtUtil, MemberRepository memberRepository, TokenService tokenService) {
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
        this.tokenService = tokenService;
    }

    // ✅ 로그인 메서드 (Access & Refresh Token 반환)
    public Map<String, String> login(String id, String password) {
        Optional<Member> userOptional = memberRepository.findById(id);
        if (userOptional.isEmpty() || !password.equals(userOptional.get().getPassword())) {
            return null;
        }

        Member user = userOptional.get();
        int userIdx = user.getIdx(); // ✅ 정확한 user_idx 사용

        String accessToken = jwtUtil.generateToken(user.getId(), user.getType(), userIdx);

        // ✅ 기존 Refresh Token 확인 (재사용)
        Optional<Token> existingToken = tokenService.getTokenByUserIdx(userIdx);
        String refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get().getToken();
            System.out.println("✅ 기존 Refresh Token 재사용: " + refreshToken);
        } else {
            refreshToken = jwtUtil.generateRefreshToken(user.getId());
            System.out.println("🔄 새로운 Refresh Token 생성: " + refreshToken);

            tokenService.saveToken(refreshToken, userIdx); // ✅ 정확한 user_idx 사용하여 저장
            System.out.println("💾 Refresh Token 저장 완료: " + refreshToken);
        }

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("type", user.getType());
        tokenMap.put("userIdx", String.valueOf(userIdx));

        return tokenMap;
    }

    // ✅ Refresh Token 검증
    public boolean validateToken(String token) {
        int userIdx = jwtUtil.extractUserIdx(token); // ✅ 정확한 user_idx 사용

        Optional<Token> optionalToken = tokenService.getTokenByUserIdx(userIdx);
        if (optionalToken.isEmpty()) {
            System.out.println("❌ 해당 user_idx의 Refresh Token이 없음: " + userIdx);
            return false;
        }

        Token refreshToken = optionalToken.get();
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenService.deleteToken(refreshToken.getToken());
            System.out.println("🗑 만료된 Refresh Token 삭제: " + refreshToken.getToken());
            return false;
        }

        return jwtUtil.validateToken(token);
    }

    // ✅ 로그아웃 시 Token 삭제
    @Transactional
    public void deleteToken(String token) {
        int userIdx = jwtUtil.extractUserIdx(token);
        Optional<Token> tokenOptional = tokenService.getTokenByUserIdx(userIdx);

        if (tokenOptional.isPresent()) {
            System.out.println("🗑 로그아웃 - Refresh Token 삭제: " + tokenOptional.get().getToken());
            tokenService.deleteToken(tokenOptional.get().getToken());
        } else {
            System.out.println("❌ 로그아웃 실패 - 해당 user_idx의 Refresh Token 없음: " + userIdx);
        }
    }
}