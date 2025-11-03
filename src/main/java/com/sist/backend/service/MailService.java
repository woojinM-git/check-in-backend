package com.sist.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.PaymentRequestDto;
import com.sist.backend.entity.EmailLog;
import com.sist.backend.repository.EmailLogRepository;
import com.sist.backend.util.QRCodeGenerator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 발송 서비스 Gmail SMTP를 사용하여 결제 완료 이메일을 발송합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final QRCodeGenerator qrCodeGenerator;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 호텔 예약 결제 완료 이메일을 발송합니다.
     *
     * @param request 결제 요청 정보
     * @param qrUrl QR 코드 URL
     * @return 이메일 발송 성공 여부
     */
    public boolean sendHotelReservationEmail(PaymentRequestDto request, String qrUrl) {
        try {
            // TODO: 이메일 발송 전 사용자 이메일 유효성 검증 추가
            // TODO: 이메일 발송 실패 시 재시도 로직 추가
            // TODO: 이메일 발송 상태 추적 및 로깅 시스템 구축

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 이메일 기본 정보 설정
            helper.setFrom(fromEmail);
            helper.setTo(request.getCustomerEmail());
            helper.setSubject("[Check-In] 결제가 완료되었습니다");

            // HTML 본문 생성
            String htmlContent = generateHotelReservationEmailHtml(request, qrUrl);
            helper.setText(htmlContent, true);

            // QR 코드 이미지를 첨부파일로 추가 (Base64에서 디코딩)
            if (qrUrl != null && qrUrl.startsWith("data:image")) {
                try {
                    String base64Data = qrUrl.split(",")[1];
                    byte[] qrImageBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayResource qrResource = new ByteArrayResource(qrImageBytes);
                    helper.addAttachment("reservation_qr.png", qrResource, "image/png");
                } catch (Exception e) {
                    log.error("QR 코드 이미지 첨부 실패", e);
                }
            }

            // 이메일 발송
            mailSender.send(message);
            log.info("호텔 예약 이메일 발송 완료: customerEmail={}, orderId={}",
                    request.getCustomerEmail(), request.getOrderId());

            // EmailLog 저장
            saveEmailLog(request.getCustomerIdx(), "hotel_reservation",
                    "[Check-In] 결제가 완료되었습니다",
                    request.getCustomerEmail(), htmlContent, true);

            return true;

        } catch (MessagingException e) {
            log.error("호텔 예약 이메일 발송 실패: customerEmail={}, orderId={}",
                    request.getCustomerEmail(), request.getOrderId(), e);

            // EmailLog 저장 (실패)
            saveEmailLog(request.getCustomerIdx(), "hotel_reservation",
                    "[Check-In] 결제가 완료되었습니다",
                    request.getCustomerEmail(), "이메일 발송 실패: " + e.getMessage(), false);

            return false;
        }
    }

    /**
     * 중고 호텔 구매 결제 완료 이메일을 발송합니다.
     *
     * @param request 결제 요청 정보
     * @param qrUrl QR 코드 URL
     * @return 이메일 발송 성공 여부
     */
    public boolean sendUsedHotelPurchaseEmail(PaymentRequestDto request, String qrUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 이메일 기본 정보 설정
            helper.setFrom(fromEmail);
            helper.setTo(request.getCustomerEmail());
            helper.setSubject("[Check-In] 중고 호텔 구매가 완료되었습니다");

            // HTML 본문 생성
            String htmlContent = generateUsedHotelPurchaseEmailHtml(request, qrUrl);
            helper.setText(htmlContent, true);

            // QR 코드 이미지를 첨부파일로 추가 (Base64에서 디코딩)
            if (qrUrl != null && qrUrl.startsWith("data:image")) {
                try {
                    String base64Data = qrUrl.split(",")[1];
                    byte[] qrImageBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayResource qrResource = new ByteArrayResource(qrImageBytes);
                    helper.addAttachment("purchase_qr.png", qrResource, "image/png");
                } catch (Exception e) {
                    log.error("QR 코드 이미지 첨부 실패", e);
                }
            }

            // 이메일 발송
            mailSender.send(message);
            log.info("중고 호텔 구매 이메일 발송 완료: customerEmail={}, orderId={}",
                    request.getCustomerEmail(), request.getOrderId());

            // EmailLog 저장
            saveEmailLog(request.getCustomerIdx(), "used_hotel_purchase",
                    "[Check-In] 중고 호텔 구매가 완료되었습니다",
                    request.getCustomerEmail(), htmlContent, true);

            return true;

        } catch (MessagingException e) {
            log.error("중고 호텔 구매 이메일 발송 실패: customerEmail={}, orderId={}",
                    request.getCustomerEmail(), request.getOrderId(), e);

            // EmailLog 저장 (실패)
            saveEmailLog(request.getCustomerIdx(), "used_hotel_purchase",
                    "[Check-In] 중고 호텔 구매가 완료되었습니다",
                    request.getCustomerEmail(), "이메일 발송 실패: " + e.getMessage(), false);

            return false;
        }
    }

    /**
     * 호텔 예약 이메일 HTML 본문을 생성합니다 (리디자인 버전)
     */
    private String generateHotelReservationEmailHtml(PaymentRequestDto request, String qrUrl) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        return String.format("""
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>결제 완료 안내</title>
            <style>
                body {
                    font-family: 'Malgun Gothic', Arial, sans-serif;
                    background-color: #f4f6f9;
                    margin: 0;
                    padding: 40px 0;
                    line-height: 1.6;
                    color: #333;
                }
                .container {
                    max-width: 640px;
                    margin: 0 auto;
                    background-color: #fff;
                    border-radius: 12px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    color: #fff;
                    text-align: center;
                    padding: 40px 20px;
                }
                .header h1 {
                    margin: 0;
                    font-size: 28px;
                }
                .header p {
                    margin-top: 10px;
                    font-size: 16px;
                    opacity: 0.9;
                }
                .content {
                    padding: 30px 35px 40px;
                }
                .info-box {
                    background: #fafafa;
                    padding: 24px;
                    border-radius: 10px;
                    margin-bottom: 28px;
                    border: 1px solid #eee;
                }
                .info-title {
                    color: #4a56e2;
                    font-size: 18px;
                    font-weight: bold;
                    margin-bottom: 18px;
                    border-left: 4px solid #4a56e2;
                    padding-left: 10px;
                }
                .info-row {
                    display: flex;
                    justify-content: space-between;
                    padding: 8px 0;
                    border-bottom: 1px solid #eee;
                    font-size: 15px;
                }
                .info-row:last-child {
                    border-bottom: none;
                }
                .total-row {
                    font-size: 17px;
                    font-weight: bold;
                    color: #4a56e2;
                    padding-top: 14px;
                }
                .qr-section {
                    text-align: center;
                    margin-top: 40px;
                    padding: 30px;
                    background: #f9f9ff;
                    border-radius: 10px;
                    border: 1px solid #eee;
                }
                .qr-section h3 {
                    color: #4a56e2;
                    margin-bottom: 12px;
                }
                .qr-section p {
                    font-size: 14px;
                    color: #555;
                    margin-bottom: 20px;
                }
                .qr-section img {
                    max-width: 220px;
                    border-radius: 10px;
                    border: 1px solid #ddd;
                    box-shadow: 0 3px 6px rgba(0,0,0,0.1);
                }
                .footer {
                    background: #333;
                    color: #fff;
                    padding: 25px 20px;
                    text-align: center;
                    font-size: 14px;
                }
                .footer p {
                    margin: 6px 0;
                }
                .footer small {
                    color: #bbb;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🎉 예약이 완료되었습니다!</h1>
                    <p>체크인 호텔과 함께 멋진 여행을 시작하세요.</p>
                </div>

                <div class="content">
                    <div class="info-box">
                        <div class="info-title">📅 예약 정보</div>
                        <div class="info-row"><span>주문번호</span><span><strong>%s</strong></span></div>
                        <div class="info-row"><span>체크인</span><span>%s</span></div>
                        <div class="info-row"><span>체크아웃</span><span>%s</span></div>
                        <div class="info-row"><span>숙박 일수</span><span>%d박</span></div>
                        <div class="info-row"><span>게스트</span><span>%d명</span></div>
                    </div>

                    <div class="info-box">
                        <div class="info-title">👤 예약자 정보</div>
                        <div class="info-row"><span>이름</span><span>%s</span></div>
                        <div class="info-row"><span>이메일</span><span>%s</span></div>
                        <div class="info-row"><span>전화번호</span><span>%s</span></div>
                        %s
                    </div>

                    <div class="info-box">
                        <div class="info-title">💳 결제 정보</div>
                        <div class="info-row"><span>객실 가격</span><span>₩%,d /박</span></div>
                        <div class="info-row"><span>숙박 일수</span><span>%d박</span></div>
                        <div class="info-row"><span>결제 수단</span><span>%s</span></div>
                        <div class="info-row total-row">
                            <span>총 결제 금액</span>
                            <span>₩%,d</span>
                        </div>
                    </div>

                    <div class="qr-section">
                        <h3>📱 예약 확인 QR 코드</h3>
                        <p>체크인 시 아래 QR 코드를 제시해주세요.</p>
                        %s
                    </div>
                </div>

                <div class="footer">
                    <p><strong>체크인 서비스</strong></p>
                    <p>고객센터: 1588-0000 | 이메일: support@checkin.com</p>
                    <small>이 이메일은 발신 전용입니다. 문의사항은 고객센터로 연락해주세요.</small>
                </div>
            </div>
        </body>
        </html>
        """,
                request.getOrderId(),
                request.getCheckIn() != null ? LocalDate.parse(request.getCheckIn()).format(formatter) : "미정",
                request.getCheckOut() != null ? LocalDate.parse(request.getCheckOut()).format(formatter) : "미정",
                request.getNights() != null ? request.getNights() : 0,
                request.getGuests() != null ? request.getGuests() : 0,
                request.getCustomerName() != null ? request.getCustomerName() : "",
                request.getCustomerEmail() != null ? request.getCustomerEmail() : "",
                request.getCustomerPhone() != null ? request.getCustomerPhone() : "",
                request.getSpecialRequests() != null
                ? String.format("<div class=\"info-row\"><span>특별 요청사항</span><span>%s</span></div>", request.getSpecialRequests()) : "",
                request.getRoomPrice() != null ? request.getRoomPrice() : 0,
                request.getNights() != null ? request.getNights() : 0,
                request.getMethod() != null ? request.getMethod() : "카드",
                request.getAmount() != null ? request.getAmount() : 0,
                qrUrl != null ? String.format("<img src=\"%s\" alt=\"QR Code\">", qrUrl) : "<p>QR 코드 생성 중 오류가 발생했습니다.</p>"
        );
    }

    /**
     * 중고 호텔 구매 이메일 HTML 본문을 생성합니다.
     */
    private String generateUsedHotelPurchaseEmailHtml(PaymentRequestDto request, String qrUrl) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>구매 완료</title>
                <style>
                    body { font-family: 'Malgun Gothic', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; }
                    .header { background: linear-gradient(135deg, #ff6b6b 0%%, #ee5a24 100%%); padding: 30px; text-align: center; color: white; }
                    .content { padding: 30px; }
                    .info-box { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .info-title { color: #ff6b6b; font-size: 18px; font-weight: bold; margin-bottom: 15px; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
                    .info-row:last-child { border-bottom: none; }
                    .total-row { font-size: 18px; font-weight: bold; color: #ff6b6b; padding: 15px 0; }
                    .qr-section { text-align: center; margin: 30px 0; }
                    .qr-section img { max-width: 200px; border: 1px solid #ddd; border-radius: 8px; }
                    .footer { background: #333; color: white; padding: 20px; text-align: center; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 구매 완료!</h1>
                        <p>중고 호텔 구매가 성공적으로 완료되었습니다.</p>
                    </div>
                    
                    <div class="content">
                        <div class="info-box">
                            <div class="info-title">구매 정보</div>
                            <div class="info-row">
                                <span>주문번호</span>
                                <span><strong>%s</strong></span>
                            </div>
                            <div class="info-row">
                                <span>체크인</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span>체크아웃</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span>숙박 일수</span>
                                <span>%d박</span>
                            </div>
                            <div class="info-row">
                                <span>게스트</span>
                                <span>%d명</span>
                            </div>
                        </div>
                        
                        <div class="info-box">
                            <div class="info-title">구매자 정보</div>
                            <div class="info-row">
                                <span>이름</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span>이메일</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row">
                                <span>전화번호</span>
                                <span>%s</span>
                            </div>
                        </div>
                        
                        <div class="info-box">
                            <div class="info-title">결제 정보</div>
                            <div class="info-row">
                                <span>결제 수단</span>
                                <span>%s</span>
                            </div>
                            <div class="info-row total-row">
                                <span>총 결제 금액</span>
                                <span>₩%,d</span>
                            </div>
                        </div>
                        
                        <div class="qr-section">
                            <h3>구매 확인 QR 코드</h3>
                            <p>체크인 시 이 QR 코드를 제시해주세요.</p>
                            %s
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p><strong>체크인 서비스</strong></p>
                        <p>고객센터: 1588-0000 | 이메일: support@checkin.com</p>
                        <p style="color: #ccc; font-size: 12px; margin-top: 10px;">
                            이 이메일은 발신 전용입니다. 문의사항이 있으시면 고객센터로 연락해주세요.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
                request.getOrderId(),
                request.getCheckIn() != null ? LocalDate.parse(request.getCheckIn()).format(formatter) : "미정",
                request.getCheckOut() != null ? LocalDate.parse(request.getCheckOut()).format(formatter) : "미정",
                request.getNights() != null ? request.getNights() : 0,
                request.getGuests() != null ? request.getGuests() : 0,
                request.getCustomerName() != null ? request.getCustomerName() : "",
                request.getCustomerEmail() != null ? request.getCustomerEmail() : "",
                request.getCustomerPhone() != null ? request.getCustomerPhone() : "",
                request.getMethod() != null ? request.getMethod() : "카드",
                request.getAmount() != null ? request.getAmount() : 0,
                qrUrl != null ? String.format("<img src=\"%s\" alt=\"QR Code\">", qrUrl) : "<p>QR 코드 생성 중 오류가 발생했습니다.</p>"
        );
    }

    /**
     * 이메일 발송 로그 저장
     *
     * @param customerIdx 고객 ID
     * @param template 이메일 템플릿명
     * @param subject 제목
     * @param toEmail 수신 이메일
     * @param payloadJson 이메일 본문 (HTML 또는 JSON)
     * @param status 발송 성공 여부
     */
    private void saveEmailLog(Integer customerIdx, String template, String subject,
            String toEmail, String payloadJson, Boolean status) {
        try {
            EmailLog emailLog = EmailLog.builder()
                    .customerIdx(customerIdx)
                    .template(template)
                    .subject(subject)
                    .toEmail(toEmail)
                    .payloadJson(payloadJson != null && payloadJson.length() > 5000
                            ? payloadJson.substring(0, 5000) + "..."
                            : payloadJson) // TEXT 컬럼이므로 너무 길면 잘라냄
                    .status(status)
                    .sentAt(LocalDateTime.now())
                    .build();

            emailLogRepository.save(emailLog);
            log.debug("이메일 로그 저장 완료: customerIdx={}, template={}, status={}",
                    customerIdx, template, status);
        } catch (Exception e) {
            log.error("이메일 로그 저장 실패 (무시): customerIdx={}", customerIdx, e);
            // 로그 저장 실패는 무시 (이메일 발송은 이미 완료됨)
        }
    }
}
