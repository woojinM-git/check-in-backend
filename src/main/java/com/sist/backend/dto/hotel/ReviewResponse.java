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
    private Double rating; // star를 1-5 스케일로 (DB는 1-5 스케일)
    private String comment; // content
    private List<String> images; // 리뷰 이미지 URL 배열 (imageUrl + imageUrl2~5 중 null이 아닌 것들)
}
