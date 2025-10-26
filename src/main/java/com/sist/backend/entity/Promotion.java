package com.sist.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    
    @Id
    @Column(name = "promotionIdx")
    private Integer promotionIdx;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "couponCount")
    private Integer couponCount;
}

