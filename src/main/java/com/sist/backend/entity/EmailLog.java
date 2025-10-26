package com.sist.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emailLog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emailLogIdx")
    private Long emailLogIdx;  // BIGINT → Long으로 수정

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerIdx", referencedColumnName = "customerIdx", insertable = false, updatable = false)
    @ToString.Exclude
    private Customer customer;

    @Column(name = "customerIdx", nullable = false, length = 20)
    private Integer customerIdx;  // FK (customer.id)

    @Column(name = "template", length = 100)
    private String template;  // 이메일 템플릿명

    @Column(name = "subject", length = 200, nullable = false)
    private String subject;  // 제목

    @Column(name = "toEmail", length = 255, nullable = false)
    private String toEmail;  // 수신 이메일 주소

    @Column(name = "payloadJson", columnDefinition = "TEXT")
    private String payloadJson;  // 이메일 본문 JSON 문자열

    @Column(name = "status")
    private Boolean status;  // false=실패, true=성공

    @Column(name = "sentAt")
    private LocalDateTime sentAt;  // 발송 시각
}
