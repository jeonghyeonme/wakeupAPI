package com.example.testproject.controller;

import com.example.testproject.entity.Schedule;
import com.example.testproject.repository.ScheduleRepository;
import com.example.testproject.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/crew")
@RequiredArgsConstructor
public class CrewController {

    private final ScheduleRepository scheduleRepository;
    private final JwtUtil jwtUtil;

    // ✅ 가장 가까운 시간 데이터를 가져오기
    @GetMapping("/dateTime/{date}")
    public ResponseEntity<?> getClosestSchedule(
            @RequestHeader("Authorization") String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {  // ✅ LocalDate로 받기

        try {
            // ✅ JWT에서 사용자 권한 확인
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            if (!"crew".equals(userType)) {
                return ResponseEntity.status(403).body(Map.of("message", "잘못된 access token 입니다."));
            }

            // ✅ LocalDate를 LocalDateTime으로 변환 (00:00:00 기준)
            LocalDateTime targetTime = date.atStartOfDay();

            // ✅ 현재 시간 기준 가장 가까운 일정 찾기
            List<Schedule> schedules = scheduleRepository.findByStartTimeAfterOrderByStartTimeAsc(targetTime);

            if (schedules.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "해당 날짜 이후의 일정이 없습니다."));
            }

            return ResponseEntity.ok(schedules.get(0));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }

    // ✅ 출석 클릭 이벤트
    @PutMapping("/attendance/schedule/{scheduleIdx}")
    public ResponseEntity<?> markAttendance(
            @RequestHeader("Authorization") String token,
            @PathVariable int scheduleIdx) {
        try {
            System.out.println("✅ 요청받음: /crew/attendance/schedule/" + scheduleIdx);

            // ✅ JWT에서 사용자 권한 확인
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            if (!"crew".equals(userType)) {
                return ResponseEntity.status(403).body(Map.of("message", "잘못된 access token 입니다."));
            }

            // ✅ 일정 존재 여부 확인
            Optional<Schedule> optionalSchedule = scheduleRepository.findById(scheduleIdx);
            if (optionalSchedule.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("message", "해당 idx 일정이 존재하지 않습니다."));
            }

            // ✅ 출석 이벤트 처리
            Schedule schedule = optionalSchedule.get();
            schedule.setWakeup(true);  // ✅ Hibernate가 자동으로 TINYINT(1) -> boolean 변환
            scheduleRepository.save(schedule);

            System.out.println("✅ 출석 체크 완료: " + scheduleIdx);
            return ResponseEntity.ok(Map.of("message", "출석이 완료되었습니다."));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }
}