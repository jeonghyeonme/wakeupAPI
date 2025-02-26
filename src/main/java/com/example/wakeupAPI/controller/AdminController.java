package com.example.wakeupAPI.controller;

import com.example.wakeupAPI.entity.Schedule;
import com.example.wakeupAPI.entity.Member;
import com.example.wakeupAPI.repository.ScheduleRepository;
import com.example.wakeupAPI.repository.MemberRepository;
import com.example.wakeupAPI.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    // ✅ 특정 날짜의 스케줄 조회 (관리자 전용)
    @GetMapping("/dateTime/{dateTime}")
    public ResponseEntity<?> getSchedulesByDate(
            @RequestHeader("Authorization") String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String dateTime) {

        try {
            // JWT에서 사용자 권한 확인
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            if (!"admin".equals(userType)) {
                return ResponseEntity.status(403).body(Map.of("message", "잘못된 access token 입니다."));
            }

            // 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(dateTime);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "date 오류"));
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);

            List<Schedule> schedules = scheduleRepository.findByStartTimeBetween(start, end);

            // 각 Schedule을 row 형식의 Map으로 변환 (driver 정보 포함)
            List<Map<String, Object>> rows = schedules.stream().map(schedule -> {
                Map<String, Object> row = new HashMap<>();
                row.put("idx", schedule.getIdx());
                row.put("start_time", schedule.getStartTime());
                row.put("end_time", schedule.getEndTime());
                row.put("title", schedule.getTitle());

                // driverUserIdx를 이용해 Member 정보 조회
                Optional<Member> optionalMember = memberRepository.findByIdx(schedule.getDriverUserIdx());
                if (optionalMember.isPresent()) {
                    Member member = optionalMember.get();
                    Map<String, Object> driverMap = new HashMap<>();
                    driverMap.put("name", member.getName());
                    driverMap.put("phone", member.getPhone());
                    driverMap.put("company", member.getCompany());
                    driverMap.put("user_idx", member.getIdx());
                    row.put("driver", driverMap);
                } else {
                    row.put("driver", null);
                }
                row.put("wakeup", schedule.isWakeup());
                return row;
            }).collect(Collectors.toList());

            // 최종 JSON 형식: { "rows": [ ... ] }
            return ResponseEntity.ok(Map.of("rows", rows));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }
}