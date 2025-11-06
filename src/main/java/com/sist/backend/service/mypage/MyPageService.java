package com.sist.backend.service.mypage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sist.backend.dto.mypage.ReservationResponseDTO;
import com.sist.backend.dto.mypage.DiningReservationResponseDTO;
import com.sist.backend.dto.mypage.WritableReviewDTO;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.entity.DiningReservation;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.DiningReservationRepository;
import com.sist.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MyPageService {
    
    private final RoomReservationRepository roomReservationRepository;
    private final DiningReservationRepository diningReservationRepository;
    private final ReviewRepository reviewRepository;
    
    // 날짜 포맷터 (YYYY.MM.DD)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private List<Integer> mapStatusToCodes(String status) {
        switch (status.toLowerCase()) {
            case "upcoming":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{1}); // List<Integer>로 변환됩니다.
    
            case "completed":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{4});
    
            case "cancelled":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{2, 3});
    
            default:
                return Arrays.asList(new Integer[]{0, 1, 2, 3, 4});
        }
    }

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회 (DTO 반환) - 페이지네이션 미지원 버전 (하위 호환성 유지) */
    public List<ReservationResponseDTO> getMyReservationsByStatus(Integer customerIdx, String status) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. Repository에서 데이터 조회 (Hotel, Room 정보 포함)
        List<RoomReservation> reservations = roomReservationRepository
            .findByReservationsByCustomerAndStatus(customerIdx, statusCodes);

        // 3. Entity → DTO 변환
        return reservations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회 (DTO 반환) - 페이지네이션 지원 버전
     * @param customerIdx 고객 ID (로그인 사용자)
     * @param status 프론트엔드 상태 문자열 (upcoming, completed, cancelled)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return Page<ReservationResponseDTO>
     */
    public Page<ReservationResponseDTO> getMyReservationsByStatus(Integer customerIdx, String status, int page, int size) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 3. Repository에서 페이지네이션된 데이터 조회 (Hotel, Room 정보 포함)
        Page<RoomReservation> reservationsPage = roomReservationRepository
            .findByReservationsByCustomerAndStatusWithPagination(customerIdx, statusCodes, pageable);

        // 4. Entity → DTO 변환 (Page 객체 유지)
        return reservationsPage.map(this::convertToDTO);
    }
    
    /**
     * RoomReservation Entity를 ReservationResponseDTO로 변환
     */
    private ReservationResponseDTO convertToDTO(RoomReservation reservation) {
        return ReservationResponseDTO.builder()
            // 기본 예약 정보
            .id(reservation.getReservIdx())
            .reservationNumber("R" + reservation.getReservIdx())
            
            // 호텔 정보 (Room -> HotelInfo)
            .hotelName(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null 
                ? reservation.getRoom().getHotelInfo().getTitle() 
                : "호텔명 없음")
            .location(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null 
                && reservation.getRoom().getHotelInfo().getArea() != null
                ? reservation.getRoom().getHotelInfo().getArea().getAreaName()
                : reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                    ? reservation.getRoom().getHotelInfo().getAdress()
                    : "위치 정보 없음")
            .contentId(reservation.getContentid())
            
            // 객실 정보
            .roomType(reservation.getRoom() != null 
                ? reservation.getRoom().getName() 
                : "객실 정보 없음")
            .roomIdx(reservation.getRoomIdx())
            
            // 예약 상세 정보
            .checkIn(reservation.getCheckinDate() != null 
                ? reservation.getCheckinDate().format(DATE_FORMATTER) 
                : "")
            .checkOut(reservation.getCheckoutDate() != null 
                ? reservation.getCheckoutDate().format(DATE_FORMATTER) 
                : "")
            .guest(reservation.getGuest())
            .totalprice(reservation.getTotalPrice())
            
            // 예약 상태
            .status(mapStatusCodeToString(reservation.getStatus()))
            .statusCode(reservation.getStatus())
            
            // 환불 정보 (취소 시 계산 - 실제로는 RoomPayment에서 가져와야 함)
            .refundAmount(reservation.getStatus() == 2 || reservation.getStatus() == 3 
                ? (int)(reservation.getTotalPrice() * 0.9) // 임시: 90% 환불
                : null)
            
            // 생성/수정 시간
            .createdAt(reservation.getCreatedAt() != null 
                ? reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                : "")
            .updatedAt(reservation.getUpdatedAt() != null 
                ? reservation.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                : "")
            .build();
    }
    
    /**
     * 예약 상세 정보 조회
     * @param reservationId 예약 ID
     * @param customerIdx 고객 ID (권한 검증용)
     * @return ReservationResponseDTO
     */
    public ReservationResponseDTO getReservationDetail(Integer reservationId, Integer customerIdx) {
        // 1. 예약 정보 조회 (권한 검증 포함)
        RoomReservation reservation = roomReservationRepository.findById(reservationId)
            .orElse(null);
        
        // 2. 예약이 없거나 다른 고객의 예약인 경우 null 반환
        if (reservation == null || !reservation.getCustomerIdx().equals(customerIdx)) {
            return null;
        }
        
        // 3. Entity → DTO 변환
        return convertToDTO(reservation);
    }

    /**
     * 다이닝 예약 상세 정보 조회
     * @param reservationId 다이닝 예약 ID (diningResrIdx)
     * @param customerIdx 고객 ID (권한 검증용)
     * @return DiningReservationResponseDTO
     */
    public DiningReservationResponseDTO getDiningReservationDetail(Integer reservationId, Integer customerIdx) {
        // 1. 예약 정보 조회 (권한 검증 포함)
        DiningReservation reservation = diningReservationRepository.findById(reservationId)
            .orElse(null);
        
        // 2. 예약이 없거나 다른 고객의 예약인 경우 null 반환
        if (reservation == null || !reservation.getCustomerIdx().equals(customerIdx)) {
            return null;
        }
        
        // 3. Entity → DTO 변환
        return convertDiningToDTO(reservation);
    }
    
    /**
     * 상태 코드를 한글 문자열로 변환
     */
    private String mapStatusCodeToString(Integer statusCode) {
        if (statusCode == null) return "알 수 없음";
        
        switch (statusCode) {
            case 0: return "예약대기";
            case 1: return "예약확정";
            case 2: return "취소완료";
            case 3: return "노쇼";
            case 4: return "이용완료";
            default: return "알 수 없음";
        }
    }

    /**
     * 작성 가능한 리뷰 목록 조회
     * - status = 4 (이용완료)인 예약만 조회
     * - 이미 리뷰를 작성한 예약은 제외
     */
    public List<WritableReviewDTO> getWritableReviews(Integer customerIdx) {
        // 1. 이용완료된 예약 조회 (status = 4)
        List<RoomReservation> completedReservations = 
            roomReservationRepository.findByCustomerIdxAndStatus(customerIdx, 4);
        
        // 2. 이미 리뷰를 작성한 예약 ID 목록 조회
        List<Integer> reviewedReservationIds = 
            reviewRepository.findReservationIdsByCustomerIdx(customerIdx);
        
        // 3. 리뷰를 작성하지 않은 예약만 필터링
        List<RoomReservation> writableReservations = completedReservations.stream()
            .filter(r -> !reviewedReservationIds.contains(r.getReservIdx()))
            .collect(Collectors.toList());
        
        // 4. DTO 변환
        return writableReservations.stream()
            .map(this::convertToWritableReviewDTO)
            .collect(Collectors.toList());
    }

    /**
     * RoomReservation을 WritableReviewDTO로 변환
     */
    private WritableReviewDTO convertToWritableReviewDTO(RoomReservation reservation) {
        // 체크아웃 날짜로부터 남은 일수 계산
        LocalDate checkOut = reservation.getCheckoutDate();
        long daysLeft = checkOut != null 
            ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), checkOut)
            : 0;

        return WritableReviewDTO.builder()
            .reservationIdx(reservation.getReservIdx())
            .hotelName(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                ? reservation.getRoom().getHotelInfo().getTitle()
                : "호텔명 없음")
            .location(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                && reservation.getRoom().getHotelInfo().getArea() != null
                ? reservation.getRoom().getHotelInfo().getArea().getAreaName()
                : reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                    ? reservation.getRoom().getHotelInfo().getAdress()
                    : "위치 정보 없음")
            .contentId(reservation.getContentid())
            .roomIdx(reservation.getRoomIdx())
            .roomType(reservation.getRoom() != null
                ? reservation.getRoom().getName()
                : "객실 정보 없음")
            .checkOutDate(checkOut != null ? checkOut.format(DATE_FORMATTER) : "")
            .daysLeft(daysLeft)
            .build();
    }

    /* 다이닝 예약 내역 조회 (페이지네이션 지원) */
    public Page<DiningReservationResponseDTO> getMyDiningReservationsByStatus(Integer customerIdx, String status, int page, int size) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 3. Repository에서 페이지네이션된 데이터 조회 (Dining 정보 포함)
        Page<DiningReservation> reservationsPage = diningReservationRepository
            .findByCustomerIdxAndStatusWithPagination(customerIdx, statusCodes, pageable);

        // 4. Entity → DTO 변환 (Page 객체 유지)
        return reservationsPage.map(this::convertDiningToDTO);
    }

    /**
     * DiningReservation Entity를 DiningReservationResponseDTO로 변환
     */
    private DiningReservationResponseDTO convertDiningToDTO(DiningReservation reservation) {
        return DiningReservationResponseDTO.builder()
            // 기본 예약 정보
            .id(reservation.getDiningResrIdx())
            .reservationNumber("D" + reservation.getDiningResrIdx())
            
            // 다이닝 정보 (Dining -> HotelInfo)
            .diningName(reservation.getDining() != null 
                ? reservation.getDining().getName() 
                : "다이닝 정보 없음")
            .hotelName(reservation.getDining() != null 
                && reservation.getDining().getHotelInfo() != null 
                ? reservation.getDining().getHotelInfo().getTitle() 
                : "호텔명 없음")
            .location(reservation.getDining() != null 
                && reservation.getDining().getHotelInfo() != null 
                && reservation.getDining().getHotelInfo().getArea() != null
                ? reservation.getDining().getHotelInfo().getArea().getAreaName()
                : reservation.getDining() != null 
                    && reservation.getDining().getHotelInfo() != null
                    ? reservation.getDining().getHotelInfo().getAdress()
                    : "위치 정보 없음")
            .contentId(reservation.getDining() != null 
                ? reservation.getDining().getContentid() 
                : null)
            .diningIdx(reservation.getDiningIdx())
            
            // 예약 상세 정보
            .reservationDate(reservation.getReservationDate() != null 
                ? reservation.getReservationDate().format(DATE_FORMATTER) 
                : "")
            .reservationTime(reservation.getReservationTime() != null 
                ? reservation.getReservationTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) 
                : "")
            .guest(reservation.getGuest())
            .totalPrice(reservation.getTotalPrice())
            
            // 예약 상태
            .status(mapStatusCodeToString(reservation.getStatus()))
            .statusCode(reservation.getStatus())
            
            // 환불 정보 (취소 시 - 실제로는 DiningPayment에서 가져와야 함)
            .refundAmount(reservation.getStatus() == 2 || reservation.getStatus() == 3 
                ? (int)(reservation.getTotalPrice() * 0.9) // 임시: 90% 환불
                : null)
            
            // 생성/수정 시간
            .createdAt(reservation.getCreatedAt() != null 
                ? reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                : "")
            .updatedAt(reservation.getUpdatedAt() != null 
                ? reservation.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                : "")
            
            // 예약 타입 구분
            .type("dining")
            .build();
    }
}
