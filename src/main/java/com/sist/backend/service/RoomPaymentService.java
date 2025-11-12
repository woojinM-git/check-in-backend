package com.sist.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.RoomPaymentDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.repository.RoomPaymentRepository;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 결제 테이블(roomPayment) 조회 서비스 관리자 페이지 및 매출 통계용
 *
 * 실제 결제 처리 로직은 PaymentService에서 담당합니다. - 결제 검증 및 저장 - 포인트/캐시 차감 및 PointLedger
 * 기록 - 쿠폰 처리 - 적립 및 등급 업데이트
 */
@Service
@Tag(name = "결제 통계", description = "결제 내역 조회 및 통계 API (관리자)")
public class RoomPaymentService {

    @Autowired
    RoomPaymentRepository roomPaymentRepository;

    // 특정 호텔의 오늘 매출 조회 (결제 승인일 기준)
    public Long findByPrice(String contentId) {
        return roomPaymentRepository.findByPrice(contentId);
    }

    //체크인 시간 기준 결제 내역 조회
    public Page<RoomPaymentDto> findByOrderIdxAndInTime(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndInTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }

    //체크아웃 시간 기준 결제 내역 조회
    public Page<RoomPaymentDto> findByOrderIdxAndOutTime(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndOutTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }

    //특정 호텔의 결제 전체 내역 조회
    public Page<RoomPaymentDto> findByContentId(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndOutTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }

    // 특정 호텔의 평균 결제 금액
    public Double findAveragePaymentByContentId(String contentid) {
        return roomPaymentRepository.findAveragePaymentByContentId(contentid);
    }
}
