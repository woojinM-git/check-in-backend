package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usedItem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedItem {
    
    @Id
    @Column(name = "usedItemIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer usedItemIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservIdx", referencedColumnName = "reservIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomReservation roomReservation;
    @Column(name = "reservIdx", nullable = false)
    private Integer reservIdx;
    
    @Column(name = "price", nullable = false)
    private Integer price;
    
    @Column(name = "status")
    private Integer status;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    @Column(name = "comment", length = 255)
    private String comment;
    
    // 양방향 관계
    @OneToMany(mappedBy = "usedItem", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<UsedTrade> usedTrades = new ArrayList<>();
    
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
