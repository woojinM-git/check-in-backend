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
@Table(name = "paytopromotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayToPromotion {
    
    @Id
    @Column(name = "promotionPayIdx")
    private Integer promotionPayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @Column(name = "promotionIdx", nullable = false)
    private Integer promotionIdx;
    
    @Column(name = "promotionPayDate")
    private LocalDateTime promotionPayDate;
    
    @Column(name = "couponCount")
    private Integer couponCount;
    
    @Column(name = "createDate")
    private LocalDateTime createDate;
    
    @Column(name = "endDate")
    private LocalDateTime endDate;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "status")
    private Boolean status;
    
    // 양방향 관계
    @OneToMany(mappedBy = "payToPromotion", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<PromDuplication> promDuplications = new ArrayList<>();
}
