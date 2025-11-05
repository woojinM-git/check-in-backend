package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Answer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answerIdx")
    private Integer answerIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centerIdx", referencedColumnName = "centerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Center center;
    
    @Column(name = "centerIdx", nullable = false)
    private Integer centerIdx;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "content", length = 500)
    private String content;
}
