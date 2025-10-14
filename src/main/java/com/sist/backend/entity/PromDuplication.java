package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "promDuplication")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromDuplication {
    
    @Id
    @Column(name = "promotionPayIdx")
    private Integer promotionPayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotionPayIdx", referencedColumnName = "promotionPayIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private PayToPromotion payToPromotion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;
}
