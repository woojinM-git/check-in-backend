package com.sist.backend.service;

import com.sist.backend.entity.Dining;
import com.sist.backend.repository.DiningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningService {
    
    private final DiningRepository diningRepository;
    
    /**
     * 활성화된 다이닝 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Dining> getActiveDinings(Pageable pageable) {
        return diningRepository.findActiveDinings(pageable);
    }
    
    /**
     * 호텔별 다이닝 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Dining> getDiningsByHotel(String contentid) {
        return diningRepository.findByContentidAndStatus(contentid);
    }
    
    /**
     * 다이닝 검색 (호텔 주소, 호텔 이름, 다이닝 이름으로 검색)
     */
    @Transactional(readOnly = true)
    public Page<Dining> searchDinings(String destination, Pageable pageable) {
        log.info("다이닝 검색: destination={}", destination);
        return diningRepository.searchDinings(destination, pageable);
    }
    
    /**
     * 가격 범위로 다이닝 검색
     */
    @Transactional(readOnly = true)
    public Page<Dining> searchDiningsByPrice(Integer priceMin, Integer priceMax, Pageable pageable) {
        log.info("가격 범위 검색: {} ~ {}", priceMin, priceMax);
        return diningRepository.findByPriceRange(priceMin, priceMax, pageable);
    }
    
    /**
     * 식사 시간대별 다이닝 검색
     */
    @Transactional(readOnly = true)
    public Page<Dining> searchDiningsByMealType(String mealType, Pageable pageable) {
        LocalTime searchTime = getMealTime(mealType);
        log.info("식사 시간대 검색: mealType={}, time={}", mealType, searchTime);
        return diningRepository.findByMealType(mealType, searchTime, pageable);
    }
    
    /**
     * 복합 조건으로 다이닝 검색
     */
    @Transactional(readOnly = true)
    public Page<Dining> searchDiningsWithFilters(String destination,
                                               Integer priceMin,
                                               Integer priceMax,
                                               String mealType,
                                               String sortBy,
                                               String sortDirection,
                                               Pageable pageable) {
        LocalTime searchTime = getMealTime(mealType);
        
        log.info("복합 조건 검색: destination={}, priceMin={}, priceMax={}, mealType={}, sortBy={}", 
                destination, priceMin, priceMax, mealType, sortBy);
        
        return diningRepository.searchDiningsWithFilters(
            destination, priceMin, priceMax, mealType, searchTime, 
            sortBy, sortDirection, pageable
        );
    }
    
    /**
     * 다이닝 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public Dining getDiningDetail(Integer diningIdx) {
        return diningRepository.findById(diningIdx)
            .orElseThrow(() -> new RuntimeException("다이닝을 찾을 수 없습니다: " + diningIdx));
    }
    
    /**
     * 식사 시간대에 따른 검색 시간 반환
     */
    private LocalTime getMealTime(String mealType) {
        if (mealType == null) return null;
        
        return switch (mealType.toLowerCase()) {
            case "breakfast" -> LocalTime.of(8, 0);  // 08:00
            case "lunch" -> LocalTime.of(12, 0);     // 12:00
            case "dinner" -> LocalTime.of(19, 0);    // 19:00
            default -> null;
        };
    }
}
