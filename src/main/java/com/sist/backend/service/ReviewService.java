package com.sist.backend.service;

import com.sist.backend.dto.admin.FeedbackDto;
import com.sist.backend.dto.admin.FeedbackStatsDto;
import com.sist.backend.entity.Review;
import com.sist.backend.entity.ReviewAnswer;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.Room;
import com.sist.backend.repository.ReviewRepository;
import com.sist.backend.repository.ReviewAnswerRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    
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
     * 특정 호텔의 평균 평점
     */
    public BigDecimal getAverageRatingByContentId(String contentid) {
        return reviewRepository.findAverageRatingByContentId(contentid);
    }

    /**
     * 특정 호텔의 피드백 갯수
     */
    public Long getFeedbackCountByContentId(String contentid) {
        return reviewRepository.countFeedbackByContentId(contentid);
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
    
    /**
     * 특정 호텔의 피드백 목록 조회 (답변 포함)
     */
    @Transactional(readOnly = true)
    public List<FeedbackDto> getFeedbacksByContentId(String contentid) {
        List<Review> reviews = reviewRepository.findByContentId(contentid);
        
        return reviews.stream().map(review -> {
            FeedbackDto.FeedbackDtoBuilder builder = FeedbackDto.builder()
                .reviewIdx(review.getReviewIdx())
                .reservIdx(review.getReservIdx())
                .customerIdx(review.getCustomerIdx())
                .star(review.getStar())
                .content(review.getContent())
                .createdAt(review.getCreatedAt());
            
            // 고객 정보 조회
            Optional<Customer> customerOpt = customerRepository.findByCustomerIdx(review.getCustomerIdx());
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                builder.customerId(customer.getId())
                       .customerName(customer.getName());
            }
            
            // 예약 및 객실 정보 조회
            Optional<RoomReservation> reservationOpt = roomReservationRepository.findById(review.getReservIdx());
            if (reservationOpt.isPresent()) {
                RoomReservation reservation = reservationOpt.get();
                // RoomReservation의 room 관계를 통해 정보 가져오기
                if (reservation.getRoom() != null) {
                    Room room = reservation.getRoom();
                    builder.roomNumber(String.valueOf(room.getRoomIdx()))
                           .roomName(room.getName());
                } else {
                    // room 관계가 로드되지 않은 경우 직접 조회
                    Optional<Room> roomOpt = roomRepository.findByRoomIdx(reservation.getRoomIdx());
                    if (roomOpt.isPresent()) {
                        Room room = roomOpt.get();
                        if (room.getContentId().equals(reservation.getContentid())) {
                            builder.roomNumber(String.valueOf(room.getRoomIdx()))
                                   .roomName(room.getName());
                        }
                    }
                }
            }
            
            // 답변 정보 조회
            Optional<ReviewAnswer> answerOpt = reviewAnswerRepository.findActiveByReviewIdx(review.getReviewIdx());
            if (answerOpt.isPresent()) {
                ReviewAnswer answer = answerOpt.get();
                builder.reviewAnswerIdx(answer.getReviewAnswerIdx())
                       .response(answer.getContent())
                       .responseStatus(answer.getStatus())
                       .responseCreatedAt(answer.getCreatedAt())
                       .responseUpdatedAt(answer.getUpdatedAt());
                
                // 상태 설정 (답변이 있으면 처리중)
                builder.status("in-progress");
            } else {
                // 답변이 없으면 신규
                builder.status("new");
            }
            
            // 카테고리는 기본값으로 설정 (추후 확장 가능)
            builder.category("service");
            
            return builder.build();
        }).collect(Collectors.toList());
    }
    
    /**
     * 리뷰 답변 작성
     */
    @Transactional
    public ReviewAnswer createReviewAnswer(Integer reviewIdx, Integer adminIdx, String content) {
        // 리뷰 존재 확인
        Review review = reviewRepository.findById(reviewIdx)
            .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        // 이미 답변이 있는지 확인 (활성 답변)
        Optional<ReviewAnswer> existingAnswer = reviewAnswerRepository.findActiveByReviewIdx(reviewIdx);
        if (existingAnswer.isPresent()) {
            throw new RuntimeException("이미 답변이 작성되어 있습니다.");
        }
        
        // 답변 생성
        ReviewAnswer answer = ReviewAnswer.builder()
            .reviewIdx(reviewIdx)
            .adminIdx(adminIdx)
            .content(content.trim())
            .status(1) // 활성
            .build();
        
        return reviewAnswerRepository.save(answer);
    }
    
    /**
     * 리뷰 답변 수정
     */
    @Transactional
    public ReviewAnswer updateReviewAnswer(Integer reviewAnswerIdx, Integer adminIdx, String content) {
        ReviewAnswer answer = reviewAnswerRepository.findById(reviewAnswerIdx)
            .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
        
        // 권한 확인
        if (!answer.getAdminIdx().equals(adminIdx)) {
            throw new RuntimeException("본인이 작성한 답변만 수정할 수 있습니다.");
        }
        
        answer.setContent(content.trim());
        
        return reviewAnswerRepository.save(answer);
    }
    
    /**
     * 리뷰 답변 삭제 (상태 비활성화)
     */
    @Transactional
    public void deleteReviewAnswer(Integer reviewAnswerIdx, Integer adminIdx) {
        ReviewAnswer answer = reviewAnswerRepository.findById(reviewAnswerIdx)
            .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
        
        // 권한 확인
        if (!answer.getAdminIdx().equals(adminIdx)) {
            throw new RuntimeException("본인이 작성한 답변만 삭제할 수 있습니다.");
        }
        
        answer.setStatus(0); // 비활성화
        reviewAnswerRepository.save(answer);
    }
    
    /**
     * 피드백 통계 조회
     */
    @Transactional(readOnly = true)
    public FeedbackStatsDto getFeedbackStats(String contentid) {
        List<Review> reviews = reviewRepository.findByContentId(contentid);
        
        long totalCount = reviews.size();
        long urgentCount = 0; // 긴급 카운트 (추후 로직 추가 가능)
        long inProgressCount = 0;
        long resolvedCount = 0;
        
        for (Review review : reviews) {
            Optional<ReviewAnswer> answerOpt = reviewAnswerRepository.findActiveByReviewIdx(review.getReviewIdx());
            if (answerOpt.isPresent()) {
                inProgressCount++;
            } else {
                resolvedCount++; // 답변이 없으면 해결 필요 상태로 간주
            }
        }
        
        return FeedbackStatsDto.builder()
            .totalFeedback(totalCount)
            .urgentFeedback(urgentCount)
            .inProgressFeedback(inProgressCount)
            .resolvedFeedback(resolvedCount)
            .build();
    }
}

