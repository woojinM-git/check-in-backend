package com.sist.backend.repository;

import com.sist.backend.entity.UsedTrade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsedTradeRepository extends JpaRepository<UsedTrade, Integer> {

    /**
     * 특정 중고 아이템의 활성 거래 조회 (동시성 제어용)
     * @param usedItemIdx 중고 아이템 ID
     * @return 활성 거래 (status: 0=거래중, 1=거래완료, 2=거래취소)
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.userItemIdx = :usedItemIdx " +
           "AND ut.ststus IN (0, 1) " + // 거래중 또는 거래완료
           "ORDER BY ut.usedTradeIdx DESC")
    List<UsedTrade> findActiveTradesByUsedItem(@Param("usedItemIdx") Integer usedItemIdx);

    /**
     * 특정 중고 아이템의 거래 가능 여부 확인
     * @param usedItemIdx 중고 아이템 ID
     * @return 거래 가능 여부
     */
    @Query("SELECT COUNT(ut) = 0 FROM UsedTrade ut " +
           "WHERE ut.userItemIdx = :usedItemIdx " +
           "AND ut.ststus IN (0, 1)")
    boolean isUsedItemAvailable(@Param("usedItemIdx") Integer usedItemIdx);

    /**
     * 특정 중고 아이템의 최신 거래 조회
     * @param usedItemIdx 중고 아이템 ID
     * @return 최신 거래 또는 null
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.userItemIdx = :usedItemIdx " +
           "ORDER BY ut.usedTradeIdx DESC")
    Optional<UsedTrade> findLatestTradeByUsedItem(@Param("usedItemIdx") Integer usedItemIdx);

    /**
     * 특정 구매자의 거래 조회
     * @param buyerIdx 구매자 ID
     * @return 구매자의 거래 목록
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.buyerIdx = :buyerIdx " +
           "ORDER BY ut.usedTradeIdx DESC")
    List<UsedTrade> findByBuyerIdx(@Param("buyerIdx") Integer buyerIdx);

    /**
     * 특정 판매자의 거래 조회
     * @param sellerIdx 판매자 ID
     * @return 판매자의 거래 목록
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.sellerIdx = :sellerIdx " +
           "ORDER BY ut.usedTradeIdx DESC")
    List<UsedTrade> findBySellerIdx(@Param("sellerIdx") Integer sellerIdx);

    /**
     * 특정 예약과 판매자로 거래 조회 (판매완료된 예약 권한 확인용)
     * @param reservIdx 예약 ID
     * @param sellerIdx 판매자 ID
     * @return 거래 정보 (존재하는 경우)
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.reservIdx = :reservIdx " +
           "AND ut.sellerIdx = :sellerIdx " +
           "AND ut.ststus = 1 " + // 거래완료 상태
           "ORDER BY ut.usedTradeIdx DESC")
    Optional<UsedTrade> findByReservIdxAndSellerIdx(
            @Param("reservIdx") Integer reservIdx,
            @Param("sellerIdx") Integer sellerIdx);

    /**
     * 오래된 대기 거래 조회 (정리용)
     * @param cutoffTime 기준 시간
     * @return 오래된 대기 거래 목록
     */
    @Query("SELECT ut FROM UsedTrade ut " +
           "WHERE ut.ststus = 0 " + // 거래중 상태
           "AND ut.createdAt < :cutoffTime")
    List<UsedTrade> findExpiredPendingTrades(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 거래 조회 (Pessimistic Lock - 결제 처리 시 사용)
     * SELECT FOR UPDATE로 row를 잠가서 다른 트랜잭션의 수정을 방지
     * @param usedTradeIdx 거래 ID
     * @return 거래 정보 (락이 걸린 상태)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ut FROM UsedTrade ut WHERE ut.usedTradeIdx = :usedTradeIdx")
    Optional<UsedTrade> findByIdForUpdate(@Param("usedTradeIdx") Integer usedTradeIdx);
}
