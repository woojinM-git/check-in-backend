package com.sist.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelCancelLog;

@Repository
public interface HotelCancelLogRepository extends JpaRepository<HotelCancelLog, Long> {

    Optional<HotelCancelLog> findTopByReservIdxOrderByCancelAtDesc(Integer reservIdx);
}
