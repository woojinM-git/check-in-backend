package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "hotelLocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelLocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentId", length = 50)
    private String contentId;
    
    @Column(name = "mapX", precision = 20, scale = 10)
    private BigDecimal mapX;
    
    @Column(name = "mapY", precision = 20, scale = 10)
    private BigDecimal mapY;
}
