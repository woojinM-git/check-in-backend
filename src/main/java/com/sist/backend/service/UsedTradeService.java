package com.sist.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.sist.backend.dto.UsedItemDto;
import com.sist.backend.entity.UsedItem;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.UsedItemRepository;
import com.sist.backend.repository.RoomReservationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsedTradeService {

    final private UsedItemRepository usedItemRepository;
    final private RoomReservationRepository roomReservationRepository;

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
    @Transactional
    public UsedItem registerUsedItem(Integer reservIdx, Integer price, String comment) {
        // 기존 항목이 있는지 확인
        UsedItem existingItem = usedItemRepository.findByReservIdx(reservIdx);
        
        if (existingItem != null) {
            // status가 2(거래완료) 또는 4(취소)인 경우 새로 등록 가능
            if (existingItem.getStatus() == 2 || existingItem.getStatus() == 4) {
                // 재판매를 위해 새로 등록 (기존 항목은 그대로 유지)
                log.info("양도거래 재판매 등록: reservIdx={}, 기존 status={}", reservIdx, existingItem.getStatus());
                // 기존 항목은 그대로 두고 새로 등록
            } else {
                // 이미 판매중(0) 또는 거래중(1) 상태인 경우 오류
                throw new RuntimeException("이미 등록된 양도거래가 있습니다.");
            }
        }
        
        // RoomReservation 조회하여 sellerIdx 설정
        RoomReservation reservation = roomReservationRepository.findById(reservIdx)
                .orElseThrow(() -> new RuntimeException("예약 정보를 찾을 수 없습니다."));
        
        // 새로 등록
        UsedItem usedItem = new UsedItem();
        usedItem.setReservIdx(reservIdx);
        usedItem.setPrice(price);
        usedItem.setComment(comment);
        usedItem.setSellerIdx(reservation.getCustomerIdx()); // 판매자 ID 설정
        // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료, 4 = 취소
        usedItem.setStatus(0); // 판매중 상태로 등록
        
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

    /**
     * 양도거래 아이템 취소
     * @param usedItemIdx 양도거래 아이템 ID
     * @return 취소된 UsedItem
     */
    @Transactional
    public UsedItem cancelUsedItem(Integer usedItemIdx) {
        var usedItem = usedItemRepository.findById(usedItemIdx)
            .orElseThrow(() -> new RuntimeException("양도거래 아이템을 찾을 수 없습니다."));
        
        // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료, 4 = 취소
        usedItem.setStatus(4); // 취소 상태로 변경
        
        log.info("양도거래 아이템 취소: usedItemIdx={}", usedItemIdx);
        return usedItemRepository.save(usedItem);
    }

    /**
     * checkin 날짜가 오늘 날짜와 같거나 이전인 양도매물 만료 처리
     * @return 만료 처리된 매물 개수
     */
    @Transactional
    public int expireUsedItems() {
        LocalDate today = LocalDate.now();
        List<UsedItem> expiredItems = usedItemRepository.findExpiredUsedItems(today);
        
        if (expiredItems.isEmpty()) {
            log.info("만료 처리할 양도매물이 없습니다.");
            return 0;
        }
        
        int expiredCount = 0;
        for (UsedItem item : expiredItems) {
            // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료
            item.setStatus(3); // 만료 상태로 변경
            usedItemRepository.save(item);
            expiredCount++;
        }
        
        log.info("만료 처리 완료: {}개의 양도매물이 만료되었습니다.", expiredCount);
        return expiredCount;
    }

    /**
     * 판매자의 양도거래 아이템 목록 조회
     * @param sellerIdx 판매자 ID
     * @return 판매자의 UsedItem 목록
     */
    @Transactional(readOnly = true)
    public List<UsedItem> getSellerItems(Integer sellerIdx) {
        List<UsedItem> items = usedItemRepository.findBySellerIdx(sellerIdx);
        log.info("판매자 아이템 조회 - sellerIdx: {}, 조회된 개수: {}", sellerIdx, items.size());
        return items;
    }
}
