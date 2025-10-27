package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrationRequest")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registrationIdx")
    private Integer registrationIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adminIdx", referencedColumnName = "adminIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Admin admin;
    
    @Column(name = "adminIdx", nullable = false)
    private Integer adminIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draftIdx", referencedColumnName = "draftIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private HotelDraft hotelDraft;
    
    @Column(name = "draftIdx")
    private Integer draftIdx;
    
    @Column(name = "regiDate")
    private LocalDateTime regiDate;
    
    @Column(name = "status", columnDefinition = "TINYINT(1)")
    private Integer status;
    
    @Column(name = "approvDate")
    private LocalDateTime approvDate;
}
