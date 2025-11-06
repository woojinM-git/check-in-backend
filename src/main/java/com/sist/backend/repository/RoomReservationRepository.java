package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomReservation;

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

    /* 오늘 예약한 사람의 수 */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
        "WHERE r.createdAt = CURRENT_DATE")
    Integer findByTodayCount();

    @Query("SELECT r FROM RoomReservation r " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 OR r.status = 4")
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

    /* 체크인 대기 목록 조회 (inTime 컬럼이 NULL인 경우) */
    @Query("SELECT r FROM RoomReservation r " +
        "LEFT JOIN FETCH r.room " +
        "LEFT JOIN FETCH r.customer " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 " +
        "ORDER BY r.checkinDate ASC")
    Page<RoomReservation> findCheckinPendingWithDetails(@Param("contentid") String contentid, Pageable pageable);

    /* 체크아웃 대기 목록 조회 (outTime 컬럼이 NULL인 경우) */
    @Query("SELECT r FROM RoomReservation r " +
        "LEFT JOIN FETCH r.room " +
        "LEFT JOIN FETCH r.customer " +
        "WHERE r.contentid = :contentid " +
        "AND r.status = 1 " +
        "ORDER BY r.checkoutDate ASC")
    Page<RoomReservation> findCheckoutPendingWithDetails(@Param("contentid") String contentid, Pageable pageable);

    /* room 목록 */
    List<RoomReservation> findByContentid(@Param("contentid") String contentid);

    /* 마이페이지 예약 목록 조회 (Hotel, Room 정보 포함) - 페이지네이션 미지원 */
    @Query("SELECT DISTINCT r FROM RoomReservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.hotelInfo hotel " +
            "LEFT JOIN FETCH hotel.area area " +
            "WHERE r.customerIdx = :customerIdx AND r.status IN :statusList " +
            "ORDER BY r.checkinDate DESC")
    List<RoomReservation> findByReservationsByCustomerAndStatus(
        @Param("customerIdx") Integer customerIdx,
        @Param("statusList") List<Integer> statusList);

    /* 마이페이지 예약 목록 조회 (Hotel, Room 정보 포함) - 페이지네이션 지원 */
    @Query("SELECT DISTINCT r FROM RoomReservation r " +
            "LEFT JOIN FETCH r.room room " +
            "LEFT JOIN FETCH room.hotelInfo hotel " +
            "LEFT JOIN FETCH hotel.area area " +
            "WHERE r.customerIdx = :customerIdx AND r.status IN :statusList " +
            "ORDER BY r.checkinDate DESC")
    Page<RoomReservation> findByReservationsByCustomerAndStatusWithPagination(
        @Param("customerIdx") Integer customerIdx,
        @Param("statusList") List<Integer> statusList,
        Pageable pageable);

    /* 달력용 예약 조회 - 체크인 날짜 기준으로 검색 */
    @Query("SELECT r FROM RoomReservation r " +
           "LEFT JOIN FETCH r.room " +
           "LEFT JOIN FETCH r.customer " +
           "WHERE r.contentid = :contentid " +
           "AND r.status = 1 " +
           "AND r.checkinDate >= :startDate AND r.checkinDate <= :endDate " +
           "ORDER BY r.checkinDate ASC")
    List<RoomReservation> findByDateRangeWithDetails(
            @Param("contentid") String contentid,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    @Modifying
    @Query("UPDATE RoomReservation r SET r.customerIdx = :customerIdx WHERE r.reservIdx = :reservIdx")
    int updateCustomerIdxByReservIdx(@Param("reservIdx") Integer reservIdx, @Param("customerIdx") Integer customerIdx);

    /* customerIdx와 status로 예약 목록 조회 */
    List<RoomReservation> findByCustomerIdxAndStatus(Integer customerIdx, Integer status);

    /* customerIdx와 status로 예약 개수 조회 */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
           "WHERE r.customerIdx = :customerIdx AND r.status = :status")
    Long countByCustomerIdxAndStatus(@Param("customerIdx") Integer customerIdx, @Param("status") Integer status);

    /**
     * 같은 객실/콘텐츠/체크인 조합으로 활성 예약이 존재하는지 여부
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RoomReservation r "
            + "WHERE r.roomIdx = :roomIdx AND r.contentid = :contentId AND r.checkinDate = :checkinDate "
            + "AND r.status IN (1)")
    boolean existsActiveReservation(@Param("roomIdx") Integer roomIdx,
            @Param("contentId") String contentId,
            @Param("checkinDate") java.time.LocalDate checkinDate);

    /* 특정 호텔을 이용한 기록이 있는 고객 수 */
    @Query("SELECT COUNT(DISTINCT r.customerIdx) FROM RoomReservation r " +
           "WHERE r.contentid = :contentid AND r.status = 1")
    Long countDistinctCustomersByContentId(@Param("contentid") String contentid);

    /* 이번 달 새로 이용을 시작한 고객 수 */
    @Query("SELECT COUNT(DISTINCT r.customerIdx) FROM RoomReservation r " +
           "WHERE r.contentid = :contentid " +
           "AND r.status = 1 " +
           "AND YEAR(r.checkinDate) = YEAR(CURRENT_DATE) " +
           "AND MONTH(r.checkinDate) = MONTH(CURRENT_DATE) " +
           "AND r.checkinDate = (SELECT MIN(r2.checkinDate) FROM RoomReservation r2 " +
           "                      WHERE r2.customerIdx = r.customerIdx " +
           "                      AND r2.contentid = :contentid " +
           "                      AND r2.status = 1)")
    Long countNewCustomersThisMonth(@Param("contentid") String contentid);

    /* 특정 호텔을 이용한 고객별 체크인 날짜 목록 */
    @Query("SELECT DISTINCT r.checkinDate FROM RoomReservation r " +
           "WHERE r.customerIdx = :customerIdx " +
           "AND r.contentid = :contentid " +
           "AND r.status = 1 " +
           "ORDER BY r.checkinDate ASC")
    List<java.time.LocalDate> findCheckinDatesByCustomerAndContentId(
            @Param("customerIdx") Integer customerIdx,
            @Param("contentid") String contentid);

    /* 특정 호텔을 이용한 고객의 최근 방문 날짜 (오늘 기준 가장 가까운 과거 날짜) */
    @Query("SELECT MAX(r.checkinDate) FROM RoomReservation r " +
           "WHERE r.customerIdx = :customerIdx " +
           "AND r.contentid = :contentid " +
           "AND r.status = 1 " +
           "AND r.checkinDate <= CURRENT_DATE")
    java.time.LocalDate findLastVisitDateByCustomerAndContentId(
            @Param("customerIdx") Integer customerIdx,
            @Param("contentid") String contentid);

    /* 고객 이용 이력 조회 (상태 2: 취소, 4: 완료만) */
    @Query("SELECT r FROM RoomReservation r " +
           "LEFT JOIN FETCH r.customer c " +
           "LEFT JOIN FETCH r.room rm " +
           "WHERE r.contentid = :contentid " +
           "AND r.status IN (1, 4) " +
           "AND (:customerId IS NULL OR c.id LIKE CONCAT('%', :customerId, '%')) " +
           "ORDER BY r.checkinDate DESC")
    List<RoomReservation> findCustomerHistoryByContentId(
            @Param("contentid") String contentid,
            @Param("customerId") String customerId);

    /* 특정 호텔의 전체 이용 이력 개수 (모든 예약 수) */
    @Query("SELECT COUNT(r) FROM RoomReservation r " +
           "WHERE r.contentid = :contentid")
    Long countTotalHistoryByContentId(@Param("contentid") String contentid);
}
