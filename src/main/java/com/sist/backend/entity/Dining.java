package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dining")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dining {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diningIdx")
    private Integer diningIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    // 새로운 필드들
    @Column(name = "name", length = 100)
    private String name; // 다이닝 이름
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 설명
    
    @Column(name = "imageUrl", length = 500)
    private String imageUrl; // 이미지 URL
    
    @Column(name = "totalSeats")
    private Integer totalSeats; // 총 좌석 수
    
    @Column(name = "basePrice")
    private Integer basePrice; // 1인당 기본 가격
    
    @Column(name = "openTime")
    private LocalTime openTime; // 오픈 시간
    
    @Column(name = "closeTime")
    private LocalTime closeTime; // 마감 시간
    
    @Column(name = "slotDuration")
    private Integer slotDuration; // 예약 시간 단위 (분)
    
    @Column(name = "maxGuestsPerSlot")
    private Integer maxGuestsPerSlot; // 시간대별 최대 인원
    
    @Column(name = "status")
    private Integer status; // 0: 비활성, 1: 활성
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 상세 정보
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    
    // 양방향 관계
    @OneToMany(mappedBy = "dining", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<DiningPayment> diningPayments = new ArrayList<>();
    
    @OneToMany(mappedBy = "dining", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<DiningReservation> diningReservations = new ArrayList<>();
    
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
