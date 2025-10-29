package com.sist.backend.repository;

import com.sist.backend.entity.DiningReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DiningReservationRepository extends JpaRepository<DiningReservation, Integer> {

    /**
     * 고객별 다이닝 예약 내역 조회
     */
    @Query("SELECT dr FROM DiningReservation dr WHERE dr.customerIdx = :customerIdx ORDER BY dr.createdAt DESC")
    List<DiningReservation> findByCustomerIdx(@Param("customerIdx") Integer customerIdx);

    /**
     * 다이닝별 특정 날짜 예약 조회
     */
    @Query("SELECT dr FROM DiningReservation dr WHERE dr.diningIdx = :diningIdx AND dr.reservationDate = :date AND dr.status IN (0, 1)")
    List<DiningReservation> findByDiningIdxAndDate(@Param("diningIdx") Integer diningIdx, @Param("date") LocalDate date);

    /**
     * 결제 ID로 예약 조회
     */
    @Query("SELECT dr FROM DiningReservation dr WHERE dr.diningpayIdx = :diningpayIdx")
    DiningReservation findByDiningpayIdx(@Param("diningpayIdx") Integer diningpayIdx);
}

