package com.sist.backend.service;

import org.springframework.stereotype.Service;

import com.sist.backend.repository.RoomReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomReservationService {
    
    private final RoomReservationRepository roomReservationRepository;

    /* 오늘 체크인한 사람의 수 조회 */
    public Integer getTodayCheckinCount() {
        return roomReservationRepository.findTodayCheckinCount();
    }

    /* 오늘 체크아웃한 사람의 수 조회 */
    public Integer getTodayCheckoutCount() {
        return roomReservationRepository.findTodayCheckoutCount();
    }

    /* 예약 확정인 사람의 수 조회 */
    public Integer findByStatus() {
        return roomReservationRepository.findByStatus();
    }
}
