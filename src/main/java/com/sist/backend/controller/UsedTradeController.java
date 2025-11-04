package com.sist.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sist.backend.dto.UsedItemDto;
import com.sist.backend.dto.UsedSearchRequestDto;
import com.sist.backend.service.RoomReservationService;
import com.sist.backend.service.UsedTradeService;
import com.sist.backend.service.hotel.UsedHotelTradeService;
import com.sist.backend.entity.UsedPay;
import com.sist.backend.entity.UsedTrade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/used")
@RequiredArgsConstructor
@Tag(name = "양도거래", description = "양도거래 관련 API")
public class UsedTradeController {

    private final UsedTradeService usedTradeService;
    private final UsedHotelTradeService tradeService;
    private final RoomReservationService roomReservationService;

    @GetMapping("/list")
    @Operation(summary = "양도거래 목록 조회", description = "페이징 처리된 양도거래 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<UsedItemDto>> getUsedTradeListWithDetails(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "10") 
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(usedTradeService.findAllByStatusOrderByUpdatedAtDescAsDto(pageable));
    }

    @PostMapping("/search")
    @Operation(summary = "양도거래 검색", description = "다양한 조건으로 양도거래를 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<UsedItemDto>> searchUsedItems(@RequestBody UsedSearchRequestDto searchRequest) {
        Pageable pageable = Pageable.ofSize(searchRequest.getSize()).withPage(searchRequest.getPage());
        Page<UsedItemDto> result = usedTradeService.searchByMultipleConditions(
            searchRequest.getDestination(),
            searchRequest.getCheckIn(),
            searchRequest.getCheckOut(),
            searchRequest.getAdults(),
            searchRequest.getPriceMin(),
            searchRequest.getPriceMax(),
            searchRequest.getSortBy(),
            searchRequest.getSortDirection(),
            0,
            pageable
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 예약에 대한 양도거래 등록 여부 확인
     */
    @GetMapping("/check/{reservIdx}")
    @Operation(summary = "양도거래 등록 여부 확인", description = "특정 예약에 대한 양도거래 등록 여부를 확인합니다.")
    public ResponseEntity<?> checkUsedItemStatus(@PathVariable Integer reservIdx) {
        try {
            var usedItem = usedTradeService.findByReservIdx(reservIdx);
            
            if (usedItem == null) {
                return ResponseEntity.ok(Map.of(
                    "registered", false,
                    "message", "양도거래가 등록되지 않았습니다."
                ));
            }
            
            // status가 4(취소) 또는 2(거래완료)인 경우 등록되지 않은 것으로 간주 (재판매 가능)
            // status: 0 = 판매중, 1 = 거래중, 2 = 거래완료(판매완료), 3 = 만료, 4 = 취소
            if (usedItem.getStatus() == 4 || usedItem.getStatus() == 2) {
                return ResponseEntity.ok(Map.of(
                    "registered", false,
                    "message", usedItem.getStatus() == 4 ? "양도거래가 취소되었습니다." : "양도거래가 완료되었습니다. 재판매가 가능합니다.",
                    "status", usedItem.getStatus()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "registered", true,
                "usedItemIdx", usedItem.getUsedItemIdx(),
                "price", usedItem.getPrice(),
                "status", usedItem.getStatus(),
                "comment", usedItem.getComment() != null ? usedItem.getComment() : ""
            ));
        } catch (Exception e) {
            log.error("양도거래 등록 여부 확인 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 양도거래 아이템 등록
     */
    @PostMapping("/register")
    @Operation(summary = "양도거래 아이템 등록", description = "새로운 양도거래 아이템을 등록합니다.")
    public ResponseEntity<?> registerUsedItem(@RequestBody Map<String, Object> request) {
        try {
            Integer reservIdx = parseInteger(request.get("reservIdx"));
            Integer price = parseInteger(request.get("price"));
            String comment = (String) request.get("comment");

            if (reservIdx == null || price == null || comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "필수 입력값이 누락되었습니다."));
            }

            var usedItem = usedTradeService.registerUsedItem(reservIdx, price, comment);

            return ResponseEntity.ok(Map.of(
                "message", "양도거래 아이템이 등록되었습니다.",
                "usedItemIdx", usedItem.getUsedItemIdx(),
                "reservIdx", usedItem.getReservIdx(),
                "price", usedItem.getPrice()
            ));

        } catch (Exception e) {
            log.error("양도거래 아이템 등록 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "양도거래 아이템 등록 중 오류가 발생했습니다."));
        }
    }

    /**
     * 양도거래 아이템 수정
     */
    @PutMapping("/{usedItemIdx}")
    @Operation(summary = "양도거래 아이템 수정", description = "기존 양도거래 아이템을 수정합니다.")
    public ResponseEntity<?> updateUsedItem(
            @PathVariable Integer usedItemIdx,
            @RequestBody Map<String, Object> request) {
        try {
            Integer price = parseInteger(request.get("price"));
            String comment = (String) request.get("comment");

            if (price == null || comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "필수 입력값이 누락되었습니다."));
            }

            var usedItem = usedTradeService.updateUsedItem(usedItemIdx, price, comment);

            return ResponseEntity.ok(Map.of(
                "message", "양도거래 아이템이 수정되었습니다.",
                "usedItemIdx", usedItem.getUsedItemIdx(),
                "price", usedItem.getPrice()
            ));

        } catch (Exception e) {
            log.error("양도거래 아이템 수정 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "양도거래 아이템 수정 중 오류가 발생했습니다."));
        }
    }

    /**
     * 양도거래 아이템 취소
     */
    @PostMapping("/{usedItemIdx}/cancel")
    @Operation(summary = "양도거래 아이템 취소", description = "양도거래 아이템을 취소합니다. status를 4로 변경합니다.")
    public ResponseEntity<?> cancelUsedItem(@PathVariable Integer usedItemIdx) {
        try {
            var usedItem = usedTradeService.cancelUsedItem(usedItemIdx);

            return ResponseEntity.ok(Map.of(
                "message", "양도거래가 취소되었습니다.",
                "usedItemIdx", usedItem.getUsedItemIdx(),
                "status", usedItem.getStatus()
            ));

        } catch (Exception e) {
            log.error("양도거래 아이템 취소 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "양도거래 아이템 취소 중 오류가 발생했습니다."));
        }
    }

    // ===== UsedHotelTradeController에서 이전된 API들 =====

    /**
     * 중고 아이템 거래 가능 여부 체크
     */
    @GetMapping("/{usedItemIdx}/availability")
    @Operation(summary = "거래 가능 여부 체크", description = "중고 아이템의 거래 가능 여부를 확인합니다.")
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
    @Operation(summary = "거래 생성", description = "중고 호텔 거래를 생성합니다.")
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
    @Operation(summary = "거래 확정", description = "결제 완료 후 거래를 확정합니다.")
    public ResponseEntity<?> confirmTrade(@PathVariable Integer usedTradeIdx) {
        try {
            UsedTrade confirmedTrade = tradeService.confirmTrade(usedTradeIdx);
            //roomReservation의 customerIdx 값을 변경
            roomReservationService.updateCustomerIdxByReservIdx(confirmedTrade.getReservIdx(), confirmedTrade.getBuyerIdx());

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
     * 거래 삭제 (페이지 이탈 시)
     * navigator.sendBeacon은 POST만 지원하므로 POST도 허용
     */
    @DeleteMapping("/trade/{usedTradeIdx}/delete")
    @PostMapping("/trade/{usedTradeIdx}/delete")
    @Operation(summary = "거래 삭제", description = "페이지 이탈 시 거래를 삭제합니다.")
    public ResponseEntity<?> deleteTrade(@PathVariable Integer usedTradeIdx, @RequestBody(required = false) Map<String, Object> request) {
        try {
            // sendBeacon이나 DELETE 요청 모두 처리
            String reason = (request != null && request.containsKey("reason")) 
                ? (String) request.get("reason") 
                : "사용자 페이지 이탈";
            String timestamp = (request != null && request.containsKey("timestamp")) 
                ? (String) request.get("timestamp") 
                : "";
            
            log.info("거래 삭제 요청: usedTradeIdx={}, reason={}, timestamp={}", usedTradeIdx, reason, timestamp);
            
            tradeService.deleteTrade(usedTradeIdx, reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "거래가 삭제되었습니다.",
                "usedTradeIdx", usedTradeIdx,
                "deletedAt", java.time.LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            log.error("거래 삭제 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "거래 삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 거래 취소
     */
    @PostMapping("/trade/{usedTradeIdx}/cancel")
    @Operation(summary = "거래 취소", description = "거래를 취소합니다.")
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
    @Operation(summary = "거래 상태 조회", description = "거래의 현재 상태를 조회합니다.")
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
    @Operation(summary = "구매자 거래 목록", description = "특정 구매자의 거래 목록을 조회합니다.")
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
    @Operation(summary = "판매자 거래 목록", description = "특정 판매자의 거래 목록을 조회합니다.")
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
    @Operation(summary = "결제 내역 저장", description = "중고 호텔 결제 내역을 저장합니다.")
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
