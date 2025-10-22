package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    
    @Id
    @Column(name = "couponIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer couponIdx;
    
    @Column(name = "templateIdx", nullable = false)
    private Integer templateIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateIdx", referencedColumnName = "templateIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private CouponTemplate couponTemplate;
    
    @Column(name = "id", nullable = false)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "adminIdx", nullable = false)
    private Integer adminIdx;
    
    @Column(name = "createDate", nullable = false)
    private LocalDateTime createDate;
    
    @Column(name = "endDate", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "status")
    private Boolean status;
}
