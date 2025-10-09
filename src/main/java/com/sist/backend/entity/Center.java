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
@Table(name = "center")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Center {
    
    @Id
    @Column(name = "centerIdx")
    private Integer centerIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adminIdx", referencedColumnName = "adminIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Admin admin;
    
    @Column(name = "adminIdx", nullable = false)
    private Integer adminIdx;
    
    @Column(name = "type", length = 50)
    private String type;
    
    @Column(name = "content")
    private String content;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "hide")
    private Boolean hide;
    
    // 양방향 관계
    @OneToMany(mappedBy = "center", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Answer> answers = new ArrayList<>();
}
