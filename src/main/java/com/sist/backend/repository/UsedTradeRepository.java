package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * usedTrade에 들어갈건 reservIdx, customerId, 판매자가 원하는 금액(price)
     */
}
