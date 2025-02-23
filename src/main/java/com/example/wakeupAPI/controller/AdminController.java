package com.example.wakeupAPI.controller;

import com.example.wakeupAPI.entity.Schedule;
import com.example.wakeupAPI.repository.ScheduleRepository;
import com.example.wakeupAPI.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ScheduleRepository scheduleRepository;
    private final JwtUtil jwtUtil;

    // ✅ 특정 날짜의 스케줄 조회 (관리자 전용)
    @GetMapping("/dateTime/{dateTime}")
    public ResponseEntity<?> getSchedulesByDate(
            @RequestHeader("Authorization") String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String dateTime) {

        try {
            // ✅ JWT에서 사용자 권한 확인
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            if (!"admin".equals(userType)) {
                return ResponseEntity.status(403).body(Map.of("message", "잘못된 access token 입니다."));
            }

            // ✅ 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(dateTime);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "date 오류"));
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);

            List<Schedule> schedules = scheduleRepository.findByStartTimeBetween(start, end);

            return ResponseEntity.ok(schedules);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }
}