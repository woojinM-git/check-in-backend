package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usedTrade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedTrade {
    
    @Id
    @Column(name = "usedTradeIdx")
    private Integer usedTradeIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userItemIdx", referencedColumnName = "usedItemIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private UsedItem usedItem;
    
    @Column(name = "userItemIdx", nullable = false)
    private Integer userItemIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer buyer;
    
    @Column(name = "buyerIdx", nullable = false, length = 20)
    private Integer buyerIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer seller;
    
    @Column(name = "sellerIdx", length = 20)
    private Integer sellerIdx;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "ststus")
    private Integer ststus;
    
    // 양방향 관계
    @OneToMany(mappedBy = "usedTrade", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<UsedPay> usedPays = new ArrayList<>();
}
