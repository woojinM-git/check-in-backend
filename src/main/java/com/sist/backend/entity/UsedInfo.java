package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "usedInfo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedInfo {
    
    @Id
    @Column(name = "usedInfoIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usedInfoIdx;
    
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @Column(name = "price", nullable = false)
    private Integer price;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
