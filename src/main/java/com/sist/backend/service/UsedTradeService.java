package com.sist.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.sist.backend.entity.UsedItem;
import com.sist.backend.repository.UsedItemRepository;

@Service
@RequiredArgsConstructor
public class UsedTradeService {

    final private UsedItemRepository usedItemRepository;

    public Page<UsedItem> findAllByStatusOrderByUpdatedAtDesc(Pageable pageable) {
        return usedItemRepository.findAllByStatusOrderByUpdatedAtDesc(pageable);
    }
}
