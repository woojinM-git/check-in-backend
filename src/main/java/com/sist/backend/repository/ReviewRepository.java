package com.sist.backend.repository;

import com.sist.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    /**
     * 특정 예약의 리뷰 조회 (Spring Data JPA 자동 메서드명 해석)
     */
    Optional<Review> findByReservIdxAndCustomerIdx(Integer reservIdx, Integer customerIdx);
    
    /**
     * 특정 예약에 대해 리뷰가 존재하는지 확인 (boolean)
     */
    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.reservIdx = :reservIdx AND r.customerIdx = :customerIdx")
    boolean existsByReservIdxAndCustomerIdx(@Param("reservIdx") Integer reservIdx, @Param("customerIdx") Integer customerIdx);
    
    /**
     * 고객이 이미 작성한 리뷰의 예약 ID 목록 조회
     */
    @Query("SELECT r.reservIdx FROM Review r WHERE r.customerIdx = :customerIdx")
    List<Integer> findReservationIdsByCustomerIdx(@Param("customerIdx") Integer customerIdx);
    
    /**
     * 고객이 작성한 리뷰 조회 (호텔 정보 포함)
     */
    @Query("SELECT r FROM Review r " +
        "LEFT JOIN FETCH r.hotelInfo h " +
        "WHERE r.customerIdx = :customerIdx " +
        "ORDER BY r.createdAt DESC")
    List<Review> findByCustomerIdx(@Param("customerIdx") Integer customerIdx);
}

