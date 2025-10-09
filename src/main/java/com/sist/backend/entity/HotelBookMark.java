package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "hotelBookMark")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelBookMark {
    
    @Id
    @Column(name = "hotelBookIdx")
    private Integer hotelBookIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerId", nullable = false, length = 20)
    private String customerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contentId", referencedColumnName = "contentId", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelInfo hotelInfo;
    
    @Column(name = "contentId", nullable = false, length = 50)
    private String contentId;
}
