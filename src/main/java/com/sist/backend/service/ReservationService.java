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

    private final RoomReservationRepository roomReservationRepository;
    private final RoomRepository roomRepository;
    private final QRCodeGenerator qrCodeGenerator;

    @Transactional
    public RoomReservation insertRoomReservation(PaymentRequestDto request, Integer orderIdx) {
        RoomId roomId = new RoomId(request.getRoomId(), request.getContentId());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(
                String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d, contentId=%s",
                        request.getRoomId(), request.getContentId())
        ));

        RoomReservation reservation = RoomReservation.builder()
                .customerIdx(request.getCustomerIdx())
                .roomIdx(request.getRoomId())
                .contentid(request.getContentId())
                .orderIdx(orderIdx)
                .status(1)
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

        return roomReservationRepository.save(reservation);
    }
}
