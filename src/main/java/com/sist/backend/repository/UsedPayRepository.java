package com.sist.backend.repository;

import com.sist.backend.entity.UsedPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsedPayRepository extends JpaRepository<UsedPay, Integer> {

    /**
     * 특정 거래의 결제 내역 조회
     * @param usedTradeIdx 거래 ID
     * @return 결제 내역 목록
     */
    List<UsedPay> findByUsedTradeIdxOrderByCreatedAtDesc(Integer usedTradeIdx);

    /**
     * 특정 거래의 최신 결제 내역 조회
     * @param usedTradeIdx 거래 ID
     * @return 최신 결제 내역 또는 null
     */
    @Query("SELECT up FROM UsedPay up " +
           "WHERE up.usedTradeIdx = :usedTradeIdx " +
           "ORDER BY up.createdAt DESC")
    Optional<UsedPay> findLatestPaymentByUsedTrade(@Param("usedTradeIdx") Integer usedTradeIdx);

    /**
     * 결제 키로 결제 내역 조회
     * @param paymentKey 토스페이먼츠 결제 키
     * @return 결제 내역 또는 null
     */
    Optional<UsedPay> findByPaymentKey(String paymentKey);

    /**
     * 주문 ID로 결제 내역 조회
     * @param orderId 주문 ID
     * @return 결제 내역 또는 null
     */
    Optional<UsedPay> findByOrderId(String orderId);

    /**
     * 특정 상태의 결제 내역 조회
     * @param status 결제 상태
     * @return 결제 내역 목록
     */
    List<UsedPay> findByStatusOrderByCreatedAtDesc(Integer status);
}
