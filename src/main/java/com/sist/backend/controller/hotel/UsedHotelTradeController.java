package com.sist.backend.controller.hotel;

import com.sist.backend.entity.UsedPay;
import com.sist.backend.entity.UsedTrade;
import com.sist.backend.service.hotel.UsedHotelTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 중고 호텔 거래 동시성 제어 API
 */
@Slf4j
@RestController
@RequestMapping("/api/used-hotels")
@RequiredArgsConstructor
public class UsedHotelTradeController {

    private final UsedHotelTradeService tradeService;

    /**
     * 중고 아이템 거래 가능 여부 체크
     */
    @GetMapping("/{usedItemIdx}/availability")
    public ResponseEntity<?> checkAvailability(@PathVariable Integer usedItemIdx) {
        try {
            boolean isAvailable = tradeService.isUsedItemAvailable(usedItemIdx);
            
            return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "message", isAvailable ? "거래 가능합니다." : "이미 거래된 아이템입니다."
            ));

        } catch (Exception e) {
            log.error("거래 가능성 체크 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 가능성 체크 중 오류가 발생했습니다."));
        }
    }

    /**
     * 중고 호텔 거래 생성 (결제 전)
     */
    @PostMapping("/trade")
    public ResponseEntity<?> createTrade(@RequestBody Map<String, Object> request) {
        try {
            Integer usedItemIdx = (Integer) request.get("usedItemIdx");
            Integer buyerIdx = (Integer) request.get("buyerIdx");
            Integer sellerIdx = (Integer) request.get("sellerIdx");
            Integer price = (Integer) request.get("price");
            Integer reservIdx = (Integer) request.get("reservIdx");

            // 거래 생성 (트랜잭션 내에서 동시성 제어)
            UsedTrade createdTrade = tradeService.createUsedTrade(
                usedItemIdx, buyerIdx, sellerIdx, price, reservIdx);
            
            if (createdTrade == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "이미 다른 고객이 거래한 아이템입니다."));
            }

            return ResponseEntity.ok(Map.of(
                "message", "거래가 생성되었습니다.",
                "usedTradeIdx", createdTrade.getUsedTradeIdx(),
                "status", createdTrade.getStstus()
            ));

        } catch (Exception e) {
            log.error("거래 생성 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 거래 확정 (결제 완료 후)
     */
    @PostMapping("/trade/{usedTradeIdx}/confirm")
    public ResponseEntity<?> confirmTrade(@PathVariable Integer usedTradeIdx) {
        try {
            UsedTrade confirmedTrade = tradeService.confirmTrade(usedTradeIdx);
            
            return ResponseEntity.ok(Map.of(
                "message", "거래가 확정되었습니다.",
                "usedTradeIdx", confirmedTrade.getUsedTradeIdx(),
                "status", confirmedTrade.getStstus()
            ));

        } catch (Exception e) {
            log.error("거래 확정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 거래 취소
     */
    @PostMapping("/trade/{usedTradeIdx}/cancel")
    public ResponseEntity<?> cancelTrade(
            @PathVariable Integer usedTradeIdx,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String cancelReason = request != null ? request.get("reason") : "사용자 취소";
            tradeService.cancelTrade(usedTradeIdx, cancelReason);
            
            return ResponseEntity.ok(Map.of(
                "message", "거래가 취소되었습니다.",
                "usedTradeIdx", usedTradeIdx
            ));

        } catch (Exception e) {
            log.error("거래 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 거래 상태 조회
     */
    @GetMapping("/trade/{usedTradeIdx}/status")
    public ResponseEntity<?> getTradeStatus(@PathVariable Integer usedTradeIdx) {
        try {
            Integer status = tradeService.getTradeStatus(usedTradeIdx);
            
            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            String statusText = switch (status) {
                case 0 -> "거래 중";
                case 1 -> "거래 완료";
                case 2 -> "거래 취소";
                default -> "알 수 없음";
            };

            return ResponseEntity.ok(Map.of(
                "usedTradeIdx", usedTradeIdx,
                "status", status,
                "statusText", statusText
            ));

        } catch (Exception e) {
            log.error("거래 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 상태 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구매자 거래 목록 조회
     */
    @GetMapping("/buyer/{buyerIdx}/trades")
    public ResponseEntity<?> getBuyerTrades(@PathVariable Integer buyerIdx) {
        try {
            List<UsedTrade> trades = tradeService.getBuyerTrades(buyerIdx);
            
            return ResponseEntity.ok(Map.of(
                "buyerIdx", buyerIdx,
                "trades", trades,
                "count", trades.size()
            ));

        } catch (Exception e) {
            log.error("구매자 거래 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 판매자 거래 목록 조회
     */
    @GetMapping("/seller/{sellerIdx}/trades")
    public ResponseEntity<?> getSellerTrades(@PathVariable Integer sellerIdx) {
        try {
            List<UsedTrade> trades = tradeService.getSellerTrades(sellerIdx);
            
            return ResponseEntity.ok(Map.of(
                "sellerIdx", sellerIdx,
                "trades", trades,
                "count", trades.size()
            ));

        } catch (Exception e) {
            log.error("판매자 거래 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 결제 내역 저장
     */
    @PostMapping("/payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> request) {
        try {
            Integer usedTradeIdx = parseInteger(request.get("usedTradeIdx"));
            String paymentKey = (String) request.get("paymentKey");
            String orderId = (String) request.get("orderId");
            Integer totalAmount = parseInteger(request.get("totalAmount"));
            Integer cashAmount = parseInteger(request.get("cashAmount"));
            Integer pointAmount = parseInteger(request.get("pointAmount"));
            Integer cardAmount = parseInteger(request.get("cardAmount"));
            String paymentMethod = (String) request.get("paymentMethod");
            String receiptUrl = (String) request.get("receiptUrl");
            String qrUrl = (String) request.get("qrUrl");
            String approvedAt = (String) request.get("approvedAt");

            Map<String, Object> paymentData = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "totalAmount", totalAmount,
                "cashAmount", cashAmount,
                "pointAmount", pointAmount,
                "cardAmount", cardAmount,
                "paymentMethod", paymentMethod,
                "receiptUrl", receiptUrl,
                "qrUrl", qrUrl,
                "approvedAt", approvedAt
            );

            UsedPay savedPayment = tradeService.createPayment(usedTradeIdx, paymentData);
            
            return ResponseEntity.ok(Map.of(
                "message", "결제 내역이 저장되었습니다.",
                "usedPayIdx", savedPayment.getUsedPayIdx(),
                "usedTradeIdx", savedPayment.getUsedTradeIdx()
            ));

        } catch (Exception e) {
            log.error("결제 내역 저장 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "결제 내역 저장 중 오류가 발생했습니다."));
        }
    }

    // 안전한 Integer 파싱 헬퍼 메서드
    private Integer parseInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
