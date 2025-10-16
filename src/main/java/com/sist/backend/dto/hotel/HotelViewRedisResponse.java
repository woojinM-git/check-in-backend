package com.sist.backend.dto.hotel;

//호텔 실시간 조회자 응답 DTO

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(description = "호텔 실시간 조회자 응답 DTO")
public class HotelViewRedisResponse {

    @Schema(description = "응답 메세지")
    private String message;

    @Schema(description = "데이터 본문")
    private Map<String, Object> data;

    /**
     * 조회자 수 반환 시 사용
     */
    public static HotelViewRedisResponse of(int count){
        return HotelViewRedisResponse.builder()
                .message("success")
                .data(Map.of("views",count))
                .build();
    }
    /**
     * 단순 성공 메시지용
     */
    public static HotelViewRedisResponse success(){
        return HotelViewRedisResponse.builder()
                .message("success")
                .data(Map.of())
                .build();
    }
}
