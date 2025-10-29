package com.sist.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.util.QRCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QRCodeController {

    private final QRCodeGenerator qrCodeGenerator;

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
}
