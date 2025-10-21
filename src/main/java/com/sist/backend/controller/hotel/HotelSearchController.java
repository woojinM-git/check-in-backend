package com.sist.backend.controller.hotel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.dto.hotel.HotelcardResponse;
import com.sist.backend.service.hotel.HotelSearchService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/hotel")
@Tag(name="호텔 검색", description="호텔 검색 관련 API")
public class HotelSearchController {

    @Autowired
    private HotelSearchService hotelSearchService;
    
    @PostMapping("/search")
    @Operation(summary="호텔 전체 검색", description="호텔 무작정 가져오기")
    public ResponseEntity<List<HotelInfo>> searchHotels(@RequestBody HotelInfo request){
        System.out.println("==================================="+request+"========================================");
        List<HotelInfo> hotels = hotelSearchService.findByTitle(request.getTitle());
        System.out.println("==================================="+hotels.size()+"========================================");
        return ResponseEntity.ok(hotels);
    }

    @PostMapping("/search/page")
    @Operation(summary="호텔 조건 검색", description="이름, 날짜와 페이지 번호로 호텔 가져오기")
    public ResponseEntity<List<HotelInfo>> searchHotelsByCondition(@RequestBody HotelInfo request){
        List<HotelInfo> hotels = hotelSearchService.findAll();
        return ResponseEntity.ok(hotels); 
    }
    
    @Operation(summary = "인기 호텔 조회", description = "인기 호텔 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })

    @GetMapping("/popular")
    public ResponseEntity<List<HotelcardResponse>> getPopularHotels(){
        List<HotelcardResponse> hotels = hotelSearchService.findAllPopularHotels();
        return ResponseEntity.ok(hotels);
    }

    @Operation(summary = "지역코드별 호텔 조회", description = "지역코드를 기준으로 호텔 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "해당 지역에 호텔 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    
    @GetMapping("/area")
    public ResponseEntity<List<HotelcardResponse>> getHotelsByAreaCode(
            @RequestParam String areaCode,
            @RequestParam(defaultValue = "10") int limit) {
        List<HotelcardResponse> hotels = hotelSearchService.findHotelsByAreaCode(areaCode, limit);
        return ResponseEntity.ok(hotels);
    }
    

}
