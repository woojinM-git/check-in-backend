package com.sist.backend.repository;

import com.sist.backend.entity.DiningReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 마이페이지 다이닝 예약 목록 조회 (Dining 정보 포함) - 페이지네이션 미지원
     */
    @Query("SELECT DISTINCT dr FROM DiningReservation dr " +
           "LEFT JOIN FETCH dr.dining dining " +
           "WHERE dr.customerIdx = :customerIdx AND dr.status IN :statusList " +
           "ORDER BY dr.reservationDate DESC, dr.reservationTime DESC")
    List<DiningReservation> findByCustomerIdxAndStatus(
            @Param("customerIdx") Integer customerIdx,
            @Param("statusList") List<Integer> statusList);

    /**
     * 마이페이지 다이닝 예약 목록 조회 (Dining 정보 포함) - 페이지네이션 지원
     */
    @Query("SELECT DISTINCT dr FROM DiningReservation dr " +
           "LEFT JOIN FETCH dr.dining dining " +
           "WHERE dr.customerIdx = :customerIdx AND dr.status IN :statusList " +
           "ORDER BY dr.reservationDate DESC, dr.reservationTime DESC")
    Page<DiningReservation> findByCustomerIdxAndStatusWithPagination(
            @Param("customerIdx") Integer customerIdx,
            @Param("statusList") List<Integer> statusList,
            Pageable pageable);
}

