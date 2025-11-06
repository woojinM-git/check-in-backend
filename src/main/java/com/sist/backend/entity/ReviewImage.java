package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "reviewImage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewImageIdx")
    private Integer reviewImageIdx;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewIdx", referencedColumnName = "reviewIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Review review;
    
    @Column(name = "reviewIdx", nullable = false, unique = true)
    private Integer reviewIdx;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @Column(name = "imageUrl2", length = 500)
    private String imageUrl2;
    
    @Column(name = "imageUrl3", length = 500)
    private String imageUrl3;
    
    @Column(name = "imageUrl4", length = 500)
    private String imageUrl4;
    
    @Column(name = "imageUrl5", length = 500)
    private String imageUrl5;
}

