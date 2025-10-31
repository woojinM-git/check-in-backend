package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.entity.Room;
import com.sist.backend.entity.RoomId;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

    //호텔 예약 db삽입을 담당하는 핵심 비즈니스 로직임
    //결제 완료 -> QR코드 생성 -> DB저장까지 한번에 처리
    //트랜잭션 성공시 DB삽입 실패하면 롤백됨
    private final RoomReservationRepository roomReservationRepository;
    private final RoomRepository roomRepository;
    private final QRCodeGenerator qrCodeGenerator;

    /**
     * 결제 완료 후 예약 데이터 생성( 트랜잭션 보장해줘야함)
     * @param request - 프론트에서 받은 결제요청 DTO
     * @param  orderIdx - 결제테이블의 PK
     * */
    @Transactional
    public RoomReservation insertRoomReservation(PaymentRequestDto request, Integer orderIdx) {
        //객실 존재의 여부 확인(FK무결성 보장)
        RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(
                String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d, contentId=%s",
                        request.getRoomId(), request.getContentId())
        ));

        //RoomReservation 엔티티 생성
        RoomReservation reservation = RoomReservation.builder()
                .customerIdx(request.getCustomerIdx())
                .roomIdx(request.getRoomId())
                .contentid(request.getContentId())
                .orderIdx(orderIdx)
                .status(1)//예약 확정
                .checkinDate(request.getCheckIn() != null ? LocalDate.parse(request.getCheckIn()) : null)
                .checkoutDate(request.getCheckOut() != null ? LocalDate.parse(request.getCheckOut()) : null)
                .guest(request.getGuests())
                .totalPrice(request.getTotalPrice())
                .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderNum(request.getOrderId())
                .specialRequest(request.getSpecialRequests())
                .build();

        //DB저장및 반환
        return roomReservationRepository.save(reservation);
    }
}
