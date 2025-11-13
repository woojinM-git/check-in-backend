package com.sist.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.admin.CustomerHistoryDto;
import com.sist.backend.dto.admin.RoomReservationDto;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.entity.Review;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomReservationService {
    // ********* 이건 예약 조회 통계용 서비스 ************
    private final RoomReservationRepository roomReservationRepository;
    private final ReviewRepository reviewRepository;

    /* 오늘 체크인한 사람의 수 조회 (특정 호텔) */
    public Integer getTodayCheckinCount(String contentId) {
        return roomReservationRepository.findTodayCheckinCount(contentId);
    }

    /* 오늘 체크아웃한 사람의 수 조회 (특정 호텔) */
    public Integer getTodayCheckoutCount(String contentId) {
        return roomReservationRepository.findTodayCheckoutCount(contentId);
    }

    /* 오늘 예약한 사람의 수 조회 (특정 호텔) */
    public Integer findTodayReservationCount(String contentId) {
        return roomReservationRepository.findByTodayCount(contentId);
    }

    /* 가장 최근 예약한 사람의 목록 (5개만) */
    public List<RoomReservation> findByStatus(String contentid) {
        return roomReservationRepository.findByStatus(contentid);
    }

    /* Room과 Customer 정보를 포함한 예약 목록을 DTO로 반환 */
    public List<RoomReservationDto> findByStatusWithDetails(String contentid) {
        List<RoomReservation> reservations = roomReservationRepository.findByStatusWithDetails(contentid);
        return reservations.stream()
            .map(RoomReservationDto::fromEntity)
            .collect(Collectors.toList());
    }

    /* Room과 Customer 정보를 포함한 최근 예약 5개만 DTO로 반환 (대시보드용) */
    public List<RoomReservationDto> findTop5ByStatusWithDetails(String contentid) {
        List<RoomReservation> reservations = roomReservationRepository.findTop5ByStatusWithDetails(contentid);
        // 최근 5개만 반환
        return reservations.stream()
            .limit(5)
            .map(RoomReservationDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Page<RoomReservationDto> findByStatusDto(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findByStatusDto(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 체크인 대기 목록 */
    public Page<RoomReservationDto> findCheckinPendingWithDetails(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findCheckinPendingWithDetails(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 체크아웃 대기 목록 */
    public Page<RoomReservationDto> findCheckoutPendingWithDetails(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findCheckoutPendingWithDetails(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 오늘 예약한 사람의 수 조회 (전체 호텔 - 마스터용) */
    public Integer findByTodayCount() {
        return roomReservationRepository.findByTodayCountAll();
    }

    /* 특정 예약의 customerIdx 값을 변경하는 메서드 */
    @Transactional
    public int updateCustomerIdxByReservIdx(Integer reservIdx, Integer customerIdx) {
        return roomReservationRepository.updateCustomerIdxByReservIdx(reservIdx, customerIdx);
    }

    /* 달력용 예약 조회 - 날짜 범위로 검색 */
    public List<RoomReservationDto> findByDateRangeWithDetails(String contentid, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<RoomReservation> reservations = roomReservationRepository.findByDateRangeWithDetails(contentid, startDate, endDate);
        return reservations.stream()
            .map(RoomReservationDto::fromEntity)
            .collect(Collectors.toList());
    }

    /* 특정 호텔을 이용한 기록이 있는 고객 수 */
    public Long countDistinctCustomersByContentId(String contentid) {
        return roomReservationRepository.countDistinctCustomersByContentId(contentid);
    }

    /* 이번 달 새로 이용을 시작한 고객 수 */
    public Long countNewCustomersThisMonth(String contentid) {
        return roomReservationRepository.countNewCustomersThisMonth(contentid);
    }

    /* 고객 이용 이력 조회 (상태 필터, 평점 필터 적용) - 페이징 미지원 (하위 호환성) */
    public List<CustomerHistoryDto> findCustomerHistory(
            String contentid, 
            String customerId, 
            Integer statusFilter, 
            Integer ratingFilter) {
        
        // customerId가 null이면 빈 문자열로 처리
        String searchCustomerId = (customerId == null || customerId.trim().isEmpty()) ? null : customerId.trim();
        
        // 예약 조회
        List<RoomReservation> reservations = roomReservationRepository.findCustomerHistoryByContentId(contentid, searchCustomerId);
        
        return reservations.stream()
            .map(reservation -> {
                // 상태 필터 적용
                if (statusFilter != null && !statusFilter.equals(reservation.getStatus())) {
                    return null;
                }
                
                // 최근 리뷰 조회 (해당 예약의 가장 최근 리뷰)
                List<Review> reviews = reviewRepository.findLatestByContentIdAndReservIdx(contentid, reservation.getReservIdx());
                Optional<Review> latestReview = reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
                
                // 평점 필터 적용
                if (ratingFilter != null && latestReview.isPresent()) {
                    BigDecimal reviewStar = latestReview.get().getStar();
                    if (reviewStar == null || reviewStar.intValue() != ratingFilter) {
                        return null;
                    }
                } else if (ratingFilter != null && !latestReview.isPresent()) {
                    // 평점 필터가 있는데 리뷰가 없으면 제외
                    return null;
                }
                
                CustomerHistoryDto dto = new CustomerHistoryDto();
                dto.setReservIdx(reservation.getReservIdx());
                dto.setCustomerIdx(reservation.getCustomerIdx());
                
                // 고객 정보
                if (reservation.getCustomer() != null) {
                    dto.setCustomerName(reservation.getCustomer().getName());
                    dto.setCustomerId(reservation.getCustomer().getId());
                }
                
                // 객실 정보
                if (reservation.getRoom() != null) {
                    dto.setRoomName(reservation.getRoom().getName());
                }
                
                // 숙박 일정
                dto.setCheckinDate(reservation.getCheckinDate());
                dto.setCheckoutDate(reservation.getCheckoutDate());
                
                // 예약 금액
                dto.setTotalPrice(reservation.getTotalPrice());
                
                // 상태
                dto.setStatus(reservation.getStatus());
                
                // 평점 정보 (최근 리뷰만)
                if (latestReview.isPresent()) {
                    Review review = latestReview.get();
                    dto.setStar(review.getStar());
                    dto.setReviewContent(review.getContent());
                    if (review.getCreatedAt() != null) {
                        dto.setReviewCreatedAt(review.getCreatedAt().toLocalDate());
                    }
                }
                
                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    /* 고객 이용 이력 조회 (상태 필터, 평점 필터 적용) - 페이징 지원 */
    public Page<CustomerHistoryDto> findCustomerHistoryWithPagination(
            String contentid, 
            String customerId, 
            Integer statusFilter, 
            Integer ratingFilter,
            Pageable pageable) {
        
        // customerId가 null이면 빈 문자열로 처리
        String searchCustomerId = (customerId == null || customerId.trim().isEmpty()) ? null : customerId.trim();
        
        // 예약 조회 (페이징)
        Page<RoomReservation> reservationPage = roomReservationRepository.findCustomerHistoryByContentIdWithPagination(contentid, searchCustomerId, pageable);
        
        // DTO 변환 및 필터링
        List<CustomerHistoryDto> dtoList = reservationPage.getContent().stream()
            .map(reservation -> {
                // 상태 필터 적용
                if (statusFilter != null && !statusFilter.equals(reservation.getStatus())) {
                    return null;
                }
                
                // 최근 리뷰 조회 (해당 예약의 가장 최근 리뷰)
                List<Review> reviews = reviewRepository.findLatestByContentIdAndReservIdx(contentid, reservation.getReservIdx());
                Optional<Review> latestReview = reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
                
                // 평점 필터 적용
                if (ratingFilter != null && latestReview.isPresent()) {
                    BigDecimal reviewStar = latestReview.get().getStar();
                    if (reviewStar == null || reviewStar.intValue() != ratingFilter) {
                        return null;
                    }
                } else if (ratingFilter != null && !latestReview.isPresent()) {
                    // 평점 필터가 있는데 리뷰가 없으면 제외
                    return null;
                }
                
                CustomerHistoryDto dto = new CustomerHistoryDto();
                dto.setReservIdx(reservation.getReservIdx());
                dto.setCustomerIdx(reservation.getCustomerIdx());
                
                // 고객 정보
                if (reservation.getCustomer() != null) {
                    dto.setCustomerName(reservation.getCustomer().getName());
                    dto.setCustomerId(reservation.getCustomer().getId());
                }
                
                // 객실 정보
                if (reservation.getRoom() != null) {
                    dto.setRoomName(reservation.getRoom().getName());
                }
                
                // 숙박 일정
                dto.setCheckinDate(reservation.getCheckinDate());
                dto.setCheckoutDate(reservation.getCheckoutDate());
                
                // 예약 금액
                dto.setTotalPrice(reservation.getTotalPrice());
                
                // 상태
                dto.setStatus(reservation.getStatus());
                
                // 평점 정보 (최근 리뷰만)
                if (latestReview.isPresent()) {
                    Review review = latestReview.get();
                    dto.setStar(review.getStar());
                    dto.setReviewContent(review.getContent());
                    if (review.getCreatedAt() != null) {
                        dto.setReviewCreatedAt(review.getCreatedAt().toLocalDate());
                    }
                }
                
                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
        
        // 필터링 후 실제 개수로 Page 객체 생성
        // 주의: 필터링으로 인해 실제 반환되는 개수가 pageSize보다 적을 수 있음
        return new org.springframework.data.domain.PageImpl<>(
            dtoList,
            pageable,
            reservationPage.getTotalElements()
        );
    }

    /* 특정 호텔의 전체 이용 이력 개수 조회 */
    public Long countTotalHistoryByContentId(String contentid) {
        return roomReservationRepository.countTotalHistoryByContentId(contentid);
    }

    /**
     * 체크아웃 시간이 지난 예약확정(status=1) 예약을 이용완료(status=4)로 변경
     * 체크아웃 날짜의 15시부터 24시간이 지난 예약만 처리
     * 
     * 처리 조건:
     * - 체크아웃 날짜 < 어제 (체크아웃 날짜 + 1일 < 현재 날짜) → 항상 처리
     * - 체크아웃 날짜 = 어제 (체크아웃 날짜 + 1일 = 현재 날짜) → 오늘 15시에 처리
     * 
     * 스케줄러가 매일 15시에 실행되므로, 체크아웃 날짜가 어제 이하인 모든 예약을 처리
     * 
     * 참고: 체크인 날짜나 숙박 기간(박수)과 무관하게 체크아웃 날짜만 기준으로 처리됩니다.
     * 
     * 
     * @return 변경된 예약의 개수
     */
    @Transactional
    public int completeExpiredReservations() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate yesterday = today.minusDays(1);
        
        // 체크아웃 날짜가 어제 이하인 모든 예약 처리
        // 체크아웃 날짜 + 1일의 15시가 이미 지났으므로 처리 가능
        // (1박, 연박, 장기 체류 모두 체크아웃 날짜 기준으로 동일하게 처리됨)
        return roomReservationRepository.updateExpiredReservationsToCompleted(yesterday);
    }
}
