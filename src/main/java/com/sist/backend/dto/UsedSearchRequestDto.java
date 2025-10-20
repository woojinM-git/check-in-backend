package com.sist.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsedSearchRequestDto {
    private String destination;      // 목적지 (호텔 위치)
    private String checkIn;          // 체크인 날짜
    private String checkOut;         // 체크아웃 날짜
    private Integer adults;          // 성인 수
    private Integer priceMin;        // 최소 가격
    private Integer priceMax;        // 최대 가격
    private String sortBy;           // 정렬 기준 (date, price, discount)
    private String sortDirection;    // 정렬 방향 (asc, desc)
    private Integer status;          // 상태 (0: 판매중, 1: 예약됨, 2: 완료)
    private int page = 0;
    private int size = 10;
}
