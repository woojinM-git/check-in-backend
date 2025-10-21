package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomReservation;

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

    @Query("SELECT r FROM RoomReservation r " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1")
    List<RoomReservation> findByStatus(@Param("contentid") String contentid);

    /* 현재 로그인 한 관리자의 소유한업소(인자)를 조건으로 예약status가 1인 사람들의 목록 */
    @Query("SELECT r FROM RoomReservation r " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1")
    Page<RoomReservation> findByStatusDto(@Param("contentid") String contentid, Pageable pageable);
}
