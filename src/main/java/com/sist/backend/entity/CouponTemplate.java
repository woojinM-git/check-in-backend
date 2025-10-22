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
@Table(name = "couponTemplate")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplate {
    
    @Id
    @Column(name = "templateIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer templateIdx;
    
    @Column(name = "templateName", nullable = false, length = 100)
    private String templateName;
    
    @Column(name = "discount", nullable = false)
    private Integer discount;
    
    @Column(name = "validDays")
    private Integer validDays;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "adminIdx", nullable = false)
    private Integer adminIdx;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    // 양방향 관계
    @OneToMany(mappedBy = "couponTemplate", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Coupon> coupons = new ArrayList<>();
}
