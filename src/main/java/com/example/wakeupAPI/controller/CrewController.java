package com.example.wakeupAPI.controller;

import com.example.wakeupAPI.entity.Member;
import com.example.wakeupAPI.entity.Schedule;
import com.example.wakeupAPI.repository.MemberRepository;
import com.example.wakeupAPI.repository.ScheduleRepository;
import com.example.wakeupAPI.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/crew")
@RequiredArgsConstructor
public class CrewController {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    // ✅ 가장 가까운 시간 데이터를 가져오기 - 개선된 버전
    @GetMapping("/dateTime/{date}")
    public ResponseEntity<?> getClosestSchedule(
            @RequestHeader("Authorization") String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            // ✅ JWT에서 사용자 권한 및 idx 확인
            String userType = jwtUtil.extractUserType(token.replace("Bearer ", ""));
            int userIdx = jwtUtil.extractUserIdx(token.replace("Bearer ", ""));

            System.out.println("✅ User Type: " + userType);
            System.out.println("✅ User Idx: " + userIdx);

            // ✅ 사용자 권한 확인
            if (!"crew".equals(userType)) {
                return ResponseEntity.status(403).body(Map.of("message", "잘못된 access token 입니다."));
            }

            // ✅ LocalDate를 LocalDateTime으로 변환 (00:00:00 기준)
            LocalDateTime targetTime = date.atStartOfDay();
            System.out.println("✅ Target Time: " + targetTime);
            System.out.println("✅ User Idx for Query: " + userIdx);

            // ✅ 입력 시간 이후의 가장 가까운 일정 찾기 + 사용자 idx 조건 추가
            List<Schedule> schedules = scheduleRepository
                    .findTop1ByStartTimeAfterAndDriverUserIdxOrderByStartTimeAsc(targetTime, userIdx);

            // ✅ 첫 번째 쿼리 결과 디버깅
            System.out.println("✅ 첫 번째 쿼리 결과: " + schedules);

            // ✅ 첫 번째 쿼리 결과가 없으면 (다음 날짜 이후 일정 찾기)
            if (schedules.isEmpty()) {
                // 다음 날짜의 00:00:00로 설정
                LocalDateTime nextDay = date.plusDays(1).atStartOfDay();
                schedules = scheduleRepository
                        .findTop1ByStartTimeAfterAndDriverUserIdxOrderByStartTimeAsc(nextDay, userIdx);

                // ✅ 두 번째 쿼리 결과 디버깅
                System.out.println("✅ 두 번째 쿼리 결과: " + schedules);
            }

            if (schedules.isEmpty()) {
                System.out.println("❌ 조건에 맞는 일정이 없습니다.");
                return ResponseEntity.status(404).body(Map.of("message", "해당 날짜 이후의 일정이 없습니다."));
            }

            // ✅ 가장 가까운 일정 하나만 반환
            Schedule closestSchedule = schedules.get(0);
            Map<String, Object> scheduleData = new HashMap<>();
            scheduleData.put("idx", closestSchedule.getIdx());
            scheduleData.put("title", closestSchedule.getTitle());
            scheduleData.put("start_time", closestSchedule.getStartTime().toString());
            scheduleData.put("end_time", closestSchedule.getEndTime().toString());
            scheduleData.put("wakeup", closestSchedule.isWakeup());

            // ✅ 운전자 정보 추가
            Optional<Member> driverOpt = memberRepository.findById(closestSchedule.getDriverUserIdx());
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

            return ResponseEntity.ok(scheduleData);

        } catch (Exception e) {
            e.printStackTrace(); // ✅ 에러 로그 출력 추가
            return ResponseEntity.status(500).body(Map.of("message", "서버 연결에 실패하였습니다."));
        }
    }

    // ✅ 출석 클릭 이벤트 (기존 코드 유지)
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