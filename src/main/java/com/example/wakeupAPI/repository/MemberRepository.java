package com.example.wakeupAPI.repository;

import com.example.wakeupAPI.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {

    // ✅ 사용자 ID로 찾기
    Optional<Member> findById(String id);

    // ✅ 고유번호(idx)로 찾기
    Optional<Member> findByIdx(int idx);

    // ✅ 아이디 찾기 (이름 + 전화번호 + 회사명 기반)
    Optional<Member> findByNameAndPhoneAndCompany(String name, String phone, String company);

    // ✅ 비밀번호 찾기 (아이디 + 전화번호 + 회사명 기반)
    Optional<Member> findByIdAndPhoneAndCompany(String id, String phone, String company);
}