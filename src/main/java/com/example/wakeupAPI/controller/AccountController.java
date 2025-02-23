package com.example.wakeupAPI.controller;

import com.example.wakeupAPI.entity.Member;
import com.example.wakeupAPI.repository.MemberRepository;
import com.example.wakeupAPI.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

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

        Optional<Member> member = memberRepository.findById(id);
        if (member.isEmpty() || !member.get().getPassword().equals(password)) { // 평문 비교
            return ResponseEntity.status(403).body(Map.of("message", "인증 실패 - 잘못된 비밀번호"));
        }

        try {
            // authenticationManager.authenticate 제거 -> 대신 직접 토큰 발급
            String accessToken = jwtUtil.generateToken(id, member.get().getType(), member.get().getIdx());
            String refreshToken = jwtUtil.generateRefreshToken(id);

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken,
                    "type", member.get().getType(),
                    "userIdx", member.get().getIdx()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "예상치 못한 에러입니다."));
        }
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
}