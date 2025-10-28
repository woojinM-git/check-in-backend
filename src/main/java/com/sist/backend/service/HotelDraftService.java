package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.hotel.HotelDraftDto;
import com.sist.backend.entity.HotelDraft;
import com.sist.backend.repository.HotelDraftRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelDraftService {
    
    private final HotelDraftRepository hotelDraftRepository;
    
    // 임시저장 데이터 저장 또는 업데이트 (UPSERT)
    @Transactional
    public HotelDraftDto saveDraft(Integer adminIdx, String formData, String lastTab, Integer progress) {
        Optional<HotelDraft> existingDraft = hotelDraftRepository.findByAdminIdx(adminIdx);
        
        HotelDraft hotelDraft;
        if (existingDraft.isPresent()) {
            // 기존 데이터 업데이트
            hotelDraft = existingDraft.get();
            hotelDraft.setFormData(formData);
            hotelDraft.setLastTab(lastTab);
            hotelDraft.setProgress(progress);
            hotelDraft.setUpdatedAt(LocalDateTime.now());
        } else {
            // 새 데이터 생성
            hotelDraft = new HotelDraft();
            hotelDraft.setAdminIdx(adminIdx);
            hotelDraft.setFormData(formData);
            hotelDraft.setLastTab(lastTab);
            hotelDraft.setProgress(progress);
            hotelDraft.setCreatedAt(LocalDateTime.now());
            hotelDraft.setUpdatedAt(LocalDateTime.now());
        }
        
        HotelDraft savedDraft = hotelDraftRepository.save(hotelDraft);
        return HotelDraftDto.fromEntity(savedDraft);
    }
    
    // 임시저장 데이터 조회 (adminIdx로)
    public Optional<HotelDraftDto> getDraft(Integer adminIdx) {
        return hotelDraftRepository.findByAdminIdx(adminIdx)
                .map(HotelDraftDto::fromEntity);
    }

    // 임시저장 데이터 조회 (draftIdx로)
    public Optional<HotelDraft> findById(Integer draftIdx) {
        return hotelDraftRepository.findById(draftIdx);
    }
    
    // 임시저장 데이터 삭제
    @Transactional
    public void deleteDraft(Integer adminIdx) {
        hotelDraftRepository.deleteByAdminIdx(adminIdx);
    }
    
    // 임시저장 데이터 존재 여부 확인
    public boolean hasDraft(Integer adminIdx) {
        return hotelDraftRepository.existsByAdminIdx(adminIdx);
    }
}

