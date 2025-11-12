package com.sist.backend.repository;

import com.sist.backend.entity.RecentViewedHotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecentViewedHotelRepository extends JpaRepository<RecentViewedHotel, Integer> {

    Optional<RecentViewedHotel> findByCustomerIdxAndContentId(Integer customerIdx, String contentId);

    Optional<RecentViewedHotel> findByRecentViewedIdxAndCustomerIdx(Integer recentViewedIdx, Integer customerIdx);

    Page<RecentViewedHotel> findAllByCustomerIdxOrderByViewedAtDesc(Integer customerIdx, Pageable pageable);

    Optional<RecentViewedHotel> findFirstByCustomerIdxOrderByViewedAtAsc(Integer customerIdx);

    long countByCustomerIdx(Integer customerIdx);

    void deleteByCustomerIdx(Integer customerIdx);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RecentViewedHotel r WHERE r.createdAt < :cutoff")
    int deleteAllByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}

