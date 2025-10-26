package com.sist.backend.dto.hotel;

//호텔 실시간 조회자 응답 DTO

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

//비즈니스 로직(Redis)
/**
 * Redis 기반 호텔 실시간 조회자 응답 DTO
 * - HotelViewResponse와 구분 (Redis 명시)
 */
@Getter
@Setter
@Builder
@Schema(description = "호텔 실시간 조회자 응답 DTO")
public class HotelViewRedisResponse {

    @Schema(description = "응답 메세지(success / fail")
    private String message;

    @Schema(description = "데이터 본문")
    private Map<String, Object> data;

     //조회자 수 반환 시 사용
    public static HotelViewRedisResponse of(int count){
        return HotelViewRedisResponse.builder()
                .message("success")
                .data(Map.of("views",count))
                .build();
    }
     //단순 성공 메시지용
    public static HotelViewRedisResponse success(){
        return HotelViewRedisResponse.builder()
                .message("success")
                .data(Map.of())
                .build();
    }
}
