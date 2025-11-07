package com.sist.backend.service;

import com.sist.backend.dto.admin.CouponDto;
import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.CouponTemplate;
import com.sist.backend.entity.Customer;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.CouponTemplateRepository;
import com.sist.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {
    
    private final CouponRepository couponRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final CustomerRepository customerRepository;
    
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
        coupon.setEndDate(LocalDateTime.now().plusDays(template.getValidDays() != null ? template.getValidDays() : 30));
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

    public Page<CouponDto> findByAdminIdx(Integer adminIdx, Pageable pageable) {
        Page<Coupon> couponPage = couponRepository.findByAdminIdx(adminIdx, pageable);
        return couponPage.map(CouponDto::fromEntity);
    }

    /**
     * 등급별 쿠폰 일괄 발급
     * 선택한 등급의 모든 활성 고객에게 고유한 쿠폰을 발급
     * 
     * @param rank 등급 (예: "VIP", "Traveler", "Sky Suite", "First Class", "Explorer")
     * @param templateIdx 쿠폰 템플릿 번호
     * @param adminIdx 발급하는 마스터 관리자 번호
     * @return 발급된 쿠폰 개수
     */
    @Transactional
    public int batchIssueCouponsByRank(String rank, Integer templateIdx, Integer adminIdx) {
        // 템플릿 존재 및 활성화 확인
        CouponTemplate template = couponTemplateRepository.findById(templateIdx)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 템플릿입니다: " + templateIdx));
        
        if (template.getStatus() == null || template.getStatus() == 0) {
            throw new IllegalArgumentException("비활성화된 쿠폰 템플릿입니다: " + templateIdx);
        }

        // 등급별 활성 고객 조회 (status = 0)
        List<Customer> customers = customerRepository.findByRankAndStatus(rank);
        
        if (customers.isEmpty()) {
            log.warn("등급 '{}'에 해당하는 활성 고객이 없습니다.", rank);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(template.getValidDays() != null ? template.getValidDays() : 30);

        // 각 고객에게 고유한 쿠폰 발급
        int issuedCount = 0;
        for (Customer customer : customers) {
            Coupon coupon = new Coupon();
            coupon.setTemplateIdx(templateIdx);
            coupon.setCustomerIdx(customer.getCustomerIdx());
            coupon.setAdminIdx(adminIdx);
            coupon.setCreateDate(now);
            coupon.setEndDate(endDate);
            coupon.setStatus(false); // 미사용 상태로 명시적 설정 (0)
            
            couponRepository.save(coupon);
            issuedCount++;
        }

        log.info("등급 '{}'에 해당하는 {}명의 고객에게 쿠폰 템플릿 '{}' 일괄 발급 완료", 
            rank, issuedCount, template.getTemplateName());
        
        return issuedCount;
    }

    /**
     * 등급별 활성 고객 수 조회
     * 
     * @param rank 등급
     * @return 해당 등급의 활성 고객 수
     */
    public Long getCustomerCountByRank(String rank) {
        Long count = customerRepository.countByRankAndStatus(rank);
        return count != null ? count : 0L;
    }

    /**
     * 모든 등급별 활성 고객 수 조회
     * 
     * @return 등급별 인원수 Map (rank -> count)
     */
    public Map<String, Long> getAllRankCustomerCounts() {
        List<String> ranks = List.of("Explorer", "First Class", "Sky Suite", "Traveler", "VIP");
        Map<String, Long> rankCounts = new HashMap<>();
        
        for (String rank : ranks) {
            Long count = customerRepository.countByRankAndStatus(rank);
            rankCounts.put(rank, count != null ? count : 0L);
        }
        
        return rankCounts;
    }
}
