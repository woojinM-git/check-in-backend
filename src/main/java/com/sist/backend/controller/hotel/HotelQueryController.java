package com.sist.backend.controller.hotel;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.hotel.HotelImageResponse;
import com.sist.backend.dto.hotel.RoomAvailabilityResponse;
import com.sist.backend.dto.hotel.RoomResponse;
import com.sist.backend.service.hotel.HotelQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
// Swagger 태그: 본 컨트롤러 API 그룹과 설명 정의
@Tag(name = "Hotel Detail", description = "호텔 상세 정보 조회 API")
public class HotelQueryController {

    private final HotelQueryService hotelQueryService;

    // Swagger: 단일 호텔 상세 조회 API 문서
    @Operation(summary = "호텔 상세 조회", description = "contentId로 호텔 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "해당 호텔 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}")
    public ResponseEntity<?> getHotel(@PathVariable(name = "contentId") String contentId) {
        return hotelQueryService.getHotel(contentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Swagger: 객실 목록 조회 API 문서 (날짜 기반 예약 가능성 조회 포함)
    @Operation(summary = "호텔 객실 목록 조회", description = "contentId로 객실 목록을 조회합니다. name으로 부분 검색 가능. 날짜가 제공되면 예약 가능성 정보가 포함됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/rooms")
    public ResponseEntity<List<RoomAvailabilityResponse>> getRooms(
            @PathVariable(name = "contentId") String contentId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "checkinDate", required = false) LocalDate checkinDate,
            @RequestParam(value = "checkoutDate", required = false) LocalDate checkoutDate
    ) {
        return ResponseEntity.ok(hotelQueryService.getRooms(contentId, name, checkinDate, checkoutDate));
    }

    // Swagger: MyBatis 기반 고급 검색 API 문서
    @Operation(summary = "고급 객실 검색", description = "MyBatis로 객실을 상세 조건으로 검색합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/rooms/advanced-search")
    public ResponseEntity<List<RoomResponse>> searchRoomsAdvanced(
            @PathVariable String contentId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "minCapacity", required = false) Integer minCapacity,
            @RequestParam(value = "maxCapacity", required = false) Integer maxCapacity
    ) {
        return ResponseEntity.ok(hotelQueryService.searchRoomsAdvanced(contentId, name, minCapacity, maxCapacity));
    }

    // Swagger: 호텔 이미지 목록 조회 API 문서
    @Operation(summary = "호텔 이미지 목록 조회", description = "contentId로 호텔 이미지 목록을 조회합니다. 최대 10장까지 반환됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/images")
    public ResponseEntity<List<HotelImageResponse>> getHotelImages(@PathVariable(name = "contentId") String contentId) {
        return ResponseEntity.ok(hotelQueryService.getHotelImages(contentId));
    }

    // Swagger: 객실 예약 가능성 조회 API 문서
    @Operation(summary = "객실 예약 가능성 조회", description = "체크인/체크아웃 날짜를 기준으로 객실의 예약 가능 여부를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 날짜 형식"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{contentId}/rooms/availability")
    public ResponseEntity<List<RoomAvailabilityResponse>> getRoomAvailability(
            @PathVariable(name = "contentId") String contentId,
            @RequestParam(name = "checkinDate") LocalDate checkinDate,
            @RequestParam(name = "checkoutDate") LocalDate checkoutDate
    ) {
        return ResponseEntity.ok(hotelQueryService.getRoomAvailability(contentId, checkinDate, checkoutDate));
    }
}
