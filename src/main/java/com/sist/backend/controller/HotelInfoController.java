package com.sist.backend.controller;

import com.sist.backend.dto.HotelInfoDto;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.service.HotelInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "HotelInfo", description = "호텔 정보 API")
public class HotelInfoController {
    
    private final HotelInfoService hotelInfoService;
    
    /**
     * 상위 10개 호텔 조회 (Category, Area 정보 포함)
     */
    @GetMapping("/top10")
    @Operation(summary = "상위 10개 호텔 조회", description = "카테고리와 지역 정보를 포함한 상위 10개 호텔을 조회합니다.")
    public ResponseEntity<List<HotelInfoDto>> getTop10Hotels() {
        List<HotelInfo> hotels = hotelInfoService.getTop10Hotels();
        List<HotelInfoDto> dtoList = hotels.stream()
                .map(HotelInfoDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
    
    /**
     * 페이징 처리된 호텔 목록 조회
     */
    @GetMapping
    @Operation(summary = "호텔 목록 조회", description = "페이징 처리된 호텔 목록을 조회합니다.")
    public ResponseEntity<Page<HotelInfoDto>> getHotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<HotelInfo> hotelPage = hotelInfoService.getHotelsWithPaging(page, size);
        Page<HotelInfoDto> dtoPage = hotelPage.map(HotelInfoDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}

