package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.DiningCancelLog;

@Repository
public interface DiningCancelLogRepository extends JpaRepository<DiningCancelLog, Long> {
}

