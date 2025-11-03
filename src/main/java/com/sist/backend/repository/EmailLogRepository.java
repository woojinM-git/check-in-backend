package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.EmailLog;

/**
 * 이메일 로그 Repository 이메일 발송 이력 관리
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
