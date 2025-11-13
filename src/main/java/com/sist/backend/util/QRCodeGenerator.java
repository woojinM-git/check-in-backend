package com.sist.backend.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sist.backend.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QR 코드 생성 유틸리티 클래스 ZXing 라이브러리를 사용하여 QR 코드를 Base64 인코딩된 데이터 URL로 생성합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QRCodeGenerator {

    @Value("${server.domain}")
    private String serverDomain;

    private final JwtProvider jwtProvider;

    private static final int DEFAULT_SIZE = 200;
    // QR 코드 토큰 만료 시간 (초) - 체크인 날짜 + 1일까지 유효
    private static final int QR_TOKEN_EXPIRE_SECONDS = 86400; // 24시간

    /**
     * 주문 ID를 기반으로 QR 코드의 Base64 데이터 URL을 생성합니다.
     *
     * @param orderId 주문 ID
     * @return QR 코드 이미지의 데이터 URL (data:image/png;base64,...)
     */
    public String generateQRCodeUrl(String orderId) {
        try {
            String url = String.format("%s/api/qr/%s?s=%d", serverDomain, orderId, DEFAULT_SIZE);
            log.info("QR 코드 링크 생성 완료: orderId={}, url={}", orderId, url);
            return url;
        } catch (Exception e) {
            log.error("QR 코드 URL 생성 실패: orderId={}", orderId, e);
            return null;
        }
    }

    /**
     * QR 코드 이미지를 ByteArrayOutputStream으로 생성합니다.
     *
     * @param text QR 코드에 인코딩할 텍스트
     * @param size 이미지 크기 (픽셀)
     * @return 이미지 바이트 배열
     */
    public ByteArrayOutputStream generateQRCodeImage(String text, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);

            return baos;

        } catch (Exception e) {
            log.error("QR 코드 이미지 생성 실패: text={}, size={}", text, size, e);
            return null;
        }
    }

    /**
     * QR 코드 PNG 바이트 배열을 생성합니다.
     */
    public byte[] generateQRCodePng(String text, int size) {
        ByteArrayOutputStream baos = generateQRCodeImage(text, size);
        if (baos == null) {
            return null;
        }
        return baos.toByteArray();
    }

    /**
     * 사용자 정의 크기로 QR 코드 URL을 생성합니다.
     *
     * @param orderId 주문 ID
     * @param size QR 코드 크기 (픽셀)
     * @return QR 코드 이미지의 데이터 URL
     */
    public String generateQRCodeUrl(String orderId, int size) {
        try {
            String url = String.format("%s/api/qr/%s?s=%d", serverDomain, orderId, size);
            log.info("QR 코드 링크 생성 완료: orderId={}, url={}", orderId, url);
            return url;
        } catch (Exception e) {
            log.error("QR 코드 URL 생성 실패: orderId={}, size={}", orderId, size, e);
            return null;
        }
    }

    /**
     * 예약 정보를 포함한 상세 QR 코드 URL을 생성합니다.
     *
     * @param orderId 주문 ID
     * @param hotelName 호텔명
     * @param checkIn 체크인 날짜
     * @param checkOut 체크아웃 날짜
     * @return QR 코드 이미지 URL (파라미터 포함)
     */
    public String generateDetailedQRCodeUrl(String orderId, String hotelName, String checkIn, String checkOut) {
        try {
            // URL에 상세 정보를 파라미터로 추가
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(String.format("%s/api/qr/%s?s=%d", serverDomain, orderId, DEFAULT_SIZE));

            if (hotelName != null && !hotelName.isEmpty()) {
                urlBuilder.append("&n=").append(java.net.URLEncoder.encode(hotelName, "UTF-8"));
            }
            if (checkIn != null && !checkIn.isEmpty()) {
                urlBuilder.append("&ci=").append(java.net.URLEncoder.encode(checkIn, "UTF-8"));
            }
            if (checkOut != null && !checkOut.isEmpty()) {
                urlBuilder.append("&co=").append(java.net.URLEncoder.encode(checkOut, "UTF-8"));
            }

            String url = urlBuilder.toString();
            log.info("상세 QR 코드 링크 생성 완료: orderId={}, hotelName={}, url={}", orderId, hotelName, url);
            return url;
        } catch (Exception e) {
            log.error("상세 QR 코드 URL 생성 실패: orderId={}, hotelName={}", orderId, hotelName, e);
            return null;
        }
    }

    /**
     * 보안 강화된 QR 코드 URL 생성 (JWT 토큰 기반)
     *
     * @param reservationId 예약 ID
     * @param userId 사용자 ID
     * @param checkInDate 체크인 날짜 (만료 시간 계산용)
     * @return QR 코드 검증 URL (토큰 포함)
     */
    public String generateSecureQRCodeUrl(Integer reservationId, Integer userId, LocalDate checkInDate) {
        try {
            // JWT 토큰 생성
            Map<String, Object> claims = new HashMap<>();
            claims.put("reservationId", reservationId.toString());
            claims.put("userId", userId.toString());
            claims.put("type", "qr_verification");
            claims.put("issuedAt", System.currentTimeMillis() / 1000); // 초 단위

            // 만료 시간: 체크인 날짜 + 1일 (또는 최소 24시간)
            long expireTime;
            if (checkInDate != null) {
                long checkInTimestamp = checkInDate.atStartOfDay()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toEpochSecond();
                long now = System.currentTimeMillis() / 1000;
                // 체크인 날짜가 미래면 체크인 + 1일, 과거면 현재 + 24시간
                expireTime = checkInTimestamp > now
                        ? checkInTimestamp + QR_TOKEN_EXPIRE_SECONDS
                        : now + QR_TOKEN_EXPIRE_SECONDS;
            } else {
                expireTime = (System.currentTimeMillis() / 1000) + QR_TOKEN_EXPIRE_SECONDS;
            }
            claims.put("expiresAt", expireTime);

            // 토큰 생성 (만료 시간을 초 단위로 전달)
            int expireSeconds = (int) (expireTime - (System.currentTimeMillis() / 1000));
            String token = jwtProvider.getToken(claims, expireSeconds);

            // QR 코드 이미지 URL 생성 (이미지 엔드포인트 사용)
            String url = String.format("%s/api/qr/secure?token=%s&s=%d", serverDomain, token, DEFAULT_SIZE);
            log.info("보안 QR 코드 URL 생성 완료: reservationId={}, userId={}, url={}", reservationId, userId, url);
            return url;
        } catch (Exception e) {
            log.error("보안 QR 코드 URL 생성 실패: reservationId={}, userId={}", reservationId, userId, e);
            return null;
        }
    }
}
