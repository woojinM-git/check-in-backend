package com.sist.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import com.sist.backend.entity.RoomReservation;
import com.sist.backend.entity.UsedItem;

@Repository
public interface UsedItemRepository extends JpaRepository<UsedItem, Integer> {
    
    /*
     * 마이페이지에서 예약내역 확인, 추가로 양도거래조건에 따라 양도거래 표시
     * roomReservation의 customerId가 로그인한 사용자의 예약조건 표시
     */

    /*
     * 양도거래 표시 조건
     * roomReservation의 status가 1이고(예약완료)
     * roomPayment의 status가 1인 경우(결제완료)
     * 양도거래를 등록할 수 있는 버튼 표시
     */

    /*
     * 양도거래 등록
     * 받야아할 것
     * roomReservation의 reservIdx(integer) -> 으로 아래 것들 조회 가능
     * roomIdx(integer)
     * customerId(string)
     * checkinDate(string)
     * checkoutDate(string)
     * guest(integer)
     * totalPrice(integer)
     *                           <-
     * usedTrade에 들어갈건 reservIdx, customerId, 판매자가 원하는 금액(price), status(0: 판매중, 1: 판매완료, 2: 판매취소), created_at, updated_at
     */

    /*
     * 양도거래 취소
     * 받야아할 것
     * usedTrade의 usedInfoIdx(integer)
     * customerId(string)
     */


    /*
     * 매물 확인(기본 updated_at 순서)
     * 만약 지역, 호텔명, 가격 검색 조건이 있으면 해당 조건에 맞는 매물 확인
     */
    @Query("SELECT u FROM UsedItem u " +
        "JOIN FETCH u.roomReservation r " +
        "JOIN r.roomPayment p " +
        "WHERE u.status = 0 " +
        "ORDER BY u.updatedAt DESC")
    Page<UsedItem> findAllByStatusOrderByUpdatedAtDesc(Pageable pageable);
    
    // 방법 2: 특정 reservIdx로 RoomReservation 정보 조회
    @Query("SELECT r FROM RoomReservation r WHERE r.reservIdx = :reservIdx")
    RoomReservation findRoomReservationByReservIdx(@Param("reservIdx") Integer reservIdx);

    // 특정 reservIdx에 대한 양도거래 아이템 조회
    @Query("SELECT u FROM UsedItem u WHERE u.reservIdx = :reservIdx")
    UsedItem findByReservIdx(@Param("reservIdx") Integer reservIdx);

    /*
     * 복합 조건 검색
     */
    @Query("SELECT u FROM UsedItem u " +
        "JOIN FETCH u.roomReservation r " +
        "JOIN FETCH r.room ro " +
        "JOIN FETCH ro.hotelInfo h " +
        "WHERE " +
        "(:destination IS NULL OR h.adress LIKE CONCAT('%', :destination, '%') OR h.title LIKE CONCAT('%', :destination, '%')) AND " +
        "(:checkIn IS NULL OR r.checkinDate >= :checkIn) AND " +
        "(:checkOut IS NULL OR r.checkoutDate <= :checkOut) AND " +
        "(:adults IS NULL OR r.guest >= :adults) AND " +
        "(:priceMin IS NULL OR u.price >= :priceMin) AND " +
        "(:priceMax IS NULL OR u.price <= :priceMax) AND " +
        "(:status IS NULL OR u.status = 0) " +
        "ORDER BY " +
        "CASE WHEN :sortBy = 'date' AND :sortDirection = 'asc' THEN r.checkinDate END ASC, " +
        "CASE WHEN :sortBy = 'date' AND :sortDirection = 'desc' THEN r.checkinDate END DESC, " +
        "CASE WHEN :sortBy = 'price' AND :sortDirection = 'asc' THEN u.price END ASC, " +
        "CASE WHEN :sortBy = 'price' AND :sortDirection = 'desc' THEN u.price END DESC, " +
        "CASE WHEN :sortBy = 'discount' AND :sortDirection = 'asc' AND r.totalPrice > 0 THEN ((r.totalPrice - u.price) / r.totalPrice * 100) END ASC, " +
        "CASE WHEN :sortBy = 'discount' AND :sortDirection = 'desc' AND r.totalPrice > 0 THEN ((r.totalPrice - u.price) / r.totalPrice * 100) END DESC, " +
        "u.updatedAt DESC")
    Page<UsedItem> findByMultipleConditions(
        @Param("destination") String destination,
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut,
        @Param("adults") Integer adults,
        @Param("priceMin") Integer priceMin,
        @Param("priceMax") Integer priceMax,
        @Param("sortBy") String sortBy,
        @Param("sortDirection") String sortDirection,
        @Param("status") Integer status,
        Pageable pageable
    );

    /*
     * 결제 처리
     * 받야아할 것
     * usedTrade의 usedInfoIdx(integer)
     * customerId(string)
     * price(integer)
     * status(0: 판매중, 1: 판매완료, 2: 판매취소)
     * created_at(timestamp)
     * updated_at(timestamp)
     * 
     * 토스페이 사용
     */

    /*
     * checkin 날짜가 오늘 날짜와 같거나 이전인 양도매물 조회 (판매중인 매물만)
     */
    @Query("SELECT u FROM UsedItem u " +
        "JOIN FETCH u.roomReservation r " +
        "WHERE u.status = 0 AND r.checkinDate <= :today")
    List<UsedItem> findExpiredUsedItems(@Param("today") LocalDate today);

    /*
     * 판매자의 양도거래 아이템 목록 조회 (sellerIdx 기준)
     * roomReservation이 존재하는 경우만 조회 (JOIN FETCH 사용)
     */
    @Query("SELECT DISTINCT u FROM UsedItem u " +
        "JOIN FETCH u.roomReservation r " +
        "JOIN FETCH r.room room " +
        "JOIN FETCH room.hotelInfo hotel " +
        "LEFT JOIN FETCH hotel.area area " +
        "LEFT JOIN FETCH r.customer customer " +
        "WHERE u.sellerIdx = :sellerIdx " +
        "ORDER BY u.createdAt DESC")
    List<UsedItem> findBySellerIdx(@Param("sellerIdx") Integer sellerIdx);
}
