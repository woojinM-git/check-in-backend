package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "diningResrevation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diningResrIdx")
    private Integer diningResrIdx;
    
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diningpayIdx", referencedColumnName = "diningpayIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private DiningPayment diningPayment;
    
    @Column(name = "diningpayIdx")
    private Integer diningpayIdx;
    
    // 새로운 필드들
    @Column(name = "reservationDate")
    private LocalDate reservationDate; // 예약 날짜
    
    @Column(name = "reservationTime")
    private LocalTime reservationTime; // 예약 시간
    
    @Column(name = "guest", nullable = false)
    private Integer guest; // 인원 수
    
    @Column(name = "totalPrice", nullable = false)
    private Integer totalPrice; // 총 가격
    
    @Column(name = "status")
    private Integer status; // 0:대기, 1:확정, 2:취소, 3:노쇼, 4:완료
    
    @Column(name = "qrUrl", length = 500)
    private String qrUrl; // QR코드 URL
    
    @Column(name = "specialRequest", columnDefinition = "TEXT")
    private String specialRequest; // 특별 요청사항
    
    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;
    
    
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
