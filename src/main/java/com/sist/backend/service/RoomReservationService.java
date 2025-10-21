package com.sist.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.RoomReservationDto;
import com.sist.backend.entity.RoomReservation;
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

    /* 가장 최근 예약한 사람의 목록 (5개만) */
    public List<RoomReservation> findByStatus(String contentid) {
        return roomReservationRepository.findByStatus(contentid);
    }

    public Page<RoomReservationDto> findByStatusDto(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findByStatusDto(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }
}
