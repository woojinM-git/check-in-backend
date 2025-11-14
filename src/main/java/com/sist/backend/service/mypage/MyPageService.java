package com.sist.backend.service.mypage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.mypage.DiningReservationResponseDTO;
import com.sist.backend.dto.mypage.ReservationResponseDTO;
import com.sist.backend.dto.mypage.WritableReviewDTO;
import com.sist.backend.entity.DiningReservation;
import com.sist.backend.entity.DiningCancelLog;
import com.sist.backend.entity.DiningPayment;
import com.sist.backend.entity.HotelCancelLog;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.DiningReservationRepository;
import com.sist.backend.repository.HotelCancelLogRepository;
import com.sist.backend.repository.ReviewRepository;
import com.sist.backend.repository.DiningCancelLogRepository;
import com.sist.backend.repository.DiningPaymentRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.UsedTradeRepository;
import com.sist.backend.repository.UsedItemRepository;
import com.sist.backend.entity.UsedItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final RoomReservationRepository roomReservationRepository;
    private final DiningReservationRepository diningReservationRepository;
    private final HotelCancelLogRepository hotelCancelLogRepository;
    private final RoomPaymentRepository roomPaymentRepository;
    private final DiningCancelLogRepository diningCancelLogRepository;
    private final DiningPaymentRepository diningPaymentRepository;
    private final ReviewRepository reviewRepository;
    private final UsedTradeRepository usedTradeRepository;
    private final UsedItemRepository usedItemRepository;

    // 날짜 포맷터 (YYYY.MM.DD)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private List<Integer> mapStatusToCodes(String status) {
        switch (status.toLowerCase()) {
            case "upcoming":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{1}); // List<Integer>로 변환됩니다.

            case "completed":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{4});

            case "cancelled":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{2, 3});

            case "used":
                // 중고거래: 모든 상태의 예약 포함 (UsedItem이 있는 예약만 조회)
                return Arrays.asList(new Integer[]{0, 1, 2, 3, 4});

            default:
                return Arrays.asList(new Integer[]{0, 1, 2, 3, 4});
        }
    }

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회 (DTO 반환) - 페이지네이션 미지원 버전 (하위 호환성 유지) */
    public List<ReservationResponseDTO> getMyReservationsByStatus(Integer customerIdx, String status) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. 일반 예약 조회 (customerIdx 기준)
        List<RoomReservation> normalReservations = roomReservationRepository
                .findByReservationsByCustomerAndStatus(customerIdx, statusCodes);

        // 3. 판매완료된 예약 조회 (UsedTrade의 sellerIdx 기준) - customerIdx가 변경되었어도 조회
        List<RoomReservation> soldReservations = roomReservationRepository
                .findSoldReservationsBySellerIdx(customerIdx, statusCodes);

        // 4. UsedItem의 sellerIdx 기준으로 예약 조회 (판매중/거래중인 예약 포함)
        List<RoomReservation> usedItemReservations = roomReservationRepository
                .findReservationsByUsedItemSellerIdx(customerIdx, statusCodes);

        // 5. 세 결과를 합치고 중복 제거 (reservIdx 기준)
        List<RoomReservation> allReservations = new java.util.ArrayList<>(normalReservations);
        
        // 판매완료된 예약 추가
        for (RoomReservation soldReservation : soldReservations) {
            boolean isDuplicate = allReservations.stream()
                    .anyMatch(r -> r.getReservIdx().equals(soldReservation.getReservIdx()));
            if (!isDuplicate) {
                allReservations.add(soldReservation);
            }
        }
        
        // UsedItem의 sellerIdx 기준 예약 추가
        for (RoomReservation usedItemReservation : usedItemReservations) {
            boolean isDuplicate = allReservations.stream()
                    .anyMatch(r -> r.getReservIdx().equals(usedItemReservation.getReservIdx()));
            if (!isDuplicate) {
                allReservations.add(usedItemReservation);
            }
        }

        // 5. 체크인 날짜 기준으로 정렬
        allReservations.sort((r1, r2) -> {
            if (r1.getCheckinDate() == null && r2.getCheckinDate() == null) return 0;
            if (r1.getCheckinDate() == null) return 1;
            if (r2.getCheckinDate() == null) return -1;
            return r2.getCheckinDate().compareTo(r1.getCheckinDate()); // 내림차순
        });

        // 6. Entity → DTO 변환
        return allReservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회 (DTO 반환) - 페이지네이션 지원 버전
     * @param customerIdx 고객 ID (로그인 사용자)
     * @param status 프론트엔드 상태 문자열 (upcoming, completed, cancelled)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return Page<ReservationResponseDTO>
     */
    public Page<ReservationResponseDTO> getMyReservationsByStatus(Integer customerIdx, String status, int page, int size) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 중고거래 탭인 경우: UsedItem의 sellerIdx 기준으로만 조회
        if ("used".equals(status.toLowerCase())) {
            // UsedItem의 sellerIdx 기준으로 예약 조회 (판매중/거래중/판매완료 포함)
            List<RoomReservation> usedItemReservations = roomReservationRepository
                    .findReservationsByUsedItemSellerIdx(customerIdx, statusCodes);

            // 체크인 날짜 기준으로 정렬
            usedItemReservations.sort((r1, r2) -> {
                if (r1.getCheckinDate() == null && r2.getCheckinDate() == null) return 0;
                if (r1.getCheckinDate() == null) return 1;
                if (r2.getCheckinDate() == null) return -1;
                return r2.getCheckinDate().compareTo(r1.getCheckinDate()); // 내림차순
            });

            // 페이지네이션 적용
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), usedItemReservations.size());
            List<RoomReservation> pagedReservations = start < usedItemReservations.size() 
                    ? usedItemReservations.subList(start, end)
                    : new java.util.ArrayList<>();

            // Entity → DTO 변환
            List<ReservationResponseDTO> dtoList = pagedReservations.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Page 객체 생성
            return new org.springframework.data.domain.PageImpl<>(
                    dtoList,
                    pageable,
                    usedItemReservations.size()
            );
        }

        // 일반 탭 (upcoming, completed, cancelled): 기존 로직
        // 2. 일반 예약 조회 (customerIdx 기준)
        Pageable pageable = PageRequest.of(page, size);
        Page<RoomReservation> normalReservationsPage = roomReservationRepository
                .findByReservationsByCustomerAndStatusWithPagination(customerIdx, statusCodes, pageable);

        // 3. 판매완료된 예약 조회 (UsedTrade의 sellerIdx 기준) - customerIdx가 변경되었어도 조회
        List<RoomReservation> soldReservations = roomReservationRepository
                .findSoldReservationsBySellerIdx(customerIdx, statusCodes);

        // 4. UsedItem의 sellerIdx 기준으로 예약 조회 (판매중/거래중인 예약 포함)
        List<RoomReservation> usedItemReservations = roomReservationRepository
                .findReservationsByUsedItemSellerIdx(customerIdx, statusCodes);

        // 5. 세 결과를 합치고 중복 제거 (reservIdx 기준)
        List<RoomReservation> allReservations = new java.util.ArrayList<>(normalReservationsPage.getContent());
        
        // 판매완료된 예약 추가
        for (RoomReservation soldReservation : soldReservations) {
            boolean isDuplicate = allReservations.stream()
                    .anyMatch(r -> r.getReservIdx().equals(soldReservation.getReservIdx()));
            if (!isDuplicate) {
                allReservations.add(soldReservation);
            }
        }
        
        // UsedItem의 sellerIdx 기준 예약 추가
        for (RoomReservation usedItemReservation : usedItemReservations) {
            boolean isDuplicate = allReservations.stream()
                    .anyMatch(r -> r.getReservIdx().equals(usedItemReservation.getReservIdx()));
            if (!isDuplicate) {
                allReservations.add(usedItemReservation);
            }
        }

        // 5. 체크인 날짜 기준으로 정렬
        allReservations.sort((r1, r2) -> {
            if (r1.getCheckinDate() == null && r2.getCheckinDate() == null) return 0;
            if (r1.getCheckinDate() == null) return 1;
            if (r2.getCheckinDate() == null) return -1;
            return r2.getCheckinDate().compareTo(r1.getCheckinDate()); // 내림차순
        });

        // 6. 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allReservations.size());
        List<RoomReservation> pagedReservations = allReservations.subList(start, end);

        // 7. Entity → DTO 변환
        List<ReservationResponseDTO> dtoList = pagedReservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 8. Page 객체 생성
        return new org.springframework.data.domain.PageImpl<>(
                dtoList,
                pageable,
                allReservations.size()
        );
    }

    /**
     * RoomReservation Entity를 ReservationResponseDTO로 변환
     */
    private ReservationResponseDTO convertToDTO(RoomReservation reservation) {
        RoomPayment roomPayment = reservation.getRoomPayment();
        if (roomPayment == null && reservation.getOrderIdx() != null) {
            roomPayment = roomPaymentRepository.findByOrderIdx(reservation.getOrderIdx()).orElse(null);
        }

        int totalAmount = toSafeInt(reservation.getTotalPrice());
        int cashUsed = 0;
        int pointsUsed = 0;

        if (roomPayment != null) {
            int price = toSafeInt(roomPayment.getPrice());
            pointsUsed = toSafeInt(roomPayment.getPointsUsed());
            cashUsed = toSafeInt(roomPayment.getCashUsed());
            totalAmount = price + pointsUsed + cashUsed;
        }

        Integer refundAmount = null;
        Integer refundCash = null;
        Integer refundPoint = null;
        Integer status = reservation.getStatus();

        if (status != null && (status == 2 || status == 3)) {
            HotelCancelLog cancelLog = hotelCancelLogRepository
                    .findTopByReservIdxOrderByCancelAtDesc(reservation.getReservIdx())
                    .orElse(null);

            if (cancelLog != null) {
                refundAmount = toSafeInt(cancelLog.getRefundTotalAmount());
                refundCash = toSafeInt(cancelLog.getRefundCash());
                refundPoint = toSafeInt(cancelLog.getRefundPoint());
            } else {
                refundAmount = 0;
                refundCash = 0;
                refundPoint = 0;
            }
        }

        // UsedItem 정보 조회 (중고거래 탭용)
        // 하나의 reservIdx에 여러 UsedItem이 있을 수 있으므로, 최신 항목만 사용
        List<UsedItem> usedItems = usedItemRepository.findByReservIdx(reservation.getReservIdx());
        UsedItem usedItem = usedItems != null && !usedItems.isEmpty() ? usedItems.get(0) : null;
        
        ReservationResponseDTO.ReservationResponseDTOBuilder builder = ReservationResponseDTO.builder()
                // 기본 예약 정보
                .id(reservation.getReservIdx())
                .reservationNumber(reservation.getOrderNum() != null && !reservation.getOrderNum().isEmpty()
                        ? reservation.getOrderNum()
                        : "R" + reservation.getReservIdx()) // orderNum이 없으면 기존 방식 사용

                // 호텔 정보 (Room -> HotelInfo)
                .hotelName(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        ? reservation.getRoom().getHotelInfo().getTitle()
                        : "호텔명 없음")
                .location(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        && reservation.getRoom().getHotelInfo().getArea() != null
                        ? reservation.getRoom().getHotelInfo().getArea().getAreaName()
                        : reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        ? reservation.getRoom().getHotelInfo().getAdress()
                        : "위치 정보 없음")
                .contentId(reservation.getContentid())
                // 객실 정보
                .roomType(reservation.getRoom() != null
                        ? reservation.getRoom().getName()
                        : "객실 정보 없음")
                .roomIdx(reservation.getRoomIdx())
                // 예약 상세 정보
                .checkIn(reservation.getCheckinDate() != null
                        ? reservation.getCheckinDate().format(DATE_FORMATTER)
                        : "")
                .checkOut(reservation.getCheckoutDate() != null
                        ? reservation.getCheckoutDate().format(DATE_FORMATTER)
                        : "")
                .guest(reservation.getGuest())
                .totalprice(totalAmount)
                .cashUsed(cashUsed)
                .pointsUsed(pointsUsed)
                // 예약 상태
                .status(mapStatusCodeToString(reservation.getStatus()))
                .statusCode(reservation.getStatus())
                // 환불 정보 (취소 시 hotelCancelLog 기반)
                .refundAmount(refundAmount)
                .refundCash(refundCash)
                .refundPoint(refundPoint)
                // 생성/수정 시간
                .createdAt(reservation.getCreatedAt() != null
                        ? reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                        : "")
                .updatedAt(reservation.getUpdatedAt() != null
                        ? reservation.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                        : "")
                // QR 코드 URL
                .qrUrl(reservation.getQrUrl());

        // UsedItem 정보 추가
        if (usedItem != null) {
            builder.usedItemIdx(usedItem.getUsedItemIdx())
                   .usedItemStatus(usedItem.getStatus())
                   .usedItemPrice(usedItem.getPrice());
        }
        
        return builder.build();
    }

    private int toSafeInt(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * 예약 상세 정보 조회
     *
     * @param reservationId 예약 ID
     * @param customerIdx 고객 ID (권한 검증용)
     * @return ReservationResponseDTO
     */
    public ReservationResponseDTO getReservationDetail(Integer reservationId, Integer customerIdx) {
        // 1. 예약 정보 조회
        RoomReservation reservation = roomReservationRepository.findById(reservationId)
                .orElse(null);

        if (reservation == null) {
            System.out.println("❌ 예약 상세 조회 실패: 예약이 존재하지 않음 - reservationId=" + reservationId);
            return null;
        }

        System.out.println("✅ 예약 조회 성공 - reservationId=" + reservationId + ", 현재 customerIdx=" + reservation.getCustomerIdx() + ", 요청한 customerIdx=" + customerIdx);

        // 2. 권한 검증: customerIdx가 일치하거나, 판매완료된 예약의 판매자이거나, UsedItem의 sellerIdx인 경우 허용
        boolean hasAccess = false;
        
        // 일반 예약: customerIdx가 일치하는 경우
        if (reservation.getCustomerIdx().equals(customerIdx)) {
            hasAccess = true;
            System.out.println("✅ 권한 확인: 일반 예약 (customerIdx 일치)");
        } else {
            // 판매완료된 예약: UsedTrade의 sellerIdx가 customerIdx와 일치하고 reservIdx가 일치하는 경우
            System.out.println("🔍 판매완료된 예약 확인 중 - reservationId=" + reservationId + ", sellerIdx=" + customerIdx);
            java.util.Optional<com.sist.backend.entity.UsedTrade> trade = 
                usedTradeRepository.findByReservIdxAndSellerIdx(reservationId, customerIdx);
            if (trade.isPresent()) {
                hasAccess = true;
                System.out.println("✅ 권한 확인: 판매완료된 예약의 판매자 (UsedTrade)");
            } else {
                // UsedItem의 sellerIdx가 customerIdx와 일치하는 경우
                System.out.println("🔍 UsedItem의 sellerIdx 확인 중 - reservationId=" + reservationId + ", sellerIdx=" + customerIdx);
                boolean isUsedItemSeller = roomReservationRepository
                    .existsUsedItemByReservIdxAndSellerIdx(reservationId, customerIdx);
                if (isUsedItemSeller) {
                    hasAccess = true;
                    System.out.println("✅ 권한 확인: UsedItem의 판매자");
                } else {
                    System.out.println("❌ 권한 없음: 판매자 아님");
                }
            }
        }

        if (!hasAccess) {
            System.out.println("❌ 예약 상세 조회 실패: 권한 없음 - reservationId=" + reservationId + ", customerIdx=" + customerIdx);
            return null;
        }

        // 3. Entity → DTO 변환
        System.out.println("✅ 예약 상세 조회 성공 - reservationId=" + reservationId);
        return convertToDTO(reservation);
    }

    /**
     * 다이닝 예약 상세 정보 조회
     *
     * @param reservationId 다이닝 예약 ID (diningResrIdx)
     * @param customerIdx 고객 ID (권한 검증용)
     * @return DiningReservationResponseDTO
     */
    public DiningReservationResponseDTO getDiningReservationDetail(Integer reservationId, Integer customerIdx) {
        // 1. 예약 정보 조회 (권한 검증 포함)
        DiningReservation reservation = diningReservationRepository.findById(reservationId)
                .orElse(null);

        // 2. 예약이 없거나 다른 고객의 예약인 경우 null 반환
        if (reservation == null || !reservation.getCustomerIdx().equals(customerIdx)) {
            return null;
        }

        // 3. Entity → DTO 변환
        return convertDiningToDTO(reservation);
    }

    /**
     * 상태 코드를 한글 문자열로 변환
     */
    private String mapStatusCodeToString(Integer statusCode) {
        if (statusCode == null) {
            return "알 수 없음";
        }

        switch (statusCode) {
            case 0:
                return "예약대기";
            case 1:
                return "예약확정";
            case 2:
                return "취소완료";
            case 3:
                return "노쇼";
            case 4:
                return "이용완료";
            default:
                return "알 수 없음";
        }
    }

    /**
     * 작성 가능한 리뷰 목록 조회 - status = 4 (이용완료)인 예약만 조회 - 이미 리뷰를 작성한 예약은 제외
     */
    public List<WritableReviewDTO> getWritableReviews(Integer customerIdx) {
        // 1. 이용완료된 예약 조회 (status = 4)
        List<RoomReservation> completedReservations
                = roomReservationRepository.findByCustomerIdxAndStatus(customerIdx, 4);

        // 2. 이미 리뷰를 작성한 예약 ID 목록 조회
        List<Integer> reviewedReservationIds
                = reviewRepository.findReservationIdsByCustomerIdx(customerIdx);

        // 3. 리뷰를 작성하지 않은 예약만 필터링
        List<RoomReservation> writableReservations = completedReservations.stream()
                .filter(r -> !reviewedReservationIds.contains(r.getReservIdx()))
                .collect(Collectors.toList());

        // 4. DTO 변환
        return writableReservations.stream()
                .map(this::convertToWritableReviewDTO)
                .collect(Collectors.toList());
    }

    /**
     * RoomReservation을 WritableReviewDTO로 변환
     */
    private WritableReviewDTO convertToWritableReviewDTO(RoomReservation reservation) {
        // 체크아웃 날짜로부터 남은 일수 계산
        LocalDate checkOut = reservation.getCheckoutDate();
        long daysLeft = checkOut != null
                ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), checkOut)
                : 0;

        return WritableReviewDTO.builder()
                .reservationIdx(reservation.getReservIdx())
                .hotelName(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        ? reservation.getRoom().getHotelInfo().getTitle()
                        : "호텔명 없음")
                .location(reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        && reservation.getRoom().getHotelInfo().getArea() != null
                        ? reservation.getRoom().getHotelInfo().getArea().getAreaName()
                        : reservation.getRoom() != null && reservation.getRoom().getHotelInfo() != null
                        ? reservation.getRoom().getHotelInfo().getAdress()
                        : "위치 정보 없음")
                .contentId(reservation.getContentid())
                .roomIdx(reservation.getRoomIdx())
                .roomType(reservation.getRoom() != null
                        ? reservation.getRoom().getName()
                        : "객실 정보 없음")
                .checkOutDate(checkOut != null ? checkOut.format(DATE_FORMATTER) : "")
                .daysLeft(daysLeft)
                .build();
    }

    /* 다이닝 예약 내역 조회 (페이지네이션 지원) */
    public Page<DiningReservationResponseDTO> getMyDiningReservationsByStatus(Integer customerIdx, String status, int page, int size) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // 3. Repository에서 페이지네이션된 데이터 조회 (Dining 정보 포함)
        Page<DiningReservation> reservationsPage = diningReservationRepository
                .findByCustomerIdxAndStatusWithPagination(customerIdx, statusCodes, pageable);

        // 4. Entity → DTO 변환 (Page 객체 유지)
        return reservationsPage.map(this::convertDiningToDTO);
    }

    /**
     * DiningReservation Entity를 DiningReservationResponseDTO로 변환
     */
    private DiningReservationResponseDTO convertDiningToDTO(DiningReservation reservation) {
        DiningPayment diningPayment = reservation.getDiningPayment();
        if (diningPayment == null && reservation.getDiningpayIdx() != null) {
            diningPayment = diningPaymentRepository.findById(reservation.getDiningpayIdx()).orElse(null);
        }

        int totalAmount = toSafeInt(reservation.getTotalPrice());
        int cashUsed = 0;
        int pointsUsed = 0;

        if (diningPayment != null) {
            cashUsed = toSafeInt(diningPayment.getPrice());
            pointsUsed = toSafeInt(diningPayment.getPointUsed());
        }

        Integer refundAmount = null;
        Integer refundCash = null;
        Integer refundPoint = null;
        Integer status = reservation.getStatus();

        if (status != null && (status == 2 || status == 3)) {
            DiningCancelLog cancelLog = diningCancelLogRepository
                    .findTopByDiningResrIdxOrderByCancelAtDesc(reservation.getDiningResrIdx())
                    .orElse(null);

            if (cancelLog != null) {
                refundAmount = toSafeInt(cancelLog.getRefundTotalAmount());
                refundCash = toSafeInt(cancelLog.getRefundCash());
                refundPoint = toSafeInt(cancelLog.getRefundPoint());

                // 취소 상태에서는 결제 금액도 취소 로그 기준으로 노출
                cashUsed = refundCash;
                pointsUsed = refundPoint;
            } else {
                refundAmount = 0;
                refundCash = 0;
                refundPoint = 0;
            }
        }

        return DiningReservationResponseDTO.builder()
                // 기본 예약 정보
                .id(reservation.getDiningResrIdx())
                .reservationNumber("D" + reservation.getDiningResrIdx())
                // 다이닝 정보 (Dining -> HotelInfo)
                .diningName(reservation.getDining() != null
                        ? reservation.getDining().getName()
                        : "다이닝 정보 없음")
                .hotelName(reservation.getDining() != null
                        && reservation.getDining().getHotelInfo() != null
                        ? reservation.getDining().getHotelInfo().getTitle()
                        : "호텔명 없음")
                .location(reservation.getDining() != null
                        && reservation.getDining().getHotelInfo() != null
                        && reservation.getDining().getHotelInfo().getArea() != null
                        ? reservation.getDining().getHotelInfo().getArea().getAreaName()
                        : reservation.getDining() != null
                        && reservation.getDining().getHotelInfo() != null
                        ? reservation.getDining().getHotelInfo().getAdress()
                        : "위치 정보 없음")
                .contentId(reservation.getDining() != null
                        ? reservation.getDining().getContentid()
                        : null)
                .diningIdx(reservation.getDiningIdx())
                // 예약 상세 정보
                .reservationDate(reservation.getReservationDate() != null
                        ? reservation.getReservationDate().format(DATE_FORMATTER)
                        : "")
                .reservationTime(reservation.getReservationTime() != null
                        ? reservation.getReservationTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        : "")
                .guest(reservation.getGuest())
                .totalPrice(totalAmount)
                .cashUsed(cashUsed)
                .pointsUsed(pointsUsed)
                // 예약 상태
                .status(mapStatusCodeToString(reservation.getStatus()))
                .statusCode(reservation.getStatus())
                // 환불 정보
                .refundAmount(refundAmount)
                .refundCash(refundCash)
                .refundPoint(refundPoint)
                // 생성/수정 시간
                .createdAt(reservation.getCreatedAt() != null
                        ? reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                        : "")
                .updatedAt(reservation.getUpdatedAt() != null
                        ? reservation.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                        : "")
                // 예약 타입 구분
                .type("dining")
                .build();
    }
}
