package com.sist.backend.controller;

import com.sist.backend.dto.RouletteResultDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.PointLedger;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.PointLedgerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/roulette")
@RequiredArgsConstructor
@Tag(name = "룰렛 게임", description = "룰렛 게임 결과 생성 API")
public class RouletteController {

    private final JwtProvider jwtProvider;
    private final CustomerRepository customerRepository;
    private final PointLedgerRepository pointLedgerRepository;

    // 룰렛 금액 배열 (인덱스 순서대로)
    private static final int[] PRIZES = {100, 200, 500, 1000, 5000};
    
    // 각 금액별 확률 (누적 확률)
    // 100원: 40%, 200원: 40%, 500원: 15%, 1000원: 4%, 5000원: 1%
    private static final double[] PROBABILITIES = {
        0.40,  // 100원 (0 ~ 40%)
        0.80,  // 200원 (40% ~ 80%)
        0.95,  // 500원 (80% ~ 95%)
        0.99,  // 1000원 (95% ~ 99%)
        1.00   // 5000원 (99% ~ 100%)
    };

    @PostMapping("/spin")
    @Operation(summary = "룰렛 게임 실행", description = "확률에 따라 룰렛 결과를 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룰렛 결과 생성 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> spinRoulette(HttpServletRequest request) {
        try {
            // 확률 기반 결과 생성
            Random random = new Random();
            double randomValue = random.nextDouble(); // 0.0 ~ 1.0
            
            int prizeIndex = 0;
            int prize = 0;
            
            // 확률에 따라 당첨 금액 결정
            for (int i = 0; i < PROBABILITIES.length; i++) {
                if (randomValue <= PROBABILITIES[i]) {
                    prizeIndex = i;
                    prize = PRIZES[i];
                    break;
                }
            }
            
            log.info("룰렛 결과: randomValue={}, prizeIndex={}, prize={}원", randomValue, prizeIndex, prize);
            
            // 룰렛이 해당 인덱스에 멈추도록 계산
            // 포인터는 12시 방향(-Math.PI/2)에 위치
            // 각 구간의 시작이 -Math.PI/2에서 시작하므로, 목표 인덱스가 포인터 아래에 오도록 해야 함
            int baseRotation = random.nextInt(2) + 4; // 4~5바퀴 (부드러운 애니메이션)
            
            // 각 구간의 크기 (라디안)
            double anglePerPrize = 2 * Math.PI / PRIZES.length;
            
            // 목표 구간이 포인터 아래에 오도록 회전 각도 계산
            // 프론트엔드 룰렛 그리기: startAngle = index * anglePerPrize - Math.PI/2 + rotation
            // 포인터 위치: -Math.PI/2 (12시 방향)
            // 목표: prizeIndex 구간 내의 랜덤 위치에 포인터가 멈추도록
            // 
            // 구간 내 랜덤 위치 (20%~80% 사이, 경계 피하기)
            double randomPositionInSection = 0.2 + random.nextDouble() * 0.6; // 0.2 ~ 0.8
            double offsetInSection = prizeIndex * anglePerPrize + (randomPositionInSection * anglePerPrize);
            
            // 여러 바퀴 회전 후, 구간 내 랜덤 위치에 포인터가 오도록:
            // rotation = baseRotation * 2 * Math.PI - offsetInSection
            double finalAngle = baseRotation * 2 * Math.PI - offsetInSection;
            
            RouletteResultDto result = RouletteResultDto.builder()
                .prize(prize)
                .prizeIndex(prizeIndex)
                .message(String.format("%d원 당첨!", prize))
                .build();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            response.put("prizeIndex", prizeIndex); // 실제 당첨 인덱스 (0-4)
            response.put("finalAngle", finalAngle); // 최종 회전 각도 (라디안)
            response.put("baseRotation", baseRotation); // 기본 회전 바퀴 수
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("룰렛 게임 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "룰렛 게임 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/add-point")
    @Operation(summary = "룰렛 당첨 포인트 지급", description = "룰렛 당첨 금액을 포인트로 지급합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "포인트 지급 성공"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @Transactional
    public ResponseEntity<Map<String, Object>> addPoint(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            // 1. JWT에서 customerIdx 추출
            Integer customerIdx = getCustomerIdxFromToken(httpRequest);
            if (customerIdx == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(401).body(errorResponse);
            }

            // 2. 당첨 금액 추출
            Object prizeObj = request.get("prize");
            if (prizeObj == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "당첨 금액이 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            int prize = prizeObj instanceof Integer ? (Integer) prizeObj : Integer.parseInt(prizeObj.toString());

            // 3. 고객 정보 조회
            Optional<Customer> customerOpt = customerRepository.findByCustomerIdx(customerIdx);
            if (customerOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "고객 정보를 찾을 수 없습니다.");
                return ResponseEntity.status(404).body(errorResponse);
            }

            Customer customer = customerOpt.get();

            // 4. 포인트 추가
            int currentPoint = customer.getPoint() != null ? customer.getPoint() : 0;
            customer.setPoint(currentPoint + prize);
            customerRepository.save(customer);

            log.info("룰렛 포인트 지급: customerIdx={}, prize={}, 현재 포인트={}", customerIdx, prize, customer.getPoint());

            // 5. PointLedger에 기록
            try {
                PointLedger pointLedger = PointLedger.builder()
                        .customerIdx(customerIdx)
                        .point(prize)
                        .pointType("적립")
                        .memo("룰렛뽑기")
                        .createdAt(LocalDateTime.now())
                        .build();
                pointLedgerRepository.save(pointLedger);
                log.info("PointLedger 기록 완료: customerIdx={}, point={}, pointType=적립", customerIdx, prize);
            } catch (Exception e) {
                log.error("PointLedger 기록 실패: {}", e.getMessage(), e);
                // PointLedger 기록 실패해도 포인트 지급은 완료된 것으로 처리
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("%d 포인트가 지급되었습니다.", prize));
            response.put("prize", prize);
            response.put("totalPoint", customer.getPoint());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("포인트 지급 오류 발생", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "포인트 지급 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * JWT 토큰에서 customerIdx 추출
     */
    private Integer getCustomerIdxFromToken(HttpServletRequest request) {
        try {
            // 쿠키에서 accessToken 가져오기
            Cookie[] cookies = request.getCookies();
            String accessToken = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("accessToken".equals(cookie.getName())) {
                        accessToken = cookie.getValue();
                        break;
                    }
                }
            }

            if (accessToken == null) {
                return null;
            }

            // JWT 토큰 검증
            if (!jwtProvider.verify(accessToken)) {
                return null;
            }

            // JWT에서 customerIdx 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            Object customerIdxObj = claims.get("customerIdx");
            
            if (customerIdxObj == null) {
                return null;
            }

            if (customerIdxObj instanceof Integer) {
                return (Integer) customerIdxObj;
            } else if (customerIdxObj instanceof String) {
                try {
                    return Integer.parseInt((String) customerIdxObj);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("JWT 토큰 처리 오류", e);
            return null;
        }
    }

    @GetMapping("/prizes")
    @Operation(summary = "룰렛 당첨금 목록", description = "룰렛의 모든 당첨금 목록을 반환합니다")
    public ResponseEntity<Map<String, Object>> getPrizes() {
        Map<String, Object> response = new HashMap<>();
        response.put("prizes", PRIZES);
        response.put("probabilities", PROBABILITIES);
        return ResponseEntity.ok(response);
    }
}

