package com.sist.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.repository.CouponTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponTemplateService {
    final private CouponTemplateRepository couponTemplateRepository;

    /* 모든 템플릿 조회 */
    public List<CouponTemplate> findAll() {
        return couponTemplateRepository.findAll();
    }

    /* 활성화 중인 템플릿 조회 */
    public List<CouponTemplate> findByStatus() {
        return couponTemplateRepository.findByStatus();
    }
}
