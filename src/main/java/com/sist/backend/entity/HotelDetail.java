package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "hotelDetail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelDetail {
    
    @Id
    @Column(name = "contentid", length = 50)
    private String contentid;
    
    @OneToOne(mappedBy = "hotelDetail", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "roomcount", length = 50)
    private String roomcount;
    
    @Column(name = "foodplace", length = 500)
    private String foodplace;
    
    @Column(name = "parkinglodging", length = 500)
    private String parkinglodging;
    
    @Column(name = "reservationlodging")
    private String reservationlodging;
    
    @Column(name = "scalelodging", length = 500)
    private String scalelodging;
}
