package com.example.testproject.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ✅ 특정 엔드포인트는 JWT 검증 제외
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ No valid Authorization header found, proceeding without authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            // ✅ JWT에서 사용자 정보 추출
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractUserType(token).toLowerCase(); // 🔥 ROLE_ 제거하고 소문자로 변환

            System.out.println("✅ Extracted userType from JWT: " + role); // 🔥 role 로그 추가

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role)); // 🔥 ROLE_ 없이 저장

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                System.out.println("✅ Valid JWT Token - User: " + username + ", Role: " + role);
            }

        } catch (ExpiredJwtException e) {
            handleAuthException(response, "JWT Token expired", e);
            return;
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException e) {
            handleAuthException(response, "Invalid JWT Token", e);
            return;
        } catch (Exception e) {
            handleAuthException(response, "Invalid or expired JWT token", e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * ✅ JWT 검증이 필요하지 않은 엔드포인트 목록
     *
     * @param requestURI 요청 URI
     * @return true이면 JWT 검증을 생략
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/account/login") ||
                requestURI.startsWith("/api/account/find-id") ||
                requestURI.startsWith("/api/account/find-password");
    }

    /**
     * ✅ 인증 실패 시 처리
     *
     * @param response HTTP 응답
     * @param message  오류 메시지
     * @param e        예외 정보
     * @throws IOException 예외 처리
     */
    private void handleAuthException(HttpServletResponse response, String message, Exception e) throws IOException {
        System.out.println("❌ " + message + ": " + e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}