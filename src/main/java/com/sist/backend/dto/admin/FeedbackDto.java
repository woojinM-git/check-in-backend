package com.sist.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDto {
    // 리뷰 기본 정보
    private Integer reviewIdx;
    private Integer reservIdx;
    private Integer customerIdx;
    private String customerId;
    private String customerName;
    private String roomNumber;
    private String roomName;
    private BigDecimal star;
    private String content;
    private LocalDateTime createdAt;
    
    // 리뷰 답변 정보
    private Integer reviewAnswerIdx;
    private String response;
    private Integer responseStatus; // 0: 비활성, 1: 활성
    private LocalDateTime responseCreatedAt;
    private LocalDateTime responseUpdatedAt;
    
    // 상태 (UI에서 사용)
    // 'new': 답변 없음, 'in-progress': 답변 작성됨, 'resolved': 해결됨, 'urgent': 긴급
    private String status;
    
    // 카테고리 (UI에서 사용 - 추후 확장 가능)
    private String category;
}

