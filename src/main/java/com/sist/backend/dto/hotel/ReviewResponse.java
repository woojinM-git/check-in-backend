package com.sist.backend.dto.hotel;

import java.util.List;

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
    private String imageUrl; // imageUrl(대표이미지 1장)
    private List<String> imageUrls; // imageUrls(나머지 이미지들)
}
