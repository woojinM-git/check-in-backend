package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "reviewAnswer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewAnswerIdx")
    private Integer reviewAnswerIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewIdx", referencedColumnName = "reviewIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Review review;
    
    @Column(name = "reviewIdx", nullable = false)
    private Integer reviewIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adminIdx", referencedColumnName = "adminIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Admin admin;
    
    @Column(name = "adminIdx", nullable = false)
    private Integer adminIdx;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "status", nullable = false)
    private Integer status;
    
    @Column(name = "createdAt", nullable = false)
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updatedAt", nullable = false)
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
        if (status == null) {
            status = 1; // 기본값 1 (활성)
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}

