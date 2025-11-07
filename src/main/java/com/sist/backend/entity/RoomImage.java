package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "roomImage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomImageIdx")
    private Integer roomImageIdx;
    
    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;
    
    @Column(name = "contentId", length = 50, nullable = false)
    private String contentId;
    
    @Column(name = "imageUrl", length = 500, nullable = false)
    private String imageUrl;
    
    @Column(name = "imageOrder", nullable = false)
    private Integer imageOrder = 1; // 1-10 사이 값
    
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 0: 삭제됨, 1: 활성
    
    @Column(name = "deletedAt")
    private LocalDateTime deletedAt;
    
    // Room 엔티티와의 관계 (필요시 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "roomIdx", referencedColumnName = "roomIdx", insertable = false, updatable = false),
        @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    })
    @JsonIgnore
    @ToString.Exclude
    private Room room;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

