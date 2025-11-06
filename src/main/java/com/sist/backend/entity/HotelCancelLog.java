package com.sist.backend.entity;

import jakarta.persistence.*;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 호텔 예약 취소 로그 엔티티
 * Toss 환불 상태, 취소 사유, 처리자 등을 관리
 */
@Entity
@Table(name = "hotelCancelLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelCancelLog {

    /** 취소 로그 고유 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cancelId;

    /** roomReservation 테이블의 예약 PK */
    @Column(nullable = false)
    private Integer reservIdx;

    /** 취소 사유 */
    @Column(length = 1000)
    private String cancelReason;

    /** 환불 총 금액 (수수료 제외, 쿠폰 환불 제외) */
    private Integer refundTotalAmount;

    /** 환불된 캐시 금액 */
    private Integer refundCash;

    /** 환불된 포인트 금액 */
    private Integer refundPoint;

    /**
     * 환불 상태
     * 0: 환불 진행중
     * 1: 환불 완료
     * 2: 환불 실패
     * 3: 관리자 거절
     */
    @Column(nullable = false)
    private Integer refundStatus;

    /** 취소 시각 */
    private LocalDateTime cancelAt;

    /** 취소자 (USER, ADMIN, SYSTEM) */
    @Column(length = 20, nullable = false)
    private String canceledBy;

    //이력테이블이니 FK생략
}