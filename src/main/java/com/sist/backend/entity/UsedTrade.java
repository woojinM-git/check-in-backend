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
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer buyer;
    
    @Column(name = "buyerId", nullable = false, length = 20)
    private String buyerId;
    
    @Column(name = "sellerId", length = 20)
    private String sellerId;
    
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
