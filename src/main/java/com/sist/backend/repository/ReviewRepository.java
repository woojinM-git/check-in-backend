package com.sist.backend.repository;

import com.sist.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * 고객이 작성한 리뷰 조회 (호텔/예약 정보 포함) - N+1 방지
     */
    @Query("SELECT DISTINCT r FROM Review r " +
        "JOIN FETCH r.roomReservation rr " +
        "JOIN FETCH r.hotelInfo h " +
        "LEFT JOIN FETCH h.area a " +
        "WHERE r.customerIdx = :customerIdx " +
        "ORDER BY r.createdAt DESC")
    List<Review> findByCustomerIdx(@Param("customerIdx") Integer customerIdx);

    /**
     * 특정 호텔의 예약별 가장 최근 리뷰 조회
     */
    @Query("SELECT r FROM Review r " +
           "WHERE r.contentid = :contentid " +
           "AND r.reservIdx = :reservIdx " +
           "ORDER BY r.createdAt DESC")
    List<Review> findLatestByContentIdAndReservIdx(
            @Param("contentid") String contentid,
            @Param("reservIdx") Integer reservIdx);

    /**
     * 특정 호텔의 평균 평점
     */
    @Query("SELECT AVG(r.star) FROM Review r " +
           "WHERE r.contentid = :contentid " +
           "AND r.status = false " +
           "AND r.hide = false")
    BigDecimal findAverageRatingByContentId(@Param("contentid") String contentid);

    /**
     * 특정 호텔의 피드백 갯수 (리뷰가 작성된 예약 수)
     */
    @Query("SELECT COUNT(DISTINCT r.reservIdx) FROM Review r " +
           "WHERE r.contentid = :contentid " +
           "AND r.status = false " +
           "AND r.hide = false")
    Long countFeedbackByContentId(@Param("contentid") String contentid);
    
    /**
     * 특정 호텔의 리뷰 목록 조회 (답변 포함)
     */
    @Query("SELECT r FROM Review r " +
           "WHERE r.contentid = :contentid " +
           "AND r.status = false " +
           "AND r.hide = false " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByContentId(@Param("contentid") String contentid);
}

