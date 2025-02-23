package com.example.testproject.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenExpiration = 1000 * 60 * 60 * 12;  // 12시간 (Access Token)
    private final long refreshTokenExpiration = 1000 * 60 * 60 * 24 * 7;  // 7일 (Refresh Token)

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ✅ Access Token 생성 (username, type, userIdx 포함)
    public String generateToken(String username, String type, int userIdx) {
        return Jwts.builder()
                .setSubject(username)
                .claim("type", type)  // 사용자 역할
                .claim("userIdx", userIdx)  // 사용자 고유번호
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration)) // 1시간 유효
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Refresh Token 생성 (7일 유효)
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration)) // 7일 유효
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ JWT Claims 파싱 (모든 값 추출 가능)
    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtException("JWT 토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException e) {
            throw new JwtException("지원되지 않는 JWT 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new JwtException("잘못된 JWT 서명입니다.");
        } catch (Exception e) {
            throw new JwtException("유효하지 않은 JWT 토큰입니다.");
        }
    }

    // ✅ 사용자 이름 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ✅ 사용자 역할 (type) 추출
    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    // ✅ 사용자 userIdx 추출
    public int extractUserIdx(String token) {
        return extractClaim(token, claims -> claims.get("userIdx", Integer.class));
    }

    // ✅ 특정 Claim 추출 메서드
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    // ✅ 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true; // 정상적인 토큰이면 true 반환
        } catch (JwtException e) {
            return false; // 유효하지 않은 토큰
        }
    }

    // ✅ 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // 만료되었거나 유효하지 않은 토큰
        }
    }
}