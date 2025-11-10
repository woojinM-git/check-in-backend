package com.sist.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 예약 내역 조회 응답 DTO 프론트엔드에서 필요로 하는 형식으로 데이터를 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponseDTO {

    // 기본 예약 정보
    private Integer id;                    // reservIdx
    private String reservationNumber;      // 예약번호 (표시용)

    // 호텔 정보 (Hotel 테이블에서 JOIN)
    private String hotelName;              // 호텔명
    private String location;               // 위치 (시/도)
    private String contentId;              // 호텔 ID (상세 페이지 이동용)

    // 객실 정보 (Room 테이블에서 JOIN)
    private String roomType;               // 객실 타입
    private Integer roomIdx;               // 객실 ID

    // 예약 상세 정보
    private String checkIn;                // 체크인 날짜 (YYYY.MM.DD 형식)
    private String checkOut;               // 체크아웃 날짜 (YYYY.MM.DD 형식)
    private Integer guest;                 // 투숙 인원
    private Integer totalprice;            // 총 결제금액
    private Integer cashUsed;              // 사용된 캐시 금액
    private Integer pointsUsed;            // 사용된 포인트 금액

    // 예약 상태
    private String status;                 // 예약 상태 ("예약확정", "이용완료", "취소완료" 등)
    private Integer statusCode;            // 상태 코드 (원본)

    // 환불 정보 (취소 시)
    private Integer refundAmount;          // 환불 금액
    private Integer refundCash;            // 환불된 캐시 금액
    private Integer refundPoint;           // 환불된 포인트 금액

    // 생성/수정 시간
    private String createdAt;              // 예약 생성일
    private String updatedAt;              // 최종 수정일
}
