package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "center")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Center {
    
    @Id
    @Column(name = "centerIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer centerIdx;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "mainCategory", nullable = false, length = 50)
    private String mainCategory = "문의"; // 메인 카테고리 (기본값: 문의)
    
    @Column(name = "priority")
    private Integer priority = 0; // 0: 일반, 1: 높음, 2: 긴급
    
    @Column(name = "subCategory", length = 50)
    private String subCategory; // 세부 카테고리 (예약/취소, 회원정보, 기술지원, 결제, 호텔정보 등)
    
    // 관리자 번호 (NULL 허용)
    @Column(name = "adminIdx")
    private Integer adminIdx;
    
    // 고객 번호 (NULL 허용)
    @Column(name = "customerIdx")
    private Integer customerIdx;
    
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "status")
    private Integer status = 0; // 0: 대기, 1: 처리중, 2: 완료
    
    @Column(name = "hide")
    private Boolean hide = false;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    // 양방향 관계 - 관리자 (NULL 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adminIdx", referencedColumnName = "adminIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Admin admin;
    
    // 양방향 관계 - 고객 (NULL 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    
    // 기존 Answer 관계 유지
    @OneToMany(mappedBy = "center", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Answer> answers = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 작성자 타입 확인 메서드
    public String getWriterType() {
        if (adminIdx != null) {
            return "ADMIN";
        } else if (customerIdx != null) {
            return "CUSTOMER";
        }
        return "UNKNOWN";
    }
    
    // 작성자명 가져오기 메서드
    public String getWriterName() {
        if (adminIdx != null && admin != null) {
            return admin.getName(); // Admin 엔티티의 name 필드
        } else if (customerIdx != null && customer != null) {
            return customer.getNickname(); // Customer 엔티티의 nickname 필드
        }
        return "알 수 없음";
    }
    
}
