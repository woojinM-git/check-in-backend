package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.admin.CheckTimeUpdateDto;
import com.sist.backend.entity.ReservationTime;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.ReservationTimeRepository;
import com.sist.backend.repository.RoomReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;
    private final RoomReservationRepository roomReservationRepository;

    @Transactional
    public void checkin(CheckTimeUpdateDto dto) {
        // 체크인은 항상 최초 1회만 발생하므로 레코드가 없어야 함
        // 없으면 새로 생성하여 저장
        ReservationTime reservationTime = ReservationTime.builder()
            .orderIdx(dto.getOrderIdx())
            .inTime(dto.getInTime() != null ? dto.getInTime() : LocalDateTime.now())
            .outTime(null)
            .build();
        
        // 최초 생성이므로 save() 사용
        reservationTimeRepository.save(reservationTime);
    }

    @Transactional
    public void checkout(CheckTimeUpdateDto dto) {
        // 체크아웃은 체크인이 완료된 후에만 발생하므로 레코드는 항상 존재
        // 영속성 컨텍스트에서 조회하여 변경 감지로 자동 저장
        ReservationTime reservationTime = reservationTimeRepository.findById(dto.getOrderIdx())
            .orElseThrow(() -> new RuntimeException(
                "체크인 정보를 찾을 수 없습니다. 체크인을 먼저 처리해주세요. orderIdx=" + dto.getOrderIdx()));
        
        // 영속화된 엔티티 수정 → 변경 감지로 자동 저장됨
        reservationTime.setOutTime(dto.getOutTime() != null ? dto.getOutTime() : LocalDateTime.now());
        
        // 체크아웃 완료 시 RoomReservation의 status를 1에서 4로 변경
        List<RoomReservation> reservations = roomReservationRepository.findByOrderIdx(dto.getOrderIdx());
        for (RoomReservation reservation : reservations) {
            if (reservation.getStatus() != null && reservation.getStatus() == 1) {
                reservation.setStatus(4); // 이용완료로 변경
                reservation.setUpdatedAt(LocalDateTime.now());
            }
        }
        
        // save() 호출 불필요 - 변경 감지로 자동 저장
    }
}