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
@Table(name = "roomReservation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomReservation {
    
    @Id
    @Column(name = "reservIdx")
    private Integer reservIdx;
    
    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "roomIdx", referencedColumnName = "roomIdx", insertable = false, updatable = false),
        @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    })
    @JsonIgnore
    @ToString.Exclude
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerId", nullable = false, length = 20)
    private String customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "number", referencedColumnName = "number", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomPayment roomPayment;
    
    @Column(name = "number", nullable = false)
    private Integer number;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "checkin")
    private Integer checkin;
    
    // 양방향 관계
    @OneToMany(mappedBy = "roomReservation", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "roomReservation", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<UsedTrade> usedTrades = new ArrayList<>();
}
