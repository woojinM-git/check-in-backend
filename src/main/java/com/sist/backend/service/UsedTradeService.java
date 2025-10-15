package com.sist.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}
