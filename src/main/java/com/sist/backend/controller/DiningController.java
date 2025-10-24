package com.sist.backend.controller;

import com.sist.backend.entity.Dining;
import com.sist.backend.service.DiningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dining")
@RequiredArgsConstructor
public class DiningController {
    
    private final DiningService diningService;
    
    /**
     * 다이닝 목록 조회 (페이징)
     */
    @GetMapping("/list")
    public ResponseEntity<Page<Dining>> getDiningList(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<Dining> dinings = diningService.getActiveDinings(pageable);
        return ResponseEntity.ok(dinings);
    }
    
    /**
     * 호텔별 다이닝 목록 조회
     */
    @GetMapping("/hotel/{contentid}")
    public ResponseEntity<List<Dining>> getDiningsByHotel(@PathVariable String contentid) {
        List<Dining> dinings = diningService.getDiningsByHotel(contentid);
        return ResponseEntity.ok(dinings);
    }
    
    /**
     * 다이닝 검색 (호텔 주소, 호텔 이름, 다이닝 이름으로 검색)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Dining>> searchDinings(
            @RequestParam(required = false) String destination,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<Dining> dinings = diningService.searchDinings(destination, pageable);
        return ResponseEntity.ok(dinings);
    }
    
    /**
     * 복합 조건으로 다이닝 검색
     */
    @PostMapping("/search")
    public ResponseEntity<Page<Dining>> searchDiningsWithFilters(
            @RequestBody Map<String, Object> searchParams,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String destination = (String) searchParams.get("destination");
        Integer priceMin = searchParams.get("priceMin") != null ? 
            Integer.valueOf(searchParams.get("priceMin").toString()) : null;
        Integer priceMax = searchParams.get("priceMax") != null ? 
            Integer.valueOf(searchParams.get("priceMax").toString()) : null;
        String mealType = (String) searchParams.get("mealType");
        String sortBy = (String) searchParams.getOrDefault("sortBy", "updatedAt");
        String sortDirection = (String) searchParams.getOrDefault("sortDirection", "desc");
        
        Page<Dining> dinings = diningService.searchDiningsWithFilters(
            destination, priceMin, priceMax, mealType, sortBy, sortDirection, pageable
        );
        
        return ResponseEntity.ok(dinings);
    }
    
    /**
     * 다이닝 상세 정보 조회
     */
    @GetMapping("/{diningIdx}")
    public ResponseEntity<Dining> getDiningDetail(@PathVariable Integer diningIdx) {
        Dining dining = diningService.getDiningDetail(diningIdx);
        return ResponseEntity.ok(dining);
    }
    
    /**
     * 가격 범위로 다이닝 검색
     */
    @GetMapping("/search/price")
    public ResponseEntity<Page<Dining>> searchDiningsByPrice(
            @RequestParam(required = false) Integer priceMin,
            @RequestParam(required = false) Integer priceMax,
            @PageableDefault(size = 10, sort = "basePrice", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<Dining> dinings = diningService.searchDiningsByPrice(priceMin, priceMax, pageable);
        return ResponseEntity.ok(dinings);
    }
    
    /**
     * 식사 시간대별 다이닝 검색
     */
    @GetMapping("/search/meal-type")
    public ResponseEntity<Page<Dining>> searchDiningsByMealType(
            @RequestParam String mealType,
            @PageableDefault(size = 10, sort = "openTime", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<Dining> dinings = diningService.searchDiningsByMealType(mealType, pageable);
        return ResponseEntity.ok(dinings);
    }
}
