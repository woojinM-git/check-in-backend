package com.sist.backend.service;

import com.sist.backend.entity.Review;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.ReviewRepository;
import com.sist.backend.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final RoomReservationRepository roomReservationRepository;
    
    /**
     * 리뷰 작성
     */
    @Transactional
    public Review createReview(Integer reservationIdx, Integer customerIdx, Integer rating, String content) {
        // 1. 예약 정보 조회 및 검증
        RoomReservation reservation = roomReservationRepository.findById(reservationIdx)
            .orElseThrow(() -> new RuntimeException("예약 정보를 찾을 수 없습니다."));
        
        // 2. 소유권 확인
        if (!reservation.getCustomerIdx().equals(customerIdx)) {
            throw new RuntimeException("본인의 예약만 리뷰를 작성할 수 있습니다.");
        }
        
        // 3. 이용완료 상태 확인
        if (reservation.getStatus() != 4) {
            throw new RuntimeException("이용 완료된 예약만 리뷰를 작성할 수 있습니다.");
        }
        
        // 4. 이미 리뷰를 작성했는지 확인
        boolean exists = reviewRepository.existsByReservIdxAndCustomerIdx(reservationIdx, customerIdx);
        if (exists) {
            throw new RuntimeException("이미 리뷰를 작성하셨습니다.");
        }
        
        // 5. 리뷰 생성 및 저장
        Review review = Review.builder()
            .reservIdx(reservationIdx)
            .customerIdx(customerIdx)
            .contentid(reservation.getContentid())
            .roomIdx(reservation.getRoomIdx())
            .orderIdx(reservation.getOrderIdx())
            .content(content)
            .star(BigDecimal.valueOf(rating))
            .status(false) // false: 활성
            .hide(false)   // false: 공개
            .build();
        
        return reviewRepository.save(review);
    }
    
    /**
     * 리뷰 작성 여부 확인
     */
    public boolean hasReview(Integer reservationIdx, Integer customerIdx) {
        return reviewRepository.existsByReservIdxAndCustomerIdx(reservationIdx, customerIdx);
    }
    
    /**
     * 고객의 작성 가능한 리뷰 목록 조회
     */
    public List<Integer> getReviewedReservationIds(Integer customerIdx) {
        return reviewRepository.findReservationIdsByCustomerIdx(customerIdx);
    }
}

