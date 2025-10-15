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
@Table(name = "`rank`")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rank {
    
    @Id
    @Column(name = "`rank`", length = 50)
    private String rank;

    @Column(name = "pointRate")
    private Integer pointRate;
    
    @Column(name = "yearCoupon")
    private Integer yearCoupon;
    
    @Column(name = "maxDiscount")
    private Integer maxDiscount;
    
    @Column(name = "conditions")
    private Integer conditions;
    
    @OneToMany(mappedBy = "rankEntity", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Customer> customers = new ArrayList<>();
}

