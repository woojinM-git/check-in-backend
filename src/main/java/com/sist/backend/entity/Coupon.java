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
@IdClass(CouponId.class)
public class Coupon {
    
    @Id
    @Column(name = "couponIdx")
    private Integer couponIdx;
    
    @Id
    @Column(name = "idx")
    private Integer idx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idx", referencedColumnName = "idx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private CouponPolicy couponPolicy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;

    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;
    
    @Column(name = "createDate")
    private LocalDateTime createDate;
    
    @Column(name = "endDate")
    private LocalDateTime endDate;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "count")
    private Integer count;
}
