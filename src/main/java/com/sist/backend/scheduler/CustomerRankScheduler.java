package com.sist.backend.scheduler;

import com.sist.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객 등급 업데이트 스케줄러
 * 매일 오전 9시에 totalPrice 값에 따라 고객 등급을 자동 조정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerRankScheduler {

    private final CustomerRepository customerRepository;

    /**
     * 서버 시작 시 한 번 실행 (애플리케이션이 완전히 준비된 후 실행)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("서버 시작 시 고객 등급 업데이트 실행");
        updateCustomerRanks();
    }

    /**
     * 매일 오전 9시에 고객 등급 업데이트
     * 등급 기준:
     * - Traveler: 0원 이상 ~ 199,999원
     * - Explorer: 200,000원 이상 ~ 799,999원
     * - VIP: 800,000원 이상 ~ 1,999,999원
     * - First Class: 2,000,000원 이상 ~ 4,999,999원
     * - Sky Suite: 5,000,000원 이상
     */
    @Async
    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시
    @Transactional
    public void updateCustomerRanks() {
        try {
            log.info("=== 고객 등급 업데이트 배치 작업 시작 ===");
            
            // 모든 활성 고객 조회 (customerIdx와 totalPrice만)
            List<Object[]> customers = customerRepository.findAllCustomerIdxAndTotalPrice();
            
            int updatedCount = 0;
            int travelerCount = 0;
            int explorerCount = 0;
            int vipCount = 0;
            int firstClassCount = 0;
            int skySuiteCount = 0;
            
            for (Object[] customer : customers) {
                Integer customerIdx = (Integer) customer[0];
                Integer totalPrice = customer[1] != null ? (Integer) customer[1] : 0;
                
                String newRank = calculateRank(totalPrice);
                
                // 현재 등급과 다를 경우에만 업데이트
                String currentRank = customerRepository.findRankByCustomerIdx(customerIdx);
                if (currentRank != null && !currentRank.equals(newRank)) {
                    customerRepository.updateRankByCustomerIdx(customerIdx, newRank);
                    updatedCount++;
                    log.debug("고객 ID: {}, 총 구매액: {}원, 등급 변경: {} -> {}", 
                        customerIdx, totalPrice, currentRank, newRank);
                }
                
                // 등급별 통계
                switch (newRank) {
                    case "Traveler":
                        travelerCount++;
                        break;
                    case "Explorer":
                        explorerCount++;
                        break;
                    case "VIP":
                        vipCount++;
                        break;
                    case "First Class":
                        firstClassCount++;
                        break;
                    case "Sky Suite":
                        skySuiteCount++;
                        break;
                }
            }
            
            log.info("=== 고객 등급 업데이트 배치 작업 완료 ===");
            log.info("총 {}명의 고객 등급이 업데이트되었습니다.", updatedCount);
            log.info("등급별 분포 - Traveler: {}명, Explorer: {}명, VIP: {}명, First Class: {}명, Sky Suite: {}명",
                travelerCount, explorerCount, vipCount, firstClassCount, skySuiteCount);
        } catch (Exception e) {
            log.error("고객 등급 업데이트 배치 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * totalPrice에 따라 등급 계산
     * @param totalPrice 총 구매액
     * @return 등급명
     */
    private String calculateRank(Integer totalPrice) {
        if (totalPrice == null) {
            totalPrice = 0;
        }
        
        if (totalPrice >= 5_000_000) {
            return "Sky Suite";
        } else if (totalPrice >= 2_000_000) {
            return "First Class";
        } else if (totalPrice >= 800_000) {
            return "VIP";
        } else if (totalPrice >= 200_000) {
            return "Explorer";
        } else {
            return "Traveler";
        }
    }
}

