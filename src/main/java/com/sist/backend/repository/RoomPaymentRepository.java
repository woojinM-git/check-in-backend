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

    @Query("SELECT SUM(price) FROM RoomPayment WHERE status = 1")
    Long findByPrice();
    
    @Query("SELECT rp FROM RoomPayment rp WHERE rp.paymentKey = :paymentKey AND rp.status = 1")
    Optional<RoomPayment> findByPaymentKeyAndStatus(String paymentKey);

    @Query("SELECT rp FROM RoomPayment rp " +
        "LEFT JOIN FETCH rp.roomReservations rr " +
        "LEFT JOIN FETCH rp.reservationTime rt " +
        "WHERE rr.contentid = :contentId AND rt.inTime IS NULL")
    Page<RoomPayment> findByOrderIdxAndInTime(@Param("contentId") String contentId, Pageable pageable);

    @Query("SELECT rp FROM RoomPayment rp " +
        "LEFT JOIN FETCH rp.roomReservations rr " +
        "LEFT JOIN FETCH rp.reservationTime rt " +
        "WHERE rr.contentid = :contentId AND rt.inTime IS NOT NULL AND rt.outTime IS NULL")
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

    /**
     * orderIdx(PK)로 결제 조회 (JpaRepository#findById로도 가능하지만 가독성을 위해 별도 노출)
     */
    default java.util.Optional<RoomPayment> findByOrderIdx(Integer orderIdx) {
        return this.findById(orderIdx);
    }
}
