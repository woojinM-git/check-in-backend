package com.sist.backend.repository;


import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomPayment;

@Repository
public interface RoomPaymentRepository extends JpaRepository<RoomPayment, Integer> {

    /* 특정 호텔의 오늘 매출 조회 (결제 승인일 기준) */
    @Query("SELECT COALESCE(SUM(rp.price), 0) FROM RoomPayment rp "
           + "INNER JOIN RoomReservation rr ON rp.orderIdx = rr.orderIdx "
           + "WHERE rr.contentid = :contentId "
           + "AND rp.status = 1 "
           + "AND FUNCTION('DATE', rp.approvedAt) = CURRENT_DATE")
    Long findByPrice(@Param("contentId") String contentId);
    
    @Query("SELECT rp FROM RoomPayment rp WHERE rp.paymentKey = :paymentKey AND rp.status = 1")
    Optional<RoomPayment> findByPaymentKeyAndStatus(String paymentKey);

    @Query("SELECT rp FROM RoomPayment rp " +
        "LEFT JOIN FETCH rp.roomReservations rr " +
        "LEFT JOIN FETCH rp.reservationTime rt " +
        "WHERE rr.contentid = :contentId " +
        "AND rt.inTime IS NULL " +
        "AND rr.status != 4")
    Page<RoomPayment> findByOrderIdxAndInTime(@Param("contentId") String contentId, Pageable pageable);

    @Query("SELECT rp FROM RoomPayment rp " +
        "LEFT JOIN FETCH rp.roomReservations rr " +
        "LEFT JOIN FETCH rp.reservationTime rt " +
        "WHERE rr.contentid = :contentId " +
        "AND rt.inTime IS NOT NULL " +
        "AND rt.outTime IS NULL " +
        "AND rr.status != 4")
    Page<RoomPayment> findByOrderIdxAndOutTime(@Param("contentId") String contentId, Pageable pageable);

    @Query("SELECT DISTINCT rp FROM RoomPayment rp " +
        "LEFT JOIN FETCH rp.roomReservations rr " +
        "WHERE rr.contentid = :contentId")
    List<RoomPayment> findAllByContentIdWithReservations(@Param("contentId") String contentId);

    /* 특정 호텔의 평균 결제 금액 */
    @Query("SELECT AVG(rp.price) FROM RoomPayment rp " +
           "INNER JOIN RoomReservation rr ON rp.orderIdx = rr.orderIdx " +
           "WHERE rr.contentid = :contentid AND rp.status = 1")
    Double findAveragePaymentByContentId(@Param("contentid") String contentid);

    /* 특정 호텔의 월별 총 수익 계산 (이용 완료된 예약만, 체크아웃일 기준) */
    @Deprecated
    @Query("SELECT COALESCE(SUM(rr.totalPrice), 0) FROM RoomReservation rr " +
           "WHERE rr.contentid = :contentId " +
           "AND rr.status = 4 " +
           "AND YEAR(rr.checkoutDate) = :year " +
           "AND MONTH(rr.checkoutDate) = :month")
    Long findTotalRevenueByContentIdAndMonth(
        @Param("contentId") String contentId,
        @Param("year") int year,
        @Param("month") int month);

    /**
     * 월별 호텔별 결제 금액 집계 (결제 기준)
     * RoomReservation.status = 4인 결제만 집계
     * totalRevenue = price + pointsUsed + cashUsed
     * 
     * @param year 연도
     * @param month 월 (1-12)
     * @return Map 리스트 (contentId, totalRevenue)
     */
    @Query("SELECT rr.contentid as contentId, " +
           "       COALESCE(SUM(COALESCE(rp.price, 0) + COALESCE(rp.pointsUsed, 0) + COALESCE(rp.cashUsed, 0)), 0) as totalRevenue " +
           "FROM RoomReservation rr " +
           "INNER JOIN RoomPayment rp ON rr.orderIdx = rp.orderIdx " +
           "WHERE rr.status = 4 " +
           "AND YEAR(rr.checkoutDate) = :year " +
           "AND MONTH(rr.checkoutDate) = :month " +
           "GROUP BY rr.contentid")
    List<Object[]> findMonthlyRevenueByHotel(
        @Param("year") int year,
        @Param("month") int month);

    /**
     * orderIdx(PK)로 결제 조회 (JpaRepository#findById로도 가능하지만 가독성을 위해 별도 노출)
     */
    default java.util.Optional<RoomPayment> findByOrderIdx(Integer orderIdx) {
        return this.findById(orderIdx);
    }
}
