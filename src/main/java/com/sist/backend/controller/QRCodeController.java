package com.sist.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Admin;
import com.sist.backend.entity.RoomReservation;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.repository.RoomReservationRepository;
import com.sist.backend.repository.admin.AdminRepository;
import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QRCodeController {

    private final QRCodeGenerator qrCodeGenerator;
    private final JwtProvider jwtProvider;
    private final RoomReservationRepository roomReservationRepository;
    private final AdminRepository adminRepository;

    @Value("${server.domain}")
    private String serverDomain;

    /**
     * 주문번호 기반 QR 코드 PNG 반환 GET /api/qr/{orderId}?s=200
     */
    @GetMapping("/api/qr/{orderId}")
    public ResponseEntity<byte[]> getQrByOrderId(
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "s", required = false, defaultValue = "200") int size,
            @RequestParam(value = "n", required = false) String hotelName,
            @RequestParam(value = "ci", required = false) String checkIn,
            @RequestParam(value = "co", required = false) String checkOut) {
        try {
            if (size <= 0 || size > 1000) {
                size = 200;
            }
            // QR 코드에 포함할 텍스트 생성
            String qrText = buildQRText(orderId, hotelName, checkIn, checkOut);
            byte[] png = qrCodeGenerator.generateQRCodePng(qrText, size);
            if (png == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
            return new ResponseEntity<>(png, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("QR 코드 생성 엔드포인트 오류: orderId={}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 보안 토큰 기반 QR 코드 이미지 PNG 반환 GET /api/qr/secure?token={jwtToken}&s=200
     */
    @GetMapping("/api/qr/secure")
    public ResponseEntity<byte[]> getSecureQrImage(
            @RequestParam String token,
            @RequestParam(value = "s", required = false, defaultValue = "200") int size) {
        try {
            if (size <= 0 || size > 1000) {
                size = 200;
            }

            // 토큰 검증 (기본 검증만, 관리자 인증은 /verify에서 처리)
            if (!jwtProvider.verify(token)) {
                log.warn("QR 코드 이미지 생성: 유효하지 않은 토큰");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 보안 URL 생성 (토큰 포함)
            String secureUrl = String.format("%s/api/qr/verify?token=%s", serverDomain, token);

            // QR 코드 이미지 생성 (보안 URL을 텍스트로 인코딩)
            byte[] png = qrCodeGenerator.generateQRCodePng(secureUrl, size);
            if (png == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
            return new ResponseEntity<>(png, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("보안 QR 코드 이미지 생성 오류: token={}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * QR 코드에 포함할 텍스트 생성
     */
    private String buildQRText(String orderId, String hotelName, String checkIn, String checkOut) {
        StringBuilder sb = new StringBuilder();
        sb.append("주문번호: ").append(orderId);
        if (hotelName != null && !hotelName.isEmpty()) {
            sb.append("\n호텔: ").append(hotelName);
        }
        if (checkIn != null && !checkIn.isEmpty()) {
            sb.append("\n체크인: ").append(checkIn);
        }
        if (checkOut != null && !checkOut.isEmpty()) {
            sb.append("\n체크아웃: ").append(checkOut);
        }
        return sb.toString();
    }

    /**
     * 보안 강화된 QR 코드 검증 및 예약 정보 조회 GET /api/qr/verify?token={jwtToken}
     */
    @GetMapping("/api/qr/verify")
    public ResponseEntity<?> verifyQRCode(@RequestParam String token) {
        try {
            // 1단계: 토큰 검증
            if (!jwtProvider.verify(token)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않거나 만료된 토큰입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 2단계: 토큰에서 정보 추출
            Map<String, Object> claims = jwtProvider.getClaims(token);
            if (claims == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "토큰 정보를 읽을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 토큰 타입 확인
            Object tokenType = claims.get("type");
            if (tokenType == null || !"qr_verification".equals(tokenType.toString())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "잘못된 토큰 타입입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 3단계: 관리자 인증 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                log.warn("QR 코드 검증: 인증 정보 없음");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("redirect", true);
                errorResponse.put("message", "관리자 로그인이 필요합니다.");
                errorResponse.put("loginUrl", "/admin-login?redirect=/api/qr/verify?token=" + token);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // Principal 타입 확인
            if (!(authentication.getPrincipal() instanceof CustomerAdminSignupDTO)) {
                log.warn("QR 코드 검증: 잘못된 Principal 타입 - {}", authentication.getPrincipal().getClass().getName());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("redirect", true);
                errorResponse.put("message", "관리자 로그인이 필요합니다.");
                errorResponse.put("loginUrl", "/admin-login?redirect=/api/qr/verify?token=" + token);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
            Integer adminIdx = principal.getAdminIdx();

            if (adminIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("redirect", true);
                errorResponse.put("message", "관리자 권한이 필요합니다.");
                errorResponse.put("loginUrl", "/admin-login?redirect=/api/qr/verify?token=" + token);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // 관리자 존재 및 활성 상태 확인
            Optional<Admin> adminOpt = adminRepository.findByAdminIdxAndStatus(adminIdx, false);
            if (adminOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("redirect", true);
                errorResponse.put("message", "유효하지 않은 관리자 계정입니다.");
                errorResponse.put("loginUrl", "/admin-login?redirect=/api/qr/verify?token=" + token);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // 4단계: 예약 정보 조회
            Object reservationIdObj = claims.get("reservationId");
            if (reservationIdObj == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "예약 ID가 없습니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Integer reservationId = Integer.parseInt(reservationIdObj.toString());
            Optional<RoomReservation> reservationOpt = roomReservationRepository.findById(reservationId);

            if (reservationOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "예약 정보를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            RoomReservation reservation = reservationOpt.get();

            // 5단계: 예약 정보 반환
            Map<String, Object> reservationInfo = new HashMap<>();
            reservationInfo.put("reservationId", reservation.getReservIdx());
            reservationInfo.put("orderId", reservation.getOrderIdx() != null ? reservation.getOrderIdx().toString() : "");
            reservationInfo.put("orderNum", reservation.getOrderNum() != null ? reservation.getOrderNum() : "");
            reservationInfo.put("contentId", reservation.getContentid() != null ? reservation.getContentid() : "");
            reservationInfo.put("customerIdx", reservation.getCustomerIdx());
            reservationInfo.put("checkInDate", reservation.getCheckinDate() != null ? reservation.getCheckinDate().toString() : "");
            reservationInfo.put("checkOutDate", reservation.getCheckoutDate() != null ? reservation.getCheckoutDate().toString() : "");
            reservationInfo.put("guest", reservation.getGuest() != null ? reservation.getGuest() : 0);
            reservationInfo.put("totalPrice", reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0);
            reservationInfo.put("status", reservation.getStatus() != null ? reservation.getStatus() : 0);
            reservationInfo.put("specialRequest", reservation.getSpecialRequest() != null ? reservation.getSpecialRequest() : "");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reservation", reservationInfo);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            log.error("QR 코드 검증 오류: 예약 ID 파싱 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "잘못된 예약 ID 형식입니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (ClassCastException e) {
            log.error("QR 코드 검증 오류: Principal 타입 변환 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("redirect", true);
            errorResponse.put("message", "인증 정보 형식이 올바르지 않습니다. 다시 로그인해주세요.");
            errorResponse.put("loginUrl", "/admin-login?redirect=/api/qr/verify?token=" + token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        } catch (Exception e) {
            log.error("QR 코드 검증 엔드포인트 오류: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "서버 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
