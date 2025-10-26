package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "roomBookMark")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomBookMark {
    
    @Id
    @Column(name = "roomBookIdx")
    private Integer roomBookIdx;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;
    
    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;
    
    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;
    
    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "roomIdx", referencedColumnName = "roomIdx", insertable = false, updatable = false),
        @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    })
    @JsonIgnore
    @ToString.Exclude
    private Room room;
}
