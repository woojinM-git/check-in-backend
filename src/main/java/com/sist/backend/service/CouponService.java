package com.sist.backend.service;

import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    
    private final CouponRepository couponRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    
    /**
     * 쿠폰 생성
     * @param templateIdx 쿠폰 템플릿 ID
     * @param customerIdx 고객 ID
     * @param adminIdx 관리자 ID
     * @return 생성된 쿠폰
     */
    @Transactional
    public Coupon createCoupon(Integer templateIdx, Integer customerIdx, Integer adminIdx) {
        // 템플릿 존재 확인
        CouponTemplate template = couponTemplateRepository.findById(templateIdx)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 템플릿입니다: " + templateIdx));
        
        // 템플릿이 활성화 상태인지 확인
        if (template.getStatus() == null || template.getStatus() == 0) {
            throw new IllegalArgumentException("비활성화된 쿠폰 템플릿입니다: " + templateIdx);
        }

        // 쿠폰 생성
        Coupon coupon = new Coupon();
        coupon.setTemplateIdx(templateIdx);
        coupon.setCustomerIdx(customerIdx);
        coupon.setAdminIdx(adminIdx);
        coupon.setCreateDate(LocalDateTime.now());
        coupon.setEndDate(LocalDateTime.now().plusDays(template.getValidDays()));
        coupon.setStatus(false); // 미사용 상태로 생성

        return couponRepository.save(coupon);
    }

    /**
     * 고객별 쿠폰 조회
     */
    public List<Coupon> getCouponsByCustomer(Integer customerIdx) {
        return couponRepository.findByCustomerIdx(customerIdx);
    }

    /**
     * 활성 쿠폰 조회 (미사용 상태)
     */
    public List<Coupon> getActiveCoupons(Integer customerIdx) {
        return couponRepository.findByCustomerIdx(customerIdx).stream()
            .filter(coupon -> !coupon.getStatus() && coupon.getEndDate().isAfter(LocalDateTime.now()))
            .toList();
    }
}
