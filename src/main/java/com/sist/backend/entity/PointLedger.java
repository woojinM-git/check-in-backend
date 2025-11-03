package com.sist.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pointLedger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "pointIdx")
@Builder  // 생성 시 빌더 패턴 지원
public class PointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pointIdx")
    private Long pointIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @ToString.Exclude
    private Customer customer;  // FK 관계

    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;  // 고객 ID

    @Column(name = "memo", length = 255)
    private String memo;  // 비고 (예: "적립", "사용")

    @Column(name = "point")
    private Integer point;  // 변동 포인트

    @Column(name = "pointType",  length = 30)
    private String pointType;  // 구분 (예: "EARN", "USE")


    /** 캐시 금액 */
    @Column(name = "cash")
    private Integer cash;

    /** 캐시 타입 (예: "CHARGE", "USE") */
    @Column(name = "cashType", length = 30)
    private String cashType;

    /** 결제 PK (roomPayment.orderIdx 참조) */
    @Column(name = "orderIdx")
    private Integer orderIdx;

    /** 결제 FK (roomPayment 참조) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderIdx", referencedColumnName = "orderIdx", insertable = false, updatable = false)
    @ToString.Exclude
    private RoomPayment roomPayment;

    @Column(name = "createdAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시각

    /** 기본값 처리 */
    @PrePersist
    public void prePersist() {
        if (this.cash == null) this.cash = 0;
        if (this.point == null) this.point = 0;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
