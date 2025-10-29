package com.sist.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return roomReservationRepository.findByTodayCount();
    }

    /* 가장 최근 예약한 사람의 목록 (5개만) */
    public List<RoomReservation> findByStatus(String contentid) {
        return roomReservationRepository.findByStatus(contentid);
    }

    /* Room과 Customer 정보를 포함한 예약 목록을 DTO로 반환 */
    public List<RoomReservationDto> findByStatusWithDetails(String contentid) {
        List<RoomReservation> reservations = roomReservationRepository.findByStatusWithDetails(contentid);
        return reservations.stream()
            .map(RoomReservationDto::fromEntity)
            .collect(Collectors.toList());
    }

    public Page<RoomReservationDto> findByStatusDto(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findByStatusDto(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 체크인 대기 목록 */
    public Page<RoomReservationDto> findCheckinPendingWithDetails(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findCheckinPendingWithDetails(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 체크아웃 대기 목록 */
    public Page<RoomReservationDto> findCheckoutPendingWithDetails(String contentid, Pageable pageable) {
        Page<RoomReservation> roomReservationPage = roomReservationRepository.findCheckoutPendingWithDetails(contentid, pageable);
        return roomReservationPage.map(RoomReservationDto::fromEntity);
    }

    /* 오늘 예약한 사람의 수 조회 */
    public Integer findByTodayCount() {
        return roomReservationRepository.findByTodayCount();
    }

    /* 특정 예약의 customerIdx 값을 변경하는 메서드 */
    @Transactional
    public int updateCustomerIdxByReservIdx(Integer reservIdx, Integer customerIdx) {
        return roomReservationRepository.updateCustomerIdxByReservIdx(reservIdx, customerIdx);
    }

    /* 달력용 예약 조회 - 날짜 범위로 검색 */
    public List<RoomReservationDto> findByDateRangeWithDetails(String contentid, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<RoomReservation> reservations = roomReservationRepository.findByDateRangeWithDetails(contentid, startDate, endDate);
        return reservations.stream()
            .map(RoomReservationDto::fromEntity)
            .collect(Collectors.toList());
    }
}
