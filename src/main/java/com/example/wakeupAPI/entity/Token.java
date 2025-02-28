package com.example.wakeupAPI.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id; // ✅ token 테이블의 primary key

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_idx", nullable = false) // ✅ Member의 idx와 동일한 값
    private int userIdx;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
}