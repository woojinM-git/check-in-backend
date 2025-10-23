package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diningPayment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diningpayIdx")
    private Integer diningpayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diningIdx", referencedColumnName = "diningIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Dining dining;
    
    @Column(name = "diningIdx", nullable = false)
    private Integer diningIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerIdx", nullable = false)
    private Integer customerIdx;
    
    @Column(name = "couponIdx")
    private Integer couponIdx; // 사용한 쿠폰 ID
    
    @Column(name = "price")
    private Integer price; // 실제 결제 금액
    
    @Column(name = "status")
    private Integer status; // 0:대기, 1:완료, 2:취소, 3:환불
    
    @Column(name = "paymentKey", length = 50)
    private String paymentKey; // 토스페이먼츠 결제 키
    
    @Column(name = "pointUsed")
    private Integer pointUsed; // 사용한 포인트
    
    @Column(name = "method")
    private String method; // 결제 수단
    
    @Column(name = "receiptUrl", length = 500)
    private String receiptUrl; // 영수증 URL
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "approvedAt")
    private LocalDateTime approvedAt; // 승인 시간
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    // 양방향 관계
    @OneToMany(mappedBy = "diningPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<DiningReservation> diningReservations = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
