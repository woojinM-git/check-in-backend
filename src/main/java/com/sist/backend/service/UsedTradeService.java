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

    /**
     * 양도거래 아이템 등록
     * @param reservIdx 예약 ID
     * @param price 판매 가격
     * @param comment 설명
     * @return 등록된 UsedItem
     */
    public UsedItem registerUsedItem(Integer reservIdx, Integer price, String comment) {
        UsedItem usedItem = new UsedItem();
        usedItem.setReservIdx(reservIdx);
        usedItem.setPrice(price);
        usedItem.setComment(comment);
        usedItem.setStatus(0); // 0: 등록됨
        
        return usedItemRepository.save(usedItem);
    }

    /**
     * 예약에 대한 양도거래 아이템 조회
     * @param reservIdx 예약 ID
     * @return UsedItem 또는 null
     */
    public UsedItem findByReservIdx(Integer reservIdx) {
        return usedItemRepository.findByReservIdx(reservIdx);
    }

    /**
     * 양도거래 아이템 수정
     * @param usedItemIdx 양도거래 아이템 ID
     * @param price 판매 가격
     * @param comment 설명
     * @return 수정된 UsedItem
     */
    public UsedItem updateUsedItem(Integer usedItemIdx, Integer price, String comment) {
        var usedItem = usedItemRepository.findById(usedItemIdx)
            .orElseThrow(() -> new RuntimeException("양도거래 아이템을 찾을 수 없습니다."));
        
        usedItem.setPrice(price);
        usedItem.setComment(comment);
        // updatedAt은 @PreUpdate로 자동 갱신됨
        
        return usedItemRepository.save(usedItem);
    }
}
