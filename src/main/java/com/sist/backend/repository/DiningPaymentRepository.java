package com.sist.backend.repository;

import com.sist.backend.entity.DiningPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiningPaymentRepository extends JpaRepository<DiningPayment, Integer> {

    /**
     * paymentKey로 결제 정보 조회 (중복 결제 방지)
     */
    @Query("SELECT dp FROM DiningPayment dp WHERE dp.paymentKey = :paymentKey AND dp.status = 1")
    Optional<DiningPayment> findByPaymentKeyAndStatus(@Param("paymentKey") String paymentKey);

    /**
     * 고객별 결제 내역 조회
     */
    @Query("SELECT dp FROM DiningPayment dp WHERE dp.customerIdx = :customerIdx ORDER BY dp.createdAt DESC")
    java.util.List<DiningPayment> findByCustomerIdx(@Param("customerIdx") Integer customerIdx);
}

