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
    
    @OneToMany(mappedBy = "rankEntity", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Customer> customers = new ArrayList<>();
}

