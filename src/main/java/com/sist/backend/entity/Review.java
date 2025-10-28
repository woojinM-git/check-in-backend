package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewIdx")
    private Integer reviewIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderIdx", referencedColumnName = "orderIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomPayment roomPayment;
    
    @Column(name = "orderIdx", nullable = false)
    private Integer orderIdx;
    
    @Column(name = "content", length = 500)
    private String content;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "hide")
    private Boolean hide;
    
    @Column(name = "star", precision = 2, scale = 1)
    private BigDecimal star;
    
    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}

