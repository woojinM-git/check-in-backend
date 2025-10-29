package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roomPayment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderIdx")  // PK 변경함
    private Integer orderIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private Customer customer;

    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;

    @Column(name = "couponIdx", nullable = false)
    private Integer couponIdx;

    @Column(name = "price")
    private Integer price;

    @Column(name = "status")
    private Integer status;

    //새 컬럼들
    @Column(name = "promotionPayIdx")
    private Integer promotionPayIdx;

    @Column(name = "paymentKey")
    private String paymentKey;

    @Column(name = "pointsUsed")
    private Integer pointsUsed;

    @Column(name = "method")
    private String method;

    @Column(name = "receiptUrl")
    private String receiptUrl;

    @Column(name = "approvedAt")
    private LocalDateTime approvedAt;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;


    // 양방향 관계
    @OneToMany(mappedBy = "roomPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<RoomReservation> roomReservations = new ArrayList<>();

    @OneToMany(mappedBy = "roomPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<>();

    @OneToOne(mappedBy = "roomPayment", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private ReservationTime reservationTime;
}
