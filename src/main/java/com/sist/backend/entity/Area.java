package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "area")
public class Area {

    @Id
    @Column(name = "areaCode")
    private String areaCode;

    @Column(name = "areaName")
    private String areaName;
    
    // 양방향 관계
    @OneToMany(mappedBy = "area", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelInfo> hotelInfos = new ArrayList<>();
}
