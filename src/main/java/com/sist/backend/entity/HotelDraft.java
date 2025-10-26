package com.sist.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotelDraft")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelDraft {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draftIdx")
    private Integer draftIdx;
    
    @Column(name = "adminIdx", nullable = false, unique = true)
    private Integer adminIdx;
    
    @Column(name = "formData", columnDefinition = "JSON")
    private String formData;
    
    @Column(name = "lastTab")
    private String lastTab;
    
    @Column(name = "progress")
    private Integer progress;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}
