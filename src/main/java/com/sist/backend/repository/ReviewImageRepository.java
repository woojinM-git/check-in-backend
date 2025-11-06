package com.sist.backend.repository;

import com.sist.backend.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Integer> {
    
    /**
     * 특정 리뷰의 이미지 조회 (reviewIdx당 한 행만 존재)
     */
    Optional<ReviewImage> findByReviewIdx(Integer reviewIdx);
    
    /**
     * 특정 호텔의 모든 리뷰 이미지 조회 (contentid로 조회)
     */
    List<ReviewImage> findByContentid(String contentid);
    
    /**
     * 특정 리뷰의 이미지 삭제
     */
    void deleteByReviewIdx(Integer reviewIdx);
}

