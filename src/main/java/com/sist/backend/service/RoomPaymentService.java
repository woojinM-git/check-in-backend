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

    @Autowired
    RoomPaymentRepository roomPaymentRepository;

    public Long findByPrice() {
        return roomPaymentRepository.findByPrice();
    }

    public Page<RoomPaymentDto> findByOrderIdxAndInTime(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndInTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }

    public Page<RoomPaymentDto> findByOrderIdxAndOutTime(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndOutTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }

    public Page<RoomPaymentDto> findByContentId(String contentId, Pageable pageable) {
        Page<RoomPayment> roomPayments = roomPaymentRepository.findByOrderIdxAndOutTime(contentId, pageable);
        return roomPayments.map(RoomPaymentDto::fromEntity);
    }
}
