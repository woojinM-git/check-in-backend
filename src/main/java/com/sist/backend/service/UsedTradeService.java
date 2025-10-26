package com.sist.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;

import com.sist.backend.dto.UsedItemDto;
import com.sist.backend.entity.UsedItem;
import com.sist.backend.repository.UsedItemRepository;

@Service
@RequiredArgsConstructor
public class UsedTradeService {

    final private UsedItemRepository usedItemRepository;

    public Page<UsedItem> findAllByStatusOrderByUpdatedAtDesc(Pageable pageable) {
        return usedItemRepository.findAllByStatusOrderByUpdatedAtDesc(pageable);
    }
    
    // DTO 변환 메서드 추가
    public Page<UsedItemDto> findAllByStatusOrderByUpdatedAtDescAsDto(Pageable pageable) {
        Page<UsedItem> usedItemPage = usedItemRepository.findAllByStatusOrderByUpdatedAtDesc(pageable);
        
        // Page<UsedItem>을 Page<UsedItemDto>로 변환
        return usedItemPage.map(UsedItemDto::fromEntity);
    }

    /**
     * 복합 조건 검색
     */
    public Page<UsedItemDto> searchByMultipleConditions(
        String destination,
        String checkIn,
        String checkOut,
        Integer adults,
        Integer priceMin,
        Integer priceMax,
        String sortBy,
        String sortDirection,
        Integer status,
        Pageable pageable
    ) {
        // String을 LocalDate로 변환
        LocalDate checkInDate = null;
        LocalDate checkOutDate = null;
        
        if (checkIn != null && !checkIn.isEmpty()) {
            try {
                checkInDate = LocalDate.parse(checkIn, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                // 날짜 파싱 실패 시 null로 처리
                checkInDate = null;
            }
        }
        
        if (checkOut != null && !checkOut.isEmpty()) {
            try {
                checkOutDate = LocalDate.parse(checkOut, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                // 날짜 파싱 실패 시 null로 처리
                checkOutDate = null;
            }
        }
        
        Page<UsedItem> usedItemPage = usedItemRepository.findByMultipleConditions(
            destination, checkInDate, checkOutDate, adults, priceMin, priceMax, sortBy, sortDirection, status, pageable
        );
        
        // Page<UsedItem>을 Page<UsedItemDto>로 변환
        return usedItemPage.map(UsedItemDto::fromEntity);
    }
}
