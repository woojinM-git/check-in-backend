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
@Table(name = "dining")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dining {
    
    @Id
    @Column(name = "diningIdx")
    private Integer diningIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "amount")
    private Integer amount;
    
    // 양방향 관계
    @OneToMany(mappedBy = "dining", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<DiningPayment> diningPayments = new ArrayList<>();
    
    @OneToMany(mappedBy = "dining", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<DiningReservation> diningReservations = new ArrayList<>();
}
