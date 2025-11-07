package com.sist.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotelSettlement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlementIdx")
    private Integer settlementIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    private HotelInfo hotelInfo;

    @Column(name = "contentId", nullable = false, length = 50)
    private String contentId;

    @Column(name = "settlementMonth", nullable = false, length = 7)
    private String settlementMonth; // 형식: "2024-01"

    @Column(name = "totalRevenue", nullable = false)
    private Long totalRevenue; // 월 수익금액 (원)

    @Column(name = "commissionAmount", nullable = false)
    private Long commissionAmount; // 사이트 수수료 금액 (원)

    @Column(name = "withholdingTaxAmount", nullable = false)
    private Long withholdingTaxAmount; // 원천징수 금액 (원)

    @Column(name = "finalAmount", nullable = false)
    private Long finalAmount; // 실 지급액 (수수료 및 원천징수 제외)
}

