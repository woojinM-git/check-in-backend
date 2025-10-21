package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RoomId.class)
public class Room {
    
    @Id
    @Column(name = "roomIdx")
    private Integer roomIdx;
    
    @Id
    @Column(name = "contentId", length = 50)
    private String contentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "name", length = 120)
    private String name;
    
    @Column(name = "capacity")
    private Integer capacity;
    
    @Column(name = "basePrice")
    private Integer basePrice;
    
    @Column(name = "refundable")
    private Boolean refundable;
    
    @Column(name = "breakfastIncluded")
    private Boolean breakfastIncluded;
    
    @Column(name = "smoking")
    private Boolean smoking;
    
    @Column(name = "imageUrl", length = 500)
    private String imageUrl;

    @Column(name = "roomCount")
    private Integer roomCount;
    // 양방향 관계
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomBookMark> roomBookMarks = new ArrayList<>();
    
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomReservation> roomReservations = new ArrayList<>();
}
