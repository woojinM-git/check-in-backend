package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelCancelLog;

@Repository
public interface HotelCancelLogRepository extends JpaRepository<HotelCancelLog, Long> {
}
