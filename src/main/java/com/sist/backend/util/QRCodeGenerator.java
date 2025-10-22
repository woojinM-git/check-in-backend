package com.sist.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * QR 코드 생성 유틸리티 클래스 Google Chart API를 사용하여 QR 코드를 생성합니다.
 */
@Component
@Slf4j
public class QRCodeGenerator {

    private static final String GOOGLE_CHART_API_URL = "https://chart.googleapis.com/chart";
    private static final int DEFAULT_SIZE = 200;

    /**
     * 주문 ID를 기반으로 QR 코드 URL을 생성합니다.
     *
     * @param orderId 주문 ID
     * @return QR 코드 이미지 URL
     */
    public String generateQRCodeUrl(String orderId) {
        try {
            // TODO: QR 코드에 더 많은 정보 포함 (호텔명, 체크인/아웃 날짜 등)
            // TODO: QR 코드 만료 시간 설정 로직 추가
            // TODO: QR 코드 보안 강화 (암호화, 서명 등)

            String encodedOrderId = URLEncoder.encode(orderId, StandardCharsets.UTF_8);
            String qrUrl = String.format("%s?chs=%dx%d&cht=qr&chl=%s",
                    GOOGLE_CHART_API_URL, DEFAULT_SIZE, DEFAULT_SIZE, encodedOrderId);

            log.info("QR 코드 URL 생성 완료: orderId={}, url={}", orderId, qrUrl);
            return qrUrl;
        } catch (Exception e) {
            log.error("QR 코드 URL 생성 실패: orderId={}", orderId, e);
            return null;
        }
    }

    /**
     * 사용자 정의 크기로 QR 코드 URL을 생성합니다.
     *
     * @param orderId 주문 ID
     * @param size QR 코드 크기 (픽셀)
     * @return QR 코드 이미지 URL
     */
    public String generateQRCodeUrl(String orderId, int size) {
        try {
            String encodedOrderId = URLEncoder.encode(orderId, StandardCharsets.UTF_8);
            String qrUrl = String.format("%s?chs=%dx%d&cht=qr&chl=%s",
                    GOOGLE_CHART_API_URL, size, size, encodedOrderId);

            log.info("QR 코드 URL 생성 완료: orderId={}, size={}, url={}", orderId, size, qrUrl);
            return qrUrl;
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
     * @return QR 코드 이미지 URL
     */
    public String generateDetailedQRCodeUrl(String orderId, String hotelName, String checkIn, String checkOut) {
        try {
            String qrData = String.format("주문번호: %s\n호텔: %s\n체크인: %s\n체크아웃: %s",
                    orderId, hotelName, checkIn, checkOut);
            String encodedData = URLEncoder.encode(qrData, StandardCharsets.UTF_8);
            String qrUrl = String.format("%s?chs=%dx%d&cht=qr&chl=%s",
                    GOOGLE_CHART_API_URL, DEFAULT_SIZE, DEFAULT_SIZE, encodedData);

            log.info("상세 QR 코드 URL 생성 완료: orderId={}, hotelName={}", orderId, hotelName);
            return qrUrl;
        } catch (Exception e) {
            log.error("상세 QR 코드 URL 생성 실패: orderId={}, hotelName={}", orderId, hotelName, e);
            return null;
        }
    }

    /**
     * QR 코드 이미지를 바이트 배열로 다운로드합니다.
     *
     * @param qrUrl QR 코드 URL
     * @return QR 코드 이미지 바이트 배열
     */
    public byte[] downloadQRCodeImage(String qrUrl) {
        try {
            java.net.URL url = new java.net.URL(qrUrl);
            java.io.InputStream inputStream = url.openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] imageBytes = outputStream.toByteArray();
            outputStream.close();

            log.info("QR 코드 이미지 다운로드 완료: size={} bytes", imageBytes.length);
            return imageBytes;
        } catch (IOException e) {
            log.error("QR 코드 이미지 다운로드 실패: url={}", qrUrl, e);
            return null;
        }
    }
}
