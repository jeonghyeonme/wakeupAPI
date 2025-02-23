package com.example.testproject.controller;

import com.example.testproject.entity.Member;
import com.example.testproject.entity.Schedule;
import com.example.testproject.repository.MemberRepository;
import com.example.testproject.repository.ScheduleRepository;
import com.example.testproject.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/trip")
public class TripController {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public TripController(ScheduleRepository scheduleRepository, MemberRepository memberRepository, JwtUtil jwtUtil) {
        this.scheduleRepository = scheduleRepository;
        this.memberRepository = memberRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/data")
    public ResponseEntity<?> getUserSchedulesByDate(
            @RequestHeader("Authorization") String token,
            @RequestParam String date) {
        try {
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            int userIdx = jwtUtil.extractUserIdx(token.replace("Bearer ", ""));

            if (userIdx == -1) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }

            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return ResponseEntity.status(400).body(Map.of("message", "date 오류"));
            }

            // ✅ 특정 날짜 + 사용자 일정 조회
            List<Schedule> schedules = scheduleRepository.findByStartTimeContainingAndDriverUserIdx(date, userIdx);
            if (schedules.isEmpty()) {
                return ResponseEntity.ok(Map.of("rows", new ArrayList<>()));
            }

            // ✅ 운전자 정보 개별 조회 후 응답 데이터 생성
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (Schedule schedule : schedules) {
                Optional<Member> driverOpt = memberRepository.findById(schedule.getDriverUserIdx());

                Map<String, Object> scheduleData = new HashMap<>();
                scheduleData.put("idx", schedule.getIdx());
                scheduleData.put("title", schedule.getTitle());
                scheduleData.put("start_time", schedule.getStartTime().toString());
                scheduleData.put("end_time", schedule.getEndTime().toString());
                scheduleData.put("wakeup", schedule.isWakeup());

                if (driverOpt.isPresent()) {
                    Member driver = driverOpt.get();
                    Map<String, Object> driverData = new HashMap<>();
                    driverData.put("name", driver.getName());
                    driverData.put("phone", driver.getPhone());
                    driverData.put("company", driver.getCompany());
                    driverData.put("user_idx", driver.getIdx());
                    scheduleData.put("driver", driverData);
                } else {
                    scheduleData.put("driver", null);
                }

                responseList.add(scheduleData);
            }

            return ResponseEntity.ok(Map.of("rows", responseList));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }
}