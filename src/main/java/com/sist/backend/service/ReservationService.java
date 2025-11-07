package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sist.backend.repository.hotel.HotelInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.entity.Room;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    //호텔 예약 db삽입을 담당하는 핵심 비즈니스 로직임
    //결제 완료 -> QR코드 생성 -> DB저장까지 한번에 처리
    //트랜잭션 성공시 DB삽입 실패하면 롤백됨
    private final RoomReservationRepository roomReservationRepository;
    private final RoomRepository roomRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final HotelInfoRepository hotelInfoRepository;

    /**
     * 결제 완료 후 예약 데이터 생성( 트랜잭션 보장해줘야함)
     *
     * @param request - 프론트에서 받은 결제요청 DTO
     * @param orderIdx - 결제테이블의 PK
     *
     */
    @Transactional
    public RoomReservation insertRoomReservation(PaymentRequestDto request, Integer orderIdx) {
        //객실 존재의 여부 확인(FK무결성 보장)
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException(
                String.format("객실 정보를 찾을 수 없습니다: roomIdx=%d", request.getRoomId())
        ));
        
        // contentId 일치 여부 확인 (보안 검증)
        if (!room.getContentId().equals(request.getContentId())) {
            throw new RuntimeException(
                String.format("객실의 호텔 정보가 일치하지 않습니다: roomIdx=%d, roomContentId=%s, requestContentId=%s",
                    request.getRoomId(), room.getContentId(), request.getContentId())
            );
        }

        // 디버깅: specialRequest 길이와 앞부분 로깅
        String sr = request.getSpecialRequests();
        sr = sr != null ? sr.trim() : "";
        int srLen = sr.length();
        String srPreview = srLen > 0 ? sr.substring(0, Math.min(50, srLen)) : "";
        log.info("[RESERVATION] specialRequests len={}, preview={}", srLen, srPreview);

        // 실 결제 금액: amount와 동일하게 저장 (요청에 따라 totalPrice == amount)
        int realPrice = request.getAmount() != null ? request.getAmount() : 0;
        log.info("[RESERVATION] totalPrice(real) from amount: realPrice={}", realPrice);

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
                .totalPrice(realPrice)
                .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderNum(request.getOrderId())
                .specialRequest(sr)
                .build();

        //DB저장및 반환
        RoomReservation savedReservation = roomReservationRepository.save(reservation);
        // 호텔 예약 수 증가
        hotelInfoRepository.increaseReservationCount(request.getContentId());

        // 저장된 예약 정보 반환
        return savedReservation;
    }
}
