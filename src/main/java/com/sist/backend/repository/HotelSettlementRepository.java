package com.sist.backend.repository;

import com.sist.backend.entity.HotelSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelSettlementRepository extends JpaRepository<HotelSettlement, Integer> {

    @Query("SELECT hs FROM HotelSettlement hs " +
           "WHERE hs.contentId = :contentId " +
           "ORDER BY hs.settlementMonth DESC")
    List<HotelSettlement> findByContentId(@Param("contentId") String contentId);

    @Query("SELECT hs FROM HotelSettlement hs " +
           "WHERE hs.contentId = :contentId AND hs.settlementMonth = :settlementMonth")
    Optional<HotelSettlement> findByContentIdAndSettlementMonth(
        @Param("contentId") String contentId,
        @Param("settlementMonth") String settlementMonth);

    @Query("SELECT hs FROM HotelSettlement hs " +
           "ORDER BY hs.settlementMonth DESC, hs.contentId ASC")
    Page<HotelSettlement> findAllOrderByMonthDesc(Pageable pageable);

    @Query("SELECT hs FROM HotelSettlement hs " +
           "WHERE hs.settlementMonth = :settlementMonth " +
           "ORDER BY hs.contentId ASC")
    List<HotelSettlement> findBySettlementMonth(@Param("settlementMonth") String settlementMonth);
}

