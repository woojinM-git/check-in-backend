package com.sist.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.entity.UsedItem;
import com.sist.backend.service.UsedTradeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/used")
@RequiredArgsConstructor
public class UsedTradeController {

    final private UsedTradeService usedTradeService;
    
    @GetMapping("/list")
    public ResponseEntity<Page<UsedItem>> getUsedTradeList(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(usedTradeService.findAllByStatusOrderByUpdatedAtDesc(pageable));
    }
}
