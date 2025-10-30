package com.sist.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservationTime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationTime {

    @Id
    @Column(name = "orderIdx")
    private Integer orderIdx;

    @Column(name = "inTime")
    private LocalDateTime inTime;

    @Column(name = "outTime")
    private LocalDateTime outTime;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderIdx", referencedColumnName = "orderIdx", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    private RoomPayment roomPayment;
}

