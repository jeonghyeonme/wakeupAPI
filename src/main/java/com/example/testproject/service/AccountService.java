package com.example.testproject.service;

import com.example.testproject.entity.Member;
import com.example.testproject.repository.MemberRepository;
import com.example.testproject.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountService {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    public AccountService(JwtUtil jwtUtil, MemberRepository memberRepository) {
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    // ✅ 로그인 메서드 (Access & Refresh Token 반환)
    public Map<String, String> login(String id, String password) {
        System.out.println("🔍 Searching for user with id: " + id);

        Optional<Member> userOptional = memberRepository.findById(id);

        if (userOptional.isEmpty()) {
            System.out.println("❌ User not found.");
            return null;
        }

        Member user = userOptional.get();
        System.out.println("✅ Found user: " + user.getId() + ", Type: " + user.getType());

        // ✅ 평문 비밀번호 비교 (향후 암호화 필요)
        if (!password.equals(user.getPassword())) {
            System.out.println("❌ Incorrect password.");
            return null;
        }

        System.out.println("✅ Password matched. Generating JWT...");
        String accessToken = jwtUtil.generateToken(user.getId(), user.getType(), user.getIdx());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        System.out.println("🎫 Generated Access Token: " + accessToken);
        System.out.println("🔄 Generated Refresh Token: " + refreshToken);

        // ✅ Access & Refresh Token 반환
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("type", user.getType());
        tokenMap.put("userIdx", String.valueOf(user.getIdx()));  // int → String 변환

        return tokenMap;
    }
}