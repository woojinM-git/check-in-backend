package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomReservation;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Integer>{

    /* 오늘 체크인한 사람의 수 */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
        "WHERE r.checkinDate = CURRENT_DATE")
    Integer findTodayCheckinCount();

    /* 오늘 체크아웃한 사람의 수 */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
        "WHERE r.checkoutDate = CURRENT_DATE")
    Integer findTodayCheckoutCount();

    /* 예약 확정인 사람의 수 */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
        "WHERE r.status = 1")
    Integer findByStatus();

    /* 마이페이지 예약 목록 조회 */
    @Query("SELECT r FROM RoomReservation r " +
            "WHERE r.customerIdx = :customerIdx AND r.status IN :statusList " +
            "ORDER BY r.checkinDate DESC")
    List<RoomReservation> findByReservationsByCustomerAndStatus(
        @Param("customerIdx") Integer customerIdx,
        @Param("statusList") List<Integer> statusList);
} 
