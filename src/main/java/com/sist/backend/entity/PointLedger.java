package com.sist.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pointLedger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // 생성 시 빌더 패턴 지원
public class PointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pointIdx")
    private Long pointIdx;  // Long으로 설정

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    @ToString.Exclude
    private Customer customer;  // FK 관계

    @Column(name = "id", nullable = false, length = 20)
    private String id;  // 고객 ID

    @Column(name = "memo", length = 255)
    private String memo;  // 비고 (예: "적립", "사용")

    @Column(name = "amount", nullable = false)
    private Integer amount;  // 변동 포인트

    @Column(name = "type", nullable = false, length = 30)
    private String type;  // 구분 (예: "EARN", "USE")

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;  // 생성 시각
}
