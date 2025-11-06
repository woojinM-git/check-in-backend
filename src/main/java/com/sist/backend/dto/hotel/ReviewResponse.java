package com.sist.backend.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Integer id; // reviewIdx
    private String userName; // Customer의 name 또는 nickname
    private String date; // createdAt을 날짜 문자열로
    private String roomType; // Room의 name
    private Double rating; // star를 0-10 스케일로 변환 (DB는 0-5 스케일)
    private String comment; // content
}
