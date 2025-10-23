package com.sist.backend.repository;

import com.sist.backend.entity.RoomReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Integer> {

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

    /* Room과 Customer 정보를 함께 조회하는 메서드 */
    @Query("SELECT r FROM RoomReservation r " +
        "LEFT JOIN FETCH r.room " +
        "LEFT JOIN FETCH r.customer " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 " +
        "ORDER BY r.createdAt DESC")
    List<RoomReservation> findByStatusWithDetails(@Param("contentid") String contentid);

    /* 현재 로그인 한 관리자의 소유한업소(인자)를 조건으로 예약status가 1인 사람들의 목록 */
    @Query("SELECT r FROM RoomReservation r " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1")
    Page<RoomReservation> findByStatusDto(@Param("contentid") String contentid, Pageable pageable);

    /* 체크인 대기 목록 조회 (checkin 컬럼이 NULL인 경우) */
    @Query("SELECT r FROM RoomReservation r " +
        "LEFT JOIN FETCH r.room " +
        "LEFT JOIN FETCH r.customer " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 " +
        "AND r.checkinDate IS NULL " +
        "ORDER BY r.checkinDate ASC")
    Page<RoomReservation> findCheckinPendingWithDetails(@Param("contentid") String contentid, Pageable pageable);

    /* 체크아웃 대기 목록 조회 (checkin은 완료되고 checkout이 NULL인 경우) */
    @Query("SELECT r FROM RoomReservation r " +
        "LEFT JOIN FETCH r.room " +
        "LEFT JOIN FETCH r.customer " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 " +
        "AND r.checkinDate IS NOT NULL " +
        "AND r.checkoutDate IS NULL " +
        "ORDER BY r.checkoutDate ASC")
    Page<RoomReservation> findCheckoutPendingWithDetails(@Param("contentid") String contentid, Pageable pageable);

    /* room 목록 */
    List<RoomReservation> findByContentid(@Param("contentid") String contentid);

    /* 마이페이지 예약 목록 조회 (Hotel, Room 정보 포함) */
    @Query("SELECT DISTINCT r FROM RoomReservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.hotelInfo hotel " +
            "LEFT JOIN FETCH hotel.area area " +
            "WHERE r.customerIdx = :customerIdx AND r.status IN :statusList " +
            "ORDER BY r.checkinDate DESC")
    List<RoomReservation> findByReservationsByCustomerAndStatus(
        @Param("customerIdx") Integer customerIdx,
        @Param("statusList") List<Integer> statusList);

    @Query("UPDATE RoomReservation r SET r.customerIdx = :customerIdx WHERE r.reservIdx = :reservIdx")
    int updateCustomerIdxByReservIdx(@Param("reservIdx") Integer reservIdx, @Param("customerIdx") Integer customerIdx);
}
