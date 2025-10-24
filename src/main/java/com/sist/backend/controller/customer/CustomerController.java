package com.sist.backend.controller.customer;

import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;
import com.sist.backend.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
@Tag(name="고객 관리", description="고객 정보 관련 API")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JwtProvider jwtProvider;

    @GetMapping("/me")
    @Operation(summary="현재 사용자 정보 조회", description="httpOnly 쿠키에서 사용자 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 1. 쿠키에서 accessToken 가져오기
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
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증이 필요합니다."
                ));
            }
            
            // 2. JWT에서 사용자 정보 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            Object customerIdxObj = claims.get("customerIdx");
            Integer customerIdx = null;
            
            if (customerIdxObj != null) {
                if (customerIdxObj instanceof String) {
                    try {
                        customerIdx = Integer.parseInt((String) customerIdxObj);
                    } catch (NumberFormatException e) {
                        System.err.println("customerIdx 파싱 오류: " + e.getMessage());
                    }
                } else if (customerIdxObj instanceof Integer) {
                    customerIdx = (Integer) customerIdxObj;
                }
            } else {
                // customerIdx가 없으면 id로 조회
                String userId = (String) claims.get("id");
                if (userId != null) {
                    Optional<Customer> customer = customerService.findById(userId);
                    if (customer.isPresent()) {
                        customerIdx = customer.get().getCustomerIdx();
                    }
                }
            }
            
            if (customerIdx == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }
            
            // 3. 데이터베이스에서 고객 정보 조회
            Optional<Customer> customerOpt = customerService.findByCustomerIdx(customerIdx);
            if (!customerOpt.isPresent()) {
                return ResponseEntity.status(401).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }
            
            Customer customerEntity = customerOpt.get();
            
            // 4. 응답 데이터 구성
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("customerIdx", customerEntity.getCustomerIdx());
            userInfo.put("id", customerEntity.getId());
            userInfo.put("nickname", customerEntity.getNickname());
            userInfo.put("rank", customerEntity.getRank());
            userInfo.put("joinDate", customerEntity.getJoinDate());
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "message", "사용자 정보 조회 중 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }
}
