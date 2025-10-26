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
@Table(name = "category")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    
    @Id
    @Column(name = "hotelCategoryCode", length = 20)
    private String hotelCategoryCode;
    
    @Column(name = "categoryName", length = 100)
    private String categoryName;
    
    // 양방향 관계
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<HotelInfo> hotelInfos = new ArrayList<>();
}
