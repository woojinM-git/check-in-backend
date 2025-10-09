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
@Table(name = "couponPolicy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponPolicy {
    
    @Id
    @Column(name = "idx")
    private Integer idx;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "expireStandard")
    private LocalDateTime expireStandard;
    
    @Column(name = "count")
    private Integer count;
    
    @Column(name = "minPrice")
    private Integer minPrice;
    
    @Column(name = "maxPrice")
    private Integer maxPrice;
    
    @Column(name = "couponName", length = 100)
    private String couponName;
    
    @Column(name = "couponType")
    private Integer couponType;
    
    // 양방향 관계
    @OneToMany(mappedBy = "couponPolicy", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Coupon> coupons = new ArrayList<>();
}
