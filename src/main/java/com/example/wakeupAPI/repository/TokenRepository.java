package com.example.wakeupAPI.repository;

import com.example.wakeupAPI.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByUserIdx(int userIdx); // ✅ 변경됨
    void deleteByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime now);
}