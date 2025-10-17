package com.sist.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.entity.Center;
import com.sist.backend.repository.CenterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CenterService {
    
    private final CenterRepository centerRepository;
    
    /**
     * 고객센터 글 등록
     */
    @Transactional
    public Center createCenter(Center center) {
        return centerRepository.save(center);
    }
    
    
    /**
     * 고객센터 글 상세 조회
     */
    public Center getCenterById(Integer centerIdx) {
        return centerRepository.findById(centerIdx)
            .orElseThrow(() -> new RuntimeException("고객센터 글을 찾을 수 없습니다."));
    }
    
    /**
     * 고객센터 글 수정
     */
    @Transactional
    public Center updateCenter(Integer centerIdx, Center center) {
        Center existingCenter = centerRepository.findById(centerIdx)
            .orElseThrow(() -> new RuntimeException("고객센터 글을 찾을 수 없습니다."));
        
        // 수정 가능한 필드들만 업데이트
        existingCenter.setTitle(center.getTitle());
        existingCenter.setMainCategory(center.getMainCategory());
        existingCenter.setSubCategory(center.getSubCategory());
        existingCenter.setPriority(center.getPriority());
        existingCenter.setContent(center.getContent());
        existingCenter.setStatus(center.getStatus());
        existingCenter.setHide(center.getHide());
        
        return centerRepository.save(existingCenter);
    }
    
    /**
     * 고객센터 글 삭제
     */
    @Transactional
    public void deleteCenter(Integer centerIdx) {
        if (!centerRepository.existsById(centerIdx)) {
            throw new RuntimeException("고객센터 글을 찾을 수 없습니다.");
        }
        centerRepository.deleteById(centerIdx);
    }
    
    
    /**
     * 복합 조건 검색
     */
    public Page<Center> searchByMultipleConditions(
        String mainCategory,
        String subCategory,
        Integer status,
        Integer priority,
        Integer customerIdx,
        Integer adminIdx,
        String title,
        Pageable pageable
    ) {
        return centerRepository.findByMultipleConditions(
            mainCategory, subCategory, status, priority, customerIdx, adminIdx, title, pageable
        );
    }
}
