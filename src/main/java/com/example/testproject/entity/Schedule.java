package com.example.testproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idx;  // 일정 고유번호

    @Column(nullable = false)
    private LocalDateTime startTime;  // 일정 시작 시간

    @Column(nullable = false)
    private LocalDateTime endTime;  // 일정 종료 시간

    @Column(nullable = false)
    private String title;  // 일정 제목

    @Column(nullable = false)
    private boolean wakeup;  // wakeup 여부

    @Column(name = "driver_user_idx", nullable = false)
    private int driverUserIdx;  // 운전자 ID (FK로 사용)
}