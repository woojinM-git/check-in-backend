package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.ReservationTime;

@Repository
public interface ReservationTimeRepository extends JpaRepository<ReservationTime, Integer> {
    ReservationTime findByOrderIdx(Integer orderIdx);
}
