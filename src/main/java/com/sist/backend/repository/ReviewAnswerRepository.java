package com.sist.backend.repository;

import com.sist.backend.entity.ReviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswer, Integer> {
    
    /**
     * 특정 리뷰의 답변 조회
     */
    Optional<ReviewAnswer> findByReviewIdx(Integer reviewIdx);
    
    /**
     * 특정 관리자가 작성한 답변 목록 조회
     */
    List<ReviewAnswer> findByAdminIdx(Integer adminIdx);
    
    /**
     * 활성 상태인 답변만 조회
     */
    @Query("SELECT ra FROM ReviewAnswer ra WHERE ra.status = 1")
    List<ReviewAnswer> findAllActive();
    
    /**
     * 특정 리뷰의 활성 답변 조회
     */
    @Query("SELECT ra FROM ReviewAnswer ra WHERE ra.reviewIdx = :reviewIdx AND ra.status = 1")
    Optional<ReviewAnswer> findActiveByReviewIdx(@Param("reviewIdx") Integer reviewIdx);
}

