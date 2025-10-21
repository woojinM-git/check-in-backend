package com.sist.backend.controller.hotel;

import com.sist.backend.dto.hotel.HotelFacilityResponse;
import com.sist.backend.service.hotel.HotelFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotel Detail", description = "호텔 상세 정보 조회 API")
public class HotelFacilityController {

    private final HotelFacilityService hotelFacilityService;

    @Operation(summary = "호텔 편의시설 및 위치 정보 조회", description = "contentId로 호텔의 편의시설 및 위치 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/facilities")
    public ResponseEntity<HotelFacilityResponse> getHotelFacilities(@PathVariable String contentId) {
        return hotelFacilityService.getHotelFacilities(contentId)
                .<ResponseEntity<HotelFacilityResponse>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
