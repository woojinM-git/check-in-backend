package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.dto.PaymentResponseDto;
import com.sist.backend.entity.Coupon;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.Dining;
import com.sist.backend.entity.DiningPayment;
import com.sist.backend.entity.DiningReservation;
import com.sist.backend.entity.PointLedger;
import com.sist.backend.entity.RoomPayment;
import com.sist.backend.repository.CouponRepository;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.DiningPaymentRepository;
import com.sist.backend.repository.DiningRepository;
import com.sist.backend.repository.DiningReservationRepository;
import com.sist.backend.repository.EmailLogRepository;
import com.sist.backend.repository.PointLedgerRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RoomPaymentRepository roomPaymentRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final ReservationService reservationService;
    private final DiningPaymentRepository diningPaymentRepository;
    private final DiningReservationRepository diningReservationRepository;
    private final DiningRepository diningRepository;
    private final TossPaymentsService tossPaymentsService;
    private final MailService mailService;
    private final QRCodeGenerator qrCodeGenerator;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final ReservationLockService reservationLockService;
    private final PointLedgerRepository pointLedgerRepository;
    private final CouponRepository couponRepository;
    private final EmailLogRepository emailLogRepository;

    // 등급별 적립률 (%)
    private static final Map<String, Double> RANK_REWARD_RATE = Map.of(
            "Traveler", 0.01,
            "Explorer", 0.02,
            "VIP", 0.03,
            "First Class", 0.04,
            "Sky Suite", 0.05
    );

    // 등급 업그레이드 기준 금액
    private static final Map<String, Integer> RANK_THRESHOLDS = Map.of(
            "Traveler", 0,
            "Explorer", 200000,
            "VIP", 800000,
            "First Class", 2000000,
            "Sky Suite", 5000000
    );

    /**
     * 특별 요청사항 바이트 길이 검증 (utf8mb4 기준)
     */
    private void validateSpecialRequest(String specialRequest) {
        if (specialRequest == null || specialRequest.isEmpty()) {
            return;
        }

        try {
            int byteLength = specialRequest.getBytes("UTF-8").length;
            if (byteLength > 1000) {
                throw new IllegalArgumentException(
                        String.format("특별 요청사항은 최대 1000바이트까지 입력 가능합니다. (현재: %d바이트)", byteLength)
                );
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("특별 요청사항 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 캐시/포인트 90% 제한 검증
     */
    private void validatePaymentLimits(PaymentRequestDto request) {
        int totalAmount = request.getTotalPrice() != null ? request.getTotalPrice() : request.getAmount();
        int cashUsed = request.getCashUsed() != null ? request.getCashUsed() : 0;
        int pointsUsed = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
        int couponDiscount = request.getCouponDiscount() != null ? request.getCouponDiscount() : 0;

        int totalDiscount = cashUsed + pointsUsed + couponDiscount;
        int maxAllowed = (int) Math.floor(totalAmount * 0.9);

        if (totalDiscount > maxAllowed) {
            throw new IllegalArgumentException(
                    String.format("쿠폰, 포인트, 캐시를 합쳐서 상품 금액의 90%% 이상 사용할 수 없습니다. (최대: %d원, 현재: %d원)",
                            maxAllowed, totalDiscount)
            );
        }

        // 전액 결제(카드 결제 금액 0원) 불가
        int cardAmount = totalAmount - totalDiscount;
        if (cardAmount <= 0) {
            throw new IllegalArgumentException("카드로 최소 10%는 결제해야 합니다. 포인트 및 캐시만으로 전액 결제할 수 없습니다.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto verifyAndSavePayment(PaymentRequestDto request) {
        log.info("[CONFIRM] start: type={}, orderId={}, paymentKey={}, amount={}, customerIdx={}, contentId={}, roomId={}, pointsUsed={}, cashUsed={}, couponIdx={}, couponDiscount={}, specialRequestsLen={}",
                request.getType(), request.getOrderId(), request.getPaymentKey(), request.getAmount(),
                request.getCustomerIdx(), request.getContentId(), request.getRoomId(),
                request.getPointsUsed(), request.getCashUsed(), request.getCouponIdx(), request.getCouponDiscount(),
                request.getSpecialRequests() != null ? request.getSpecialRequests().length() : 0);

        // 1단계: 사전 검증
        validateSpecialRequest(request.getSpecialRequests());
        // 프론트에서 90% 제한/전액 결제 방지 검증 수행. 백엔드에서는 더 이상 차단하지 않음.

        // 0단계: 이미 처리된 결제인지 확인 (중복 요청 방지) - 타입별로 확인
        if ("dining_reservation".equals(request.getType())) {
            Optional<DiningPayment> existingDiningOpt = diningPaymentRepository.findByPaymentKeyAndStatus(request.getPaymentKey());
            if (existingDiningOpt.isPresent()) {
                DiningPayment existing = existingDiningOpt.get();
                log.warn("이미 처리된 다이닝 결제입니다: paymentKey={}, diningpayIdx={}", request.getPaymentKey(), existing.getDiningpayIdx());
                return PaymentResponseDto.builder()
                        .success(true)
                        .message("이미 처리된 결제입니다.")
                        .orderId(request.getOrderId())
                        .paymentKey(request.getPaymentKey())
                        .amount(existing.getPrice())
                        .status("DONE")
                        .approvedAt(existing.getApprovedAt())
                        .receiptUrl(existing.getReceiptUrl())
                        .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                        .emailSent(false)
                        .build();
            }
        } else {
            Optional<RoomPayment> existingPaymentOpt = roomPaymentRepository.findByPaymentKeyAndStatus(request.getPaymentKey());
            if (existingPaymentOpt.isPresent()) {
                RoomPayment existingPayment = existingPaymentOpt.get();
                log.warn("이미 처리된 호텔 결제입니다: paymentKey={}, orderIdx={}",
                        request.getPaymentKey(), existingPayment.getOrderIdx());

                return PaymentResponseDto.builder()
                        .success(true)
                        .message("이미 처리된 결제입니다.")
                        .orderId(request.getOrderId())
                        .paymentKey(request.getPaymentKey())
                        .amount(existingPayment.getPrice())
                        .status("DONE")
                        .approvedAt(existingPayment.getApprovedAt())
                        .receiptUrl(existingPayment.getReceiptUrl())
                        .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                        .emailSent(false) // 이미 처리됨: 컨트롤러에서 이메일 재발송 금지
                        .build();
            }
        }

        try {
            // 1단계: TossPayments API로 결제 검증
            Map<String, Object> tossResponse = tossPaymentsService.confirmPayment(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount()
            );

            // 2단계: 검증 결과 확인
            String status = (String) tossResponse.get("status");
            if (!"DONE".equals(status)) {
                throw new RuntimeException("TossPayments 결제 검증 실패: status=" + status);
            }

            // 3~4단계: 타입별로 결제/예약 저장
            if ("hotel_reservation".equals(request.getType())) {
                // 3-사전검증: 이미 같은 객실/날짜로 확정된 예약이 있는지 확인 (트랜잭션 내 보장)
                if (request.getRoomId() == null || request.getContentId() == null || request.getCheckIn() == null) {
                    throw new RuntimeException("필수 파라미터 누락(roomId/contentId/checkIn)");
                }
                LocalDate checkinDate = LocalDate.parse(request.getCheckIn());
                boolean exists = roomReservationRepository.existsActiveReservation(
                        request.getRoomId(), request.getContentId(), checkinDate);
                if (exists) {
                    throw new RuntimeException("이미 다른 인원이 결제/예약을 완료한 객실입니다.");
                }
                //결제 저장
                RoomPayment savedPayment = saveRoomPayment(request);

                log.info("호텔 예약 저장 시작: contentId={}, roomId={}, specialRequestsLen={}", request.getContentId(), request.getRoomId(), request.getSpecialRequests() != null ? request.getSpecialRequests().length() : 0);
                reservationService.insertRoomReservation(request, savedPayment.getOrderIdx());
                log.info("호텔 예약 저장 완료");

                // 5단계: 쿠폰 처리 (사용 완료 상태 업데이트)
                if (request.getCouponIdx() != null && request.getCouponIdx() > 0) {
                    processCoupon(request.getCouponIdx());
                }

                // 6단계: Customer 테이블 업데이트 (캐시/포인트 차감 및 PointLedger 기록)
                updateCustomerBalanceAndRecordLedger(request, savedPayment.getOrderIdx());

                // 7단계: 적립금 지급 및 등급 업데이트
                calculateAndAddRewards(request, savedPayment.getOrderIdx());

                // 8단계: 예약 락 해제 (결제 성공 시)
                try {
                    reservationLockService.releaseLock(
                            request.getContentId(),
                            request.getRoomId(),
                            request.getCheckIn(),
                            request.getCustomerIdx()
                    );
                    log.info("예약 락 해제 완료: contentId={}, roomId={}", request.getContentId(), request.getRoomId());
                } catch (Exception e) {
                    log.warn("예약 락 해제 실패 (무시): {}", e.getMessage());
                    // 락 해제 실패는 무시 (TTL로 자동 만료됨)
                }

                log.info("호텔 결제 및 예약 저장 완료: orderIdx={}", savedPayment.getOrderIdx());

                return PaymentResponseDto.builder()
                        .success(true)
                        .message("결제가 성공적으로 완료되었습니다.")
                        .orderId(request.getOrderId())
                        .paymentKey(request.getPaymentKey())
                        .amount(request.getAmount())
                        .status("DONE")
                        .approvedAt(savedPayment.getApprovedAt())
                        .receiptUrl(savedPayment.getReceiptUrl())
                        .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                        .emailSent(true) // 신규 처리됨: 컨트롤러에서 이메일 발송 허용
                        .build();
            } else if ("dining_reservation".equals(request.getType())) {
                // 다이닝: 결제/예약 저장만 수행 (쿠폰/차감/적립/등급 업데이트 제외)
                DiningPayment savedDining = saveDiningReservation(request);

                log.info("다이닝 결제 및 예약 저장 완료: diningpayIdx={}", savedDining.getDiningpayIdx());

                return PaymentResponseDto.builder()
                        .success(true)
                        .message("결제가 성공적으로 완료되었습니다.")
                        .orderId(request.getOrderId())
                        .paymentKey(request.getPaymentKey())
                        .amount(request.getAmount())
                        .status("DONE")
                        .approvedAt(savedDining.getApprovedAt())
                        .receiptUrl(savedDining.getReceiptUrl())
                        .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                        .emailSent(true)
                        .build();
            } else {
                log.warn("예약 저장 조건 미충족 또는 알 수 없는 타입: type={}, contentId={}, roomId={}, diningIdx={}",
                        request.getType(), request.getContentId(), request.getRoomId(), request.getDiningIdx());
                throw new RuntimeException("지원하지 않는 결제 타입입니다: " + request.getType());
            }

        } catch (RuntimeException e) {
            log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}", request.getOrderId(), e);
            throw e; // 예외를 다시 던져서 롤백 보장
        } catch (Exception e) {
            log.error("결제 처리 실패 - 트랜잭션 롤백: orderId={}", request.getOrderId(), e);
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private RoomPayment saveRoomPayment(PaymentRequestDto request) {
        // 실 결제 금액 계산
        int price = request.getTotalPrice() != null ? request.getTotalPrice() : request.getAmount();
        int couponDiscount = request.getCouponDiscount() != null ? request.getCouponDiscount() : 0;
        int pointsUsed = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
        int cashUsed = request.getCashUsed() != null ? request.getCashUsed() : 0;
        int realPrice = price - couponDiscount - pointsUsed - cashUsed;

        log.info("[ROOMPAYMENT] calc: price={}, couponDiscount={}, pointsUsed={}, cashUsed={}, realPrice={}",
                price, couponDiscount, pointsUsed, cashUsed, realPrice);

        RoomPayment roomPayment = RoomPayment.builder()
                .customerIdx(request.getCustomerIdx())
                .couponIdx(request.getCouponIdx() != null ? request.getCouponIdx() : 0)
                .price(request.getAmount()) // TossPayments로 결제한 실제 금액
                .status(1) // 결제 완료
                .promotionPayIdx(0)
                .paymentKey(request.getPaymentKey())
                .pointsUsed(pointsUsed)
                .cashUsed(cashUsed)
                .method(request.getMethod() != null ? request.getMethod() : "card")
                .receiptUrl("https://api.tosspayments.com/v1/payments/" + request.getPaymentKey() + "/receipt")
                .approvedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RoomPayment savedPayment = roomPaymentRepository.save(roomPayment);
        log.info("[ROOMPAYMENT] saved: orderIdx={}, customerIdx={}, pointsUsed={}, cashUsed={}, price(card)={}, realPrice(calc)={}",
                savedPayment.getOrderIdx(), request.getCustomerIdx(), savedPayment.getPointsUsed(), savedPayment.getCashUsed(), savedPayment.getPrice(), realPrice);
        return savedPayment;
    }

    /**
     * 쿠폰 처리 (사용 완료 상태 업데이트)
     */
    private void processCoupon(Integer couponIdx) {
        try {
            Coupon coupon = couponRepository.findById(couponIdx)
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: couponIdx=" + couponIdx));

            coupon.setStatus(true); // 사용 완료
            couponRepository.save(coupon);
            log.info("쿠폰 사용 처리 완료: couponIdx={}", couponIdx);
        } catch (Exception e) {
            log.error("쿠폰 처리 실패: couponIdx={}", couponIdx, e);
            throw new RuntimeException("쿠폰 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 고객 캐시/포인트 차감 처리 - 사용 이력은 RoomPayment(pointsUsed, cashUsed)에만 저장 -
     * PointLedger에는 '적립'만 기록함
     */
    private void updateCustomerBalanceAndRecordLedger(PaymentRequestDto request, Integer orderIdx) {
        try {
            Customer customer = customerRepository.findById(request.getCustomerIdx())
                    .orElseThrow(() -> new RuntimeException("고객 정보를 찾을 수 없습니다: customerIdx=" + request.getCustomerIdx()));

            // 캐시 차감 및 기록
            int usedCash = request.getCashUsed() != null ? request.getCashUsed() : 0;
            if (usedCash > 0) {
                if (customer.getCash() == null || customer.getCash() < usedCash) {
                    throw new RuntimeException("보유 캐시가 부족합니다: 보유="
                            + (customer.getCash() != null ? customer.getCash() : 0) + ", 사용=" + usedCash);
                }
                customer.setCash(customer.getCash() - usedCash);
                log.info("캐시 차감 및 기록: customerIdx={}, 차감={}, 잔액={}",
                        request.getCustomerIdx(), usedCash, customer.getCash());
            }

            // 포인트 차감 및 기록
            int usedPoint = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
            if (usedPoint > 0) {
                if (customer.getPoint() == null || customer.getPoint() < usedPoint) {
                    throw new RuntimeException("보유 포인트가 부족합니다: 보유="
                            + (customer.getPoint() != null ? customer.getPoint() : 0) + ", 사용=" + usedPoint);
                }
                customer.setPoint(customer.getPoint() - usedPoint);
                log.info("포인트 차감 및 기록: customerIdx={}, 차감={}, 잔액={}",
                        request.getCustomerIdx(), usedPoint, customer.getPoint());
            }

            customerRepository.save(customer);
            log.info("고객 잔액 업데이트 완료: customerIdx={}", request.getCustomerIdx());
        } catch (Exception e) {
            log.error("고객 잔액 업데이트 실패", e);
            throw new RuntimeException("고객 잔액 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 적립금 계산 및 지급 + 등급 업데이트 - 실 결제 금액(realPrice)을 기준으로 적립 -
     * customer.totalPrice에는 실 결제 금액만 누적
     */
    private void calculateAndAddRewards(PaymentRequestDto request, Integer orderIdx) {
        try {
            Customer customer = customerRepository.findById(request.getCustomerIdx())
                    .orElseThrow(() -> new RuntimeException("고객 정보를 찾을 수 없습니다: customerIdx=" + request.getCustomerIdx()));

            // 실 결제 금액은 amount와 동일하게 처리 (요청에 따라 amount==totalPrice)
            int realPrice = request.getAmount() != null ? request.getAmount() : 0;
            log.info("적립 기준 금액 계산: 실결제={}", realPrice);

            // 현재 등급의 적립률 계산 (실 결제 금액 기준)
            String currentRank = customer.getRank() != null ? customer.getRank() : "Traveler";
            double rewardRate = RANK_REWARD_RATE.getOrDefault(currentRank, 0.01);
            int rewardPoints = (int) Math.floor(realPrice * rewardRate);

            // 포인트 적립
            if (rewardPoints > 0) {
                int currentPoints = customer.getPoint() != null ? customer.getPoint() : 0;
                customer.setPoint(currentPoints + rewardPoints);

                // PointLedger에 적립 기록
                PointLedger rewardLedger = PointLedger.builder()
                        .customerIdx(request.getCustomerIdx())
                        .orderIdx(orderIdx)
                        .point(rewardPoints)
                        .pointType("적립")
                        .memo("호텔 결제 적립")
                        .build();
                pointLedgerRepository.save(rewardLedger);
                log.info("포인트 적립: customerIdx={}, 적립={}, 등급={}, 적립률={}%, 실결제금액={}",
                        request.getCustomerIdx(), rewardPoints, currentRank, (int) (rewardRate * 100), realPrice);
            }

            // 누적 결제 금액 업데이트 (실 결제 금액만 누적)
            int currentTotalPrice = customer.getTotalPrice() != null ? customer.getTotalPrice() : 0;
            customer.setTotalPrice(currentTotalPrice + realPrice);

            // 등급 자동 업데이트
            String oldRank = customer.getRank();
            updateCustomerRank(customer);
            String newRank = customer.getRank();

            customerRepository.save(customer);
            log.info("적립 및 등급 업데이트 완료: customerIdx={}, 누적금액={}, 등급={}",
                    customer.getCustomerIdx(), customer.getTotalPrice(), customer.getRank());

            if (!oldRank.equals(newRank)) {
                log.info("등급 변경: {} → {}", oldRank, newRank);
            }

        } catch (Exception e) {
            log.error("적립금 지급 실패", e);
            throw new RuntimeException("적립금 지급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 고객 등급 자동 업데이트 - 누적 금액 기준으로 등급 결정
     */
    private void updateCustomerRank(Customer customer) {
        int totalPrice = customer.getTotalPrice() != null ? customer.getTotalPrice() : 0;
        String newRank = "Traveler";

        // 등급 기준: Traveler(0) < Explorer(200,000) < VIP(800,000) < First Class(2,000,000) < Sky Suite(5,000,000)
        if (totalPrice >= RANK_THRESHOLDS.get("Sky Suite")) {
            newRank = "Sky Suite";
        } else if (totalPrice >= RANK_THRESHOLDS.get("First Class")) {
            newRank = "First Class";
        } else if (totalPrice >= RANK_THRESHOLDS.get("VIP")) {
            newRank = "VIP";
        } else if (totalPrice >= RANK_THRESHOLDS.get("Explorer")) {
            newRank = "Explorer";
        }

        customer.setRank(newRank);
    }

    // 예약 저장은 ReservationService에서 별도 트랜잭션으로 처리
    /**
     * 다이닝 예약 저장
     */
    private DiningPayment saveDiningReservation(PaymentRequestDto request) {
        // Dining 존재 여부 확인
        Dining dining = diningRepository.findById(request.getDiningIdx())
                .orElseThrow(() -> new RuntimeException(
                String.format("다이닝 정보를 찾을 수 없습니다: diningIdx=%d", request.getDiningIdx())
        ));

        log.info("다이닝 정보 확인 완료: diningIdx={}, name={}", dining.getDiningIdx(), dining.getName());

        // 다이닝 결제 정보 저장
        DiningPayment diningPayment = DiningPayment.builder()
                .diningIdx(request.getDiningIdx())
                .customerIdx(request.getCustomerIdx())
                .couponIdx(0) // TODO: 쿠폰 시스템 연동
                .price(request.getAmount())
                .status(1) // 결제 완료
                .paymentKey(request.getPaymentKey())
                .pointUsed(request.getPointsUsed() != null ? request.getPointsUsed() : 0)
                .method(request.getMethod() != null ? request.getMethod() : "card")
                .receiptUrl("https://api.tosspayments.com/v1/payments/" + request.getPaymentKey() + "/receipt")
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DiningPayment savedPayment = diningPaymentRepository.save(diningPayment);
        log.info("다이닝 결제 정보 저장 완료: diningpayIdx={}", savedPayment.getDiningpayIdx());

        // 다이닝 예약 정보 저장
        DiningReservation reservation = DiningReservation.builder()
                .diningIdx(request.getDiningIdx())
                .customerIdx(request.getCustomerIdx())
                .diningpayIdx(savedPayment.getDiningpayIdx())
                .reservationDate(request.getDiningDate() != null ? LocalDate.parse(request.getDiningDate()) : null)
                .reservationTime(request.getDiningTime() != null ? java.time.LocalTime.parse(request.getDiningTime()) : null)
                .guest(request.getGuests())
                .totalPrice(request.getTotalPrice() != null ? request.getTotalPrice() : request.getAmount())
                .status(1) // 예약 확정
                .qrUrl(qrCodeGenerator.generateQRCodeUrl(request.getOrderId()))
                .specialRequest(request.getSpecialRequests())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        diningReservationRepository.save(reservation);
        log.info("다이닝 예약 정보 저장 완료: diningResrIdx={}", reservation.getDiningResrIdx());
        return savedPayment;
    }

    public PaymentResponseDto getPaymentInfo(String orderId) {
        try {
            // 주문 ID로 결제 정보 조회 (실제 구현에서는 orderId로 조회)
            // 현재는 간단히 성공 응답만 반환
            return PaymentResponseDto.builder()
                    .success(true)
                    .message("결제 정보 조회 성공")
                    .orderId(orderId)
                    .status("DONE")
                    .build();
        } catch (Exception e) {
            log.error("결제 정보 조회 실패", e);
            throw new RuntimeException("결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}
