package com.sist.backend.repository;

import java.util.List;

import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.UsedItem;
import com.sist.backend.entity.UsedTrade;

@Repository
public interface UsedTradeRepository extends JpaRepository<UsedTrade, Integer> {
    
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
    // @Query("SELECT u FROM UsedInfo u " +
    //     "JOIN u.roomReservation r ON u.reservIdx = r.reservIdx " +
    //     "JOIN r.roomPayment p ON r.reservIdx = p.reservIdx " +
    //     "WHERE u.status = 0 " +
    //     "AND (:areaCode IS NULL OR a.areaCode = :areaCode) " +
    //     "AND (:hotelName IS NULL OR hi.title LIKE %:hotelName%) " +
    //     "AND (:minPrice IS NULL OR u.price >= :minPrice) " +
    //     "AND (:maxPrice IS NULL OR u.price <= :maxPrice) " +
    //     "ORDER BY u.updatedAt DESC")
    // List<UsedInfo> findAllByStatusOrderByUpdatedAtDesc(String areaCode, String hotelName, Integer minPrice, Integer maxPrice, Pageable pageable);

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
}
