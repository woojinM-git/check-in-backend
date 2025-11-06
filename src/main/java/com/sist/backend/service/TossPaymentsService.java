package com.sist.backend.service;

import com.sist.backend.config.TossPaymentsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * TossPayments 결제 검증 서비스 TossPayments API를 사용하여 결제를 검증합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsService {

    private final TossPaymentsConfig tossPaymentsConfig;

    private static final String DEFAULT_API_BASE = "https://api.tosspayments.com";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * TossPayments API를 사용하여 결제를 검증합니다.
     *
     * @param paymentKey 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 검증 결과
     */
    public Map<String, Object> confirmPayment(String paymentKey, String orderId, Integer amount) {
        try {
            log.info("TossPayments 결제 검증 시작: paymentKey={}, orderId={}, amount={}",
                    paymentKey, orderId, amount);

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((tossPaymentsConfig.getPayments().getSecretKey() + ":").getBytes()));

            // 요청 본문 설정
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", paymentKey);
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount);

            log.info("TossPayments API 요청 데이터: orderId={}, amount={}", orderId, amount);
            String baseUrl = tossPaymentsConfig.getPayments().getApiBaseUrl() != null
                    ? tossPaymentsConfig.getPayments().getApiBaseUrl()
                    : DEFAULT_API_BASE;
            String confirmUrl = baseUrl + "/v1/payments/confirm";
            log.info("TossPayments API URL: {}", confirmUrl);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // TossPayments API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    confirmUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("TossPayments API 응답 상태: {}", response.getStatusCode());
            log.info("TossPayments API 응답 본문 (민감정보 제외): status={}",
                    response.getBody() != null ? response.getBody().get("status") : "null");

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("TossPayments 결제 검증 성공: orderId={}, status={}",
                        orderId, responseBody.get("status"));
                return responseBody;
            } else {
                log.error("TossPayments 결제 검증 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("TossPayments 결제 검증 실패: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("TossPayments 결제 검증 중 오류 발생: paymentKey={}, orderId={}",
                    paymentKey, orderId, e);
            throw new RuntimeException("TossPayments 결제 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 상태를 확인합니다.
     *
     * @param paymentKey 결제 키
     * @return 결제 상태 정보
     */
    public Map<String, Object> getPaymentStatus(String paymentKey) {
        try {
            log.info("TossPayments 결제 상태 조회: paymentKey={}", paymentKey);

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((tossPaymentsConfig.getPayments().getSecretKey() + ":").getBytes()));

            HttpEntity<String> request = new HttpEntity<>(headers);

            // TossPayments API 호출
            String baseUrl = tossPaymentsConfig.getPayments().getApiBaseUrl() != null
                    ? tossPaymentsConfig.getPayments().getApiBaseUrl()
                    : DEFAULT_API_BASE;
            String url = baseUrl + "/v1/payments/" + paymentKey;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("TossPayments 결제 상태 조회 성공: {}", responseBody);
                return responseBody;
            } else {
                log.error("TossPayments 결제 상태 조회 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("TossPayments 결제 상태 조회 실패");
            }

        } catch (Exception e) {
            log.error("TossPayments 결제 상태 조회 중 오류 발생: paymentKey={}", paymentKey, e);
            throw new RuntimeException("TossPayments 결제 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 취소를 처리합니다.
     * 관리자나 전액환불 시나리오를 위해 남겨둠돠
     *
     * @param paymentKey 결제 키
     * @param cancelReason 취소 사유
     * @return 취소 결과
     */
    public Map<String, Object> cancelPayment(String paymentKey, String cancelReason) {
        try {
            log.info("TossPayments 결제 취소 시작: paymentKey={}, reason={}", paymentKey, cancelReason);

            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((tossPaymentsConfig.getPayments().getSecretKey() + ":").getBytes()));

            // 요청 본문 설정
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", cancelReason);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // TossPayments API 호출
            String baseUrl = tossPaymentsConfig.getPayments().getApiBaseUrl() != null
                    ? tossPaymentsConfig.getPayments().getApiBaseUrl()
                    : DEFAULT_API_BASE;
            String url = baseUrl + "/v1/payments/" + paymentKey + "/cancel";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("TossPayments 결제 취소 성공: {}", responseBody);
                return responseBody;
            } else {
                log.error("TossPayments 결제 취소 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("TossPayments 결제 취소 실패");
            }

        } catch (Exception e) {
            log.error("TossPayments 결제 취소 중 오류 발생: paymentKey={}", paymentKey, e);
            throw new RuntimeException("TossPayments 결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 부분 취소(부분 환불)를 처리합니다.
     *
     * @param paymentKey 결제 키
     * @param cancelAmount 환불 금액(정수, KRW)
     * @param cancelReason 취소 사유
     */
    public Map<String, Object> cancelPaymentWithAmount(String paymentKey, Integer cancelAmount, String cancelReason) {
        try {
            log.info("TossPayments 부분 결제 취소 시작: paymentKey={}, amount={}, reason={}", paymentKey, cancelAmount, cancelReason);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((tossPaymentsConfig.getPayments().getSecretKey() + ":").getBytes()));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cancelReason", cancelReason);
            requestBody.put("cancelAmount", cancelAmount);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String baseUrl = tossPaymentsConfig.getPayments().getApiBaseUrl() != null
                    ? tossPaymentsConfig.getPayments().getApiBaseUrl()
                    : DEFAULT_API_BASE;
            String url = baseUrl + "/v1/payments/" + paymentKey + "/cancel";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("TossPayments 부분 결제 취소 성공: {}", responseBody);
                return responseBody;
            } else {
                log.error("TossPayments 부분 결제 취소 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("TossPayments 부분 결제 취소 실패");
            }

        } catch (Exception e) {
            log.error("TossPayments 부분 결제 취소 중 오류 발생: paymentKey={}", paymentKey, e);
            throw new RuntimeException("TossPayments 부분 결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
