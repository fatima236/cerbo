package com.example.cerbo.repository;

import com.example.cerbo.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    VerificationCode findByEmailAndCode(String email, String code);
    void deleteByEmail(String email);
}