package com.example.wakeupAPI.repository;

import com.example.wakeupAPI.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    // ✅ 특정 운전자의 일정 조회 (driver_user_idx 사용)
    @Query("SELECT s FROM Schedule s WHERE s.driverUserIdx = :driverIdx")
    List<Schedule> findByDriverUserIdx(@Param("driverIdx") int driverIdx);

    // ✅ 특정 날짜 범위의 일정 조회
    @Query("SELECT s FROM Schedule s WHERE s.startTime BETWEEN :start AND :end")
    List<Schedule> findByStartTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ✅ 특정 날짜 이후 가장 가까운 일정 조회
    @Query("SELECT s FROM Schedule s WHERE s.startTime > :currentTime AND s.driverUserIdx = :driverIdx ORDER BY s.startTime ASC")
    List<Schedule> findTop1ByStartTimeAfterAndDriverUserIdxOrderByStartTimeAsc(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("driverIdx") int driverIdx);


    // ✅ 특정 날짜 사용자 일정 조회 (driver_user_idx 기반) - 수정됨
    @Query("SELECT s FROM Schedule s WHERE s.startTime BETWEEN :startTime AND :endTime AND s.driverUserIdx = :driverIdx ORDER BY s.startTime ASC")
    List<Schedule> findByStartTimeBetweenAndDriverUserIdx(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("driverIdx") int driverIdx);
}