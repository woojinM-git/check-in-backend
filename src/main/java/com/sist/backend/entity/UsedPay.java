package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "usedPay")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedPay {
    
    @Id
    @Column(name = "usedPayIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usedPayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usedTradeIdx", referencedColumnName = "usedTradeIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private UsedTrade usedTrade;
    
    @Column(name = "usedTradeIdx", nullable = false)
    private Integer usedTradeIdx;
    
    // 결제 정보
    @Column(name = "paymentKey", length = 100)
    private String paymentKey; // 토스페이먼츠 결제 키
    
    @Column(name = "orderId", length = 100)
    private String orderId; // 주문 ID
    
    @Column(name = "totalAmount")
    private Integer totalAmount; // 총 결제 금액
    
    @Column(name = "cashAmount")
    private Integer cashAmount; // 캐시 사용 금액
    
    @Column(name = "pointAmount")
    private Integer pointAmount; // 포인트 사용 금액
    
    @Column(name = "cardAmount")
    private Integer cardAmount; // 카드 결제 금액
    
    @Column(name = "paymentMethod", length = 20)
    private String paymentMethod; // 결제 방식 (card, cash, point, mixed)
    
    @Column(name = "status")
    private Integer status; // 결제 상태 (0: 대기, 1: 완료, 2: 취소)
    
    @Column(name = "receiptUrl", length = 500)
    private String receiptUrl; // 영수증 URL
    
    @Column(name = "qrUrl", length = 500)
    private String qrUrl; // QR 코드 URL
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    @Column(name = "approvedAt")
    private LocalDateTime approvedAt; // 결제 승인 시간
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
