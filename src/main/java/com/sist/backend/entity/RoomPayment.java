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
@Table(name = "roomPayment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPayment {
    
    @Id
    @Column(name = "number")
    private Integer number;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerId", nullable = false, length = 20)
    private String customerId;
    
    @Column(name = "couponIdx", nullable = false)
    private Integer couponIdx;
    
    @Column(name = "type")
    private String type;
    
    @Column(name = "price")
    private Integer price;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "Field")
    private String field;
    
    // 양방향 관계
    @OneToMany(mappedBy = "roomPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomReservation> roomReservations = new ArrayList<>();
    
    @OneToMany(mappedBy = "roomPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();
}
