package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roomReservation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservIdx")
    private Integer reservIdx;

    @Column(name = "roomIdx", nullable = false)
    private Integer roomIdx;

    @Column(name = "contentid", nullable = false, length = 50)
    private String contentid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "roomIdx", referencedColumnName = "roomIdx", insertable = false, updatable = false),
        @JoinColumn(name = "contentid", referencedColumnName = "contentId", insertable = false, updatable = false)
    })
    @ToString.Exclude
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @ToString.Exclude
    private Customer customer;

    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderIdx", referencedColumnName = "orderIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomPayment roomPayment;

    @Column(name = "orderIdx", nullable = false)
    private Integer orderIdx;

    @Column(name = "status")
    private Integer status; // 0: 대기, 1: 확정, 2: 취소, 3: 노쇼

    @Column(name = "checkin")
    private LocalDate checkinDate;

    @Column(name = "checkout")
    private LocalDate checkoutDate;

    @Column(name = "guest")
    private Integer guest;

    @Column(name = "totalPrice")
    private Integer totalPrice;

    // qrUrl로 수정 (DB 컬럼명과 일치)
    @Column(name = "qrUrl")
    private String qrUrl;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "orderNum", unique = true, length = 100)
    private String orderNum;

    @Column(name = "specialRequest")
    private String specialRequest;


    // 양방향 관계
    @OneToMany(mappedBy = "roomReservation", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "roomReservation", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<UsedTrade> usedTrades = new ArrayList<>();
}
