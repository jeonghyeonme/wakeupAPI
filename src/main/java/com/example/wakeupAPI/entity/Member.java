package com.example.wakeupAPI.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idx;  // 회원 고유번호

    @Column(nullable = false, unique = true)
    private String id;  // 사용자 ID

    @Column(nullable = false)
    private String type;  // 사용자 유형 ('admin' or 'crew')

    @Column(nullable = false)
    private String password;  // 비밀번호

    @Column(nullable = false)
    private String name;  // 이름

    @Column(nullable = false, unique = true)
    private String phone;  // 연락처

    private String company;  // 회사명
    private String home;  // 주소
}