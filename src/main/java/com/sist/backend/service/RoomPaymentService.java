package com.sist.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.RoomPaymentDto;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.repository.RoomPaymentRepository;

@Service
public class RoomPaymentService {

    //*******결제 테이블(roomPayment) 조회하고 관리자 페이지나 매출 통계용으로 쓰는 서비스)*******
    //관리자 페이지 용도
    @Autowired
    RoomPaymentRepository roomPaymentRepository;

    //총 결제 금액 합계 조회
    public Long findByPrice() {
        return roomPaymentRepository.findByPrice();
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
