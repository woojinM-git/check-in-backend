package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "diningResrevation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiningReservation {
    
    @Id
    @Column(name = "idx")
    private String idx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diningIdx", referencedColumnName = "diningIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Dining dining;
    
    @Column(name = "diningIdx", nullable = false)
    private Integer diningIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerId", nullable = false, length = 20)
    private String customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diningpayIdx", referencedColumnName = "diningpayIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private DiningPayment diningPayment;
    
    @Column(name = "diningpayIdx", nullable = false)
    private Integer diningpayIdx;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "checkIn")
    private String checkIn;
}
