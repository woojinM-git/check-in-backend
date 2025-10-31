package com.sist.backend.service;

import com.sist.backend.entity.Review;
import com.sist.backend.entity.HotelInfo;
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
    
    /**
     * 고객이 작성한 리뷰 목록 조회
     */
    public List<com.sist.backend.dto.mypage.WrittenReviewDTO> getMyReviews(Integer customerIdx) {
        List<Review> reviews = reviewRepository.findByCustomerIdx(customerIdx);
        
        // Review → DTO 변환
        return reviews.stream().map(review -> {
            com.sist.backend.dto.mypage.WrittenReviewDTO dto = com.sist.backend.dto.mypage.WrittenReviewDTO.builder()
                .reviewIdx(review.getReviewIdx())
                .reservationIdx(review.getReservIdx())
                .contentId(review.getContentid())
                .roomIdx(review.getRoomIdx())
                .star(review.getStar())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
            
            // 호텔 정보 매핑
            HotelInfo hotelInfoEntity = review.getHotelInfo();
            if (hotelInfoEntity != null) {
                com.sist.backend.dto.mypage.WrittenReviewDTO.HotelInfoDTO hotelInfo = new com.sist.backend.dto.mypage.WrittenReviewDTO.HotelInfoDTO();
                hotelInfo.setContentId(hotelInfoEntity.getContentId());
                hotelInfo.setTitle(hotelInfoEntity.getTitle());
                hotelInfo.setAdress(hotelInfoEntity.getAdress());
                hotelInfo.setTel(hotelInfoEntity.getTel());
                dto.setHotelInfo(hotelInfo);
                dto.setHotelName(hotelInfoEntity.getTitle());
                // region: adress 첫 단어 우선, area.areaName 있으면 대체
                String region = null;
                if (hotelInfoEntity.getArea() != null && hotelInfoEntity.getArea().getAreaName() != null) {
                    region = hotelInfoEntity.getArea().getAreaName();
                } else if (hotelInfoEntity.getAdress() != null && !hotelInfoEntity.getAdress().isBlank()) {
                    String[] tokens = hotelInfoEntity.getAdress().trim().split("\\s+");
                    region = tokens.length > 0 ? tokens[0] : null;
                }
                dto.setRegion(region);
                // 썸네일: imageUrl 있으면 사용, 없으면 프록시 경로
                String thumbnail = (hotelInfoEntity.getImageUrl() != null && !hotelInfoEntity.getImageUrl().isBlank())
                    ? hotelInfoEntity.getImageUrl()
                    : ("/api/hotels/" + hotelInfoEntity.getContentId() + "/thumbnail");
                dto.setThumbnailUrl(thumbnail);
            }

            // 예약 날짜 매핑: roomReservation 에서 가져옴
            if (review.getRoomReservation() != null) {
                dto.setCheckInDate(review.getRoomReservation().getCheckinDate());
                dto.setCheckOutDate(review.getRoomReservation().getCheckoutDate());
            }
            
            return dto;
        }).toList();
    }

    /**
     * 리뷰 수정 (내용만 수정, 별점은 수정 불가)
     */
    @Transactional
    public Review updateReview(Integer reviewIdx, Integer customerIdx, String newContent) {
        Review review = reviewRepository.findById(reviewIdx)
            .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getCustomerIdx().equals(customerIdx)) {
            throw new RuntimeException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new RuntimeException("리뷰 내용을 입력해주세요.");
        }

        review.setContent(newContent.trim());

        return reviewRepository.save(review);
    }
}

