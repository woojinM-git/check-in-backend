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
@Table(name = "diningPayment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiningPayment {
    
    @Id
    @Column(name = "diningpayIdx")
    private Integer diningpayIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diningIdx", referencedColumnName = "diningIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Dining dining;
    
    @Column(name = "diningIdx", nullable = false)
    private Integer diningIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;
    
    @Column(name = "method")
    private String method;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "status")
    private Integer status;
    
    // 양방향 관계
    @OneToMany(mappedBy = "diningPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<DiningReservation> diningReservations = new ArrayList<>();
}
