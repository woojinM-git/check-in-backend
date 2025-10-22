package com.sist.backend.service.mypage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.mypage.ReservationResponseDTO;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageService {
    
    private final RoomReservationRepository roomReservationRepository;
    
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

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회 (DTO 반환)
     * @param customerIdx 고객 ID (로그인 사용자)
     * @param status 프론트엔드 상태 문자열 (upcoming, completed, cancelled)
     * @return ReservationResponseDTO 목록
     */
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
}
