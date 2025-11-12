package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "recentViewedHotel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentViewedHotel {

    @Id
    @Column(name = "recentViewedIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recentViewedIdx;

    @Column(name = "customerIdx", nullable = false)
    private Integer customerIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;

    @Column(name = "contentId", nullable = false, length = 50)
    private String contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;

    @Column(name = "viewedAt", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (viewedAt == null) {
            viewedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

