package com.example.wakeupAPI.controller;

import com.example.wakeupAPI.entity.Member;
import com.example.wakeupAPI.entity.Token;
import com.example.wakeupAPI.repository.MemberRepository;
import com.example.wakeupAPI.security.JwtUtil;
import com.example.wakeupAPI.service.AccountService;
import com.example.wakeupAPI.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final AccountService accountService;
    private final TokenService tokenService;

    // ✅ 1. 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String phone = request.get("phone");
        String company = request.get("company");

        if (name == null || phone == null || company == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "입력된 데이터가 올바르지 않습니다."));
        }

        Optional<Member> member = memberRepository.findByNameAndPhoneAndCompany(name, phone, company);
        return member.map(m -> ResponseEntity.ok(Map.of("id", m.getId())))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "존재하지 않는 사용자")));
    }

    // ✅ 2. 비밀번호 찾기
    @PostMapping("/find-password")
    public ResponseEntity<?> findPassword(@RequestBody Map<String, String> request) {
        String id = request.get("id");
        String phone = request.get("phone");
        String company = request.get("company");

        if (id == null || phone == null || company == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "입력된 데이터가 올바르지 않습니다."));
        }

        Optional<Member> member = memberRepository.findByIdAndPhoneAndCompany(id, phone, company);
        return member.map(m -> ResponseEntity.ok(Map.of("password", m.getPassword())))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "존재하지 않는 사용자")));
    }

    // ✅ 3. 로그인 (JWT 토큰 발급)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String id = request.get("id");
        String password = request.get("password");

        if (id == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "입력된 데이터가 올바르지 않습니다."));
        }

        Map<String, String> tokens = accountService.login(id, password);
        if (tokens == null) {
            return ResponseEntity.status(403).body(Map.of("message", "인증 실패 - 잘못된 비밀번호 또는 존재하지 않는 계정"));
        }

        return ResponseEntity.ok(tokens);
    }

    // ✅ 4. 내 정보 조회 (JWT 인증 필요)
    @GetMapping("/myinfo")
    public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String token) {
        try {
            String userId = jwtUtil.extractUsername(token.replace("Bearer ", ""));
            Optional<Member> member = memberRepository.findById(userId);

            return member.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "존재하지 않는 사용자")));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "인증 실패 - 유효한 토큰이 필요합니다."));
        }
    }

    // ✅ 5. 새 액세스토큰 발급 (Refresh Token으로만 인증)
    @PostMapping("/accesstoken")
    public ResponseEntity<?> getNewAccessToken(@RequestHeader("Authorization") String token) {
        String refreshToken = token.replace("Bearer ", "");

        System.out.println("🔄 Access Token 재발급 요청: Refresh Token = " + refreshToken);

        // ✅ Refresh Token 검증 (DB에서 존재하는지 확인)
        Optional<Token> optionalToken = tokenService.getTokenByToken(refreshToken);
        if (optionalToken.isEmpty()) {
            System.out.println("❌ Refresh Token이 DB에 없음 또는 만료됨.");
            return ResponseEntity.status(401).body(Map.of("message", "만료된 refresh token 입니다."));
        }

        Token validToken = optionalToken.get();
        int userIdx = validToken.getUserIdx(); // ✅ DB에서 userIdx 가져오기

        Optional<Member> member = memberRepository.findByIdx(userIdx);
        if (member.isEmpty()) {
            System.out.println("❌ 존재하지 않는 userIdx: " + userIdx);
            return ResponseEntity.status(403).body(Map.of("message", "잘못된 refresh token 입니다."));
        }

        // ✅ 새로운 Access Token 생성
        String newAccessToken = jwtUtil.generateToken(member.get().getId(), member.get().getType(), userIdx);
        System.out.println("✅ 새로운 Access Token 발급 완료: " + newAccessToken);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    // ✅ 6. 로그아웃 (Refresh Token 삭제)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        System.out.println("🔄 로그아웃 요청: Access Token 사용");
        System.out.println("📡 받은 Authorization 헤더 값: " + token);

        int userIdx = jwtUtil.extractUserIdx(token.replace("Bearer ", ""));

        if (userIdx == -1) {
            System.out.println("❌ 로그아웃 실패 - JWT에서 userIdx를 가져올 수 없음.");
            return ResponseEntity.status(401).body(Map.of("message", "잘못된 Access Token 입니다."));
        }

        System.out.println("🔍 로그아웃 요청한 userIdx: " + userIdx);
        Optional<Token> optionalToken = tokenService.getTokenByUserIdx(userIdx);

        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "해당 사용자의 Refresh Token이 존재하지 않습니다."));
        }

        tokenService.deleteToken(optionalToken.get().getToken());

        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }
}