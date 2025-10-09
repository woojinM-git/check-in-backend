package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "usedPay")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedPay {
    
    @Id
    @Column(name = "usedPayIdx")
    private Integer usedPayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usedTradeIdx", referencedColumnName = "usedTradeIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private UsedTrade usedTrade;
    
    @Column(name = "usedTradeIdx", nullable = false)
    private Integer usedTradeIdx;
}
