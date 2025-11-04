package com.sist.backend.controller.customer;

import com.sist.backend.dto.customer.CustomerDto;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;
import com.sist.backend.jwt.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary="현재 사용자 정보 조회", description="httpOnly 쿠키에서 사용자 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // // 1. 쿠키에서 accessToken 가져오기
            // Cookie[] cookies = request.getCookies();
            // String accessToken = null;
            
            // if (cookies != null) {
            //     for (Cookie cookie : cookies) {
            //         if ("accessToken".equals(cookie.getName())) {
            //             accessToken = cookie.getValue();
            //             break;
            //         }
            //     }
            // }
            
            // if (accessToken == null) {
            //     return ResponseEntity.status(401).body(Map.of(
            //         "message", "인증이 필요합니다."
            //     ));
            // }
            
            // // 2. JWT에서 사용자 정보 추출
            // Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            // Object customerIdxObj = claims.get("customerIdx");
            // Integer customerIdx = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
            Object customerIdxObj = principal.getCustomerIdx();
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
                String userId = (String) principal.getId();
                if (userId != null) {
                    Optional<Customer> customer = customerService.findByIdAndStatus(userId, 0);
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
            
            // 4. 응답 데이터 구성 (CustomerDTO로 반환)
            CustomerDto customerDto = CustomerDto.builder()
                .customerIdx(customerEntity.getCustomerIdx())
                .id(customerEntity.getId())
                .name(customerEntity.getName()) // 실명 추가
                .nickname(customerEntity.getNickname())
                .email(customerEntity.getEmail())
                .phone(customerEntity.getPhone())
                .cash(customerEntity.getCash() != null ? new java.math.BigDecimal(customerEntity.getCash()) : null)
                .point(customerEntity.getPoint() != null ? new java.math.BigDecimal(customerEntity.getPoint()) : null)
                .totalPrice(customerEntity.getTotalPrice() != null ? new java.math.BigDecimal(customerEntity.getTotalPrice()) : null)
                .rank(customerEntity.getRank())
                .joinDate(customerEntity.getJoinDate())
                .build();
            
            return ResponseEntity.ok(customerDto);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "message", "사용자 정보 조회 중 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/profile")
    @Operation(summary="프로필 정보 수정", description="현재 사용자의 프로필 정보를 수정합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> data, HttpServletRequest request) {
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
                    Optional<Customer> customer = customerService.findByIdAndStatus(userId, 0);
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
                return ResponseEntity.status(404).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }
            
            Customer customerEntity = customerOpt.get();
            
            // 4. 프로필 정보 업데이트
            if (data.containsKey("nickname")) {
                customerEntity.setNickname(data.get("nickname"));
            }
            if (data.containsKey("phone")) {
                customerEntity.setPhone(data.get("phone"));
            }
            if (data.containsKey("email")) {
                customerEntity.setEmail(data.get("email"));
            }
            
            // 5. 변경사항 저장
            Customer savedCustomer = customerService.save(customerEntity);
            
            // 6. 응답 데이터 구성
            CustomerDto customerDto = CustomerDto.builder()
                .customerIdx(savedCustomer.getCustomerIdx())
                .id(savedCustomer.getId())
                .nickname(savedCustomer.getNickname())
                .email(savedCustomer.getEmail())
                .phone(savedCustomer.getPhone())
                .cash(savedCustomer.getCash() != null ? new java.math.BigDecimal(savedCustomer.getCash()) : null)
                .point(savedCustomer.getPoint() != null ? new java.math.BigDecimal(savedCustomer.getPoint()) : null)
                .totalPrice(savedCustomer.getTotalPrice() != null ? new java.math.BigDecimal(savedCustomer.getTotalPrice()) : null)
                .rank(savedCustomer.getRank())
                .joinDate(savedCustomer.getJoinDate())
                .build();
            
            return ResponseEntity.ok(customerDto);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "message", "프로필 수정 중 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/changePassword")
    @Operation(summary="비밀번호 수정", description="현재 사용자의 비밀번호를 수정합니다. 변경 성공 시 자동으로 로그아웃됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비밀번호 수정 성공"),
        @ApiResponse(responseCode = "400", description = "비밀번호가 일치하지 않거나 유효하지 않음"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @Transactional
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> data, 
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            System.out.println("비밀번호 변경 요청 시작");
            
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
                System.err.println("비밀번호 변경 실패: accessToken이 없음");
                return ResponseEntity.status(401).body(Map.of(
                    "message", "인증이 필요합니다."
                ));
            }
            
            // 2. JWT에서 customerIdx 추출
            Map<String, Object> claims = jwtProvider.getClaims(accessToken);
            if (claims == null) {
                System.err.println("비밀번호 변경 실패: 유효하지 않은 토큰");
                return ResponseEntity.status(401).body(Map.of(
                    "message", "유효하지 않은 토큰입니다."
                ));
            }
            
            Object customerIdxObj = claims.get("customerIdx");
            Integer customerIdx = null;
            
            if (customerIdxObj != null) {
                if (customerIdxObj instanceof String) {
                    try {
                        customerIdx = Integer.parseInt((String) customerIdxObj);
                    } catch (NumberFormatException e) {
                        System.err.println("customerIdx 파싱 오류: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (customerIdxObj instanceof Integer) {
                    customerIdx = (Integer) customerIdxObj;
                }
            } else {
                // customerIdx가 없으면 id로 조회
                String userId = (String) claims.get("id");
                if (userId != null) {
                    Optional<Customer> customer = customerService.findByIdAndStatus(userId, 0);
                    if (customer.isPresent()) {
                        customerIdx = customer.get().getCustomerIdx();
                    }
                }
            }
            
            if (customerIdx == null) {
                System.err.println("비밀번호 변경 실패: customerIdx를 찾을 수 없음");
                return ResponseEntity.status(401).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }
            
            System.out.println("비밀번호 변경 요청 - customerIdx: " + customerIdx);
            
            // 3. 요청 데이터 검증
            String currentPassword = data.get("currentPassword");
            String newPassword = data.get("newPassword");
            String confirmPassword = data.get("confirmPassword");
            
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "message", "현재 비밀번호를 입력해주세요."
                ));
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                    "message", "새 비밀번호를 입력해주세요."
                ));
            }
            
            if (newPassword.length() < 8) {
                return ResponseEntity.status(400).body(Map.of(
                    "message", "비밀번호는 8자 이상이어야 합니다."
                ));
            }
            
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.status(400).body(Map.of(
                    "message", "새 비밀번호가 일치하지 않습니다."
                ));
            }
            
            // 4. DB에서 고객 정보 조회
            Optional<Customer> customerOpt = customerService.findByCustomerIdx(customerIdx);
            if (!customerOpt.isPresent()) {
                System.err.println("비밀번호 변경 실패: customerIdx " + customerIdx + "에 해당하는 고객을 찾을 수 없음");
                return ResponseEntity.status(404).body(Map.of(
                    "message", "사용자 정보를 찾을 수 없습니다."
                ));
            }
            
            Customer customerEntity = customerOpt.get();
            
            // 5. 현재 비밀번호 검증 (BCrypt로 비교)
            if (customerEntity.getPassword() == null) {
                System.err.println("비밀번호 변경 실패: DB에 저장된 비밀번호가 null임 - customerIdx: " + customerIdx);
                return ResponseEntity.status(500).body(Map.of(
                    "message", "시스템 오류가 발생했습니다."
                ));
            }
            
            if (!passwordEncoder.matches(currentPassword, customerEntity.getPassword())) {
                System.err.println("비밀번호 변경 실패: 현재 비밀번호가 일치하지 않음 - customerIdx: " + customerIdx);
                return ResponseEntity.status(400).body(Map.of(
                    "message", "현재 비밀번호가 일치하지 않습니다."
                ));
            }
            
            // 6. 새 비밀번호와 현재 비밀번호가 같은지 확인
            if (passwordEncoder.matches(newPassword, customerEntity.getPassword())) {
                return ResponseEntity.status(400).body(Map.of(
                    "message", "새 비밀번호는 현재 비밀번호와 달라야 합니다."
                ));
            }
            
            // 7. 새 비밀번호를 BCrypt로 암호화하여 저장
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            customerEntity.setPassword(encodedNewPassword);
            
            // 8. refreshToken 무효화 (자동 로그아웃 처리)
            // refToken만 null로 설정하고, refTokenUpdatedAt은 그대로 둠 (null로 설정하면 JwtFilter에서 NPE 발생 가능)
            customerEntity.setRefToken(null);
            // refTokenUpdatedAt은 null로 설정하지 않음 - 기존 값 유지
            
            // 9. 변경사항 저장
            try {
                customerService.save(customerEntity);
                System.out.println("비밀번호 변경 성공 - customerIdx: " + customerIdx);
            } catch (Exception e) {
                System.err.println("비밀번호 변경 중 DB 저장 실패 - customerIdx: " + customerIdx);
                e.printStackTrace();
                throw e;
            }
            
            // 10. 쿠키 삭제 (자동 로그아웃)
            try {
                String delAccess = "accessToken=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax";
                String delRefresh = "refreshToken=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax";
                response.setHeader("Set-Cookie", delAccess);
                response.addHeader("Set-Cookie", delRefresh);
                System.out.println("쿠키 삭제 완료");
            } catch (Exception e) {
                System.err.println("쿠키 삭제 중 오류 발생 (무시하고 진행): " + e.getMessage());
                e.printStackTrace();
                // 쿠키 삭제 실패해도 비밀번호 변경은 성공했으므로 계속 진행
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요."
            ));
            
        } catch (Exception e) {
            System.err.println("비밀번호 변경 중 예외 발생");
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "message", "비밀번호 변경 중 오류가 발생했습니다.",
                "error", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류"
            ));
        }
    }
}


