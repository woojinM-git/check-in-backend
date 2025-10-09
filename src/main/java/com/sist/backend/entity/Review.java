package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    
    @Id
    @Column(name = "reviewIdx")
    private Integer reviewIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerId", nullable = false, length = 20)
    private String customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "number", referencedColumnName = "number", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomPayment roomPayment;
    
    @Column(name = "number", nullable = false)
    private Integer number;
    
    @Column(name = "content", length = 500)
    private String content;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "hide")
    private Boolean hide;
    
    @Column(name = "star", precision = 2, scale = 1)
    private BigDecimal star;
}

