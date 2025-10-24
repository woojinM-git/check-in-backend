package com.sist.backend.controller.login;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Admin;
import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.admin.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/login")
@Tag(name="로그인/회원가입", description="로그인/회원가입 관련 API")
public class LoginController {
    

    @Autowired
    private CustomerService customerService;
    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    int accessTokenExpireTime = 3600;
    int refreshTokenExpireTime = 604800;

    @GetMapping("/getaccesstoken")
    @Operation(summary="액세스 토큰 발급", description="액세스 토큰 발급")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> getAccessToken(@CookieValue(value = "refreshToken", required = false) String refreshToken,HttpServletResponse response) {
       
        String accessToken = null;
        
        if(refreshToken != null){
            
            if(jwtProvider.verify(refreshToken)){
                Object tokenID = jwtProvider.getClaims(refreshToken).get("tokenID");
                Object id = jwtProvider.getClaims(refreshToken).get("id");
                if(tokenID != null){
                    Optional<Customer> customer = customerService.findByRefTokenAndId(tokenID.toString(),id.toString());
                    if(customer.isPresent()){
                        Customer customer_entity = customer.get();
                        Map<String, Object> accesspayload = new HashMap<>();
                        accesspayload.put("id", customer_entity.getId());
                        accesspayload.put("nickname", customer_entity.getNickname());
                        accesspayload.put("cash", customer_entity.getCash());
                        accesspayload.put("point", customer_entity.getPoint());

                        accessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime);

                        String reTokenID = UUID.randomUUID().toString();

                        Map<String, Object> refreshpayload = new HashMap<>();
                        refreshpayload.put("customerIdx", customer_entity.getCustomerIdx());
                        refreshpayload.put("tokenID",reTokenID);

                        String newRefreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);
                        String newRefreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",newRefreshToken,refreshTokenExpireTime);
                        response.addHeader("Set-Cookie", newRefreshTokenCookieHeader);

                        customer_entity.setRefToken(reTokenID);
                        customerService.save(customer_entity);
                    }

                }
            }
        }
        return ResponseEntity.ok(accessToken);
    }

    @PostMapping("")
    @Operation(summary="회원 로그인", description="로그인 창에서 입력한 값 가져오기")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> login(@RequestBody Customer customer, HttpServletResponse response) {
        Optional<Customer> customer_exist = customerService.findById(customer.getId());
        Customer customer_exist_entity = new Customer();
        String accessToken =null;

        if(customer_exist.isPresent()){
            customer_exist_entity = customer_exist.get();
        
            if(passwordEncoder.matches(customer.getPassword(), customer_exist.get().getPassword())){
                
                String uuid = UUID.randomUUID().toString();

                
                
                Map<String, Object> accesspayload = new HashMap<>();
                Map<String, Object> refreshpayload = new HashMap<>();

                accesspayload.put("customerIdx", customer_exist_entity.getCustomerIdx());
                accesspayload.put("nickname", customer_exist_entity.getNickname());
                accesspayload.put("cash", customer_exist_entity.getCash());
                accesspayload.put("point", customer_exist_entity.getPoint());
                accesspayload.put("role", "customer");
                
                accessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime);
                
                
                refreshpayload.put("id",customer_exist_entity.getId());
                refreshpayload.put("tokenID",uuid);
                
                String refreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);
                

                String accessTokenCookieHeader = String.format("accessToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",accessToken,accessTokenExpireTime);
                String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",refreshToken,refreshTokenExpireTime);

                response.setHeader("Set-Cookie", accessTokenCookieHeader);
                response.addHeader("Set-Cookie", refreshTokenCookieHeader);

                customer_exist_entity.setRefToken(uuid);
                customerService.save(customer_exist_entity);
            }
        }else{
        }

        return ResponseEntity.ok(accessToken);
    }
    
    @PostMapping("/checkId")
    @Operation(summary="아이디 중복 체크", description="아이디만 가져와서 중복 검사하기")
    public ResponseEntity<Map<String, Object>> checkId(@RequestBody Customer customer) {
        System.out.println(customer.getId());
        Map<String, Object> result = new HashMap<>();
        Optional<Customer> customer_exist = customerService.findById(customer.getId());
        
        if(customer_exist.isPresent()){
            result.put("message","중복된 아이디입니다" );
        }else{
            result.put("message","사용 가능한 아이디입니다");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/signup")
    @Operation(summary="회원가입" , description="회원가입 창에서 사용자가 입력한 값을 가져오기")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        Map<String, Object> result = new HashMap<>();
        if(customerAdminSignupDTO.getRole().equals("customer")){
            Customer customer = new Customer();
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
            customer.setJoinDate(LocalDateTime.now());
            customer.setCash(0);
            customer.setStatus(0);
            customer.setTotalPrice(0);
            customer.setPoint(0);
            customer.setRefToken(null);
            customer.setProvider(null);
            customer.setRank("Traveler");
            if(customerService.save(customer) != null) {
                result.put("message","고객 회원가입 성공");
            }else{
                result.put("message","회원가입 실패");
            }
        }else if(customerAdminSignupDTO.getRole().equals("admin")){
            Admin admin = new Admin();
            admin.setId(customerAdminSignupDTO.getId());
            admin.setPw(passwordEncoder.encode(customerAdminSignupDTO.getPassword()));
            admin.setStatus(false);
            admin.setName(customerAdminSignupDTO.getName());
            admin.setPhone(customerAdminSignupDTO.getPhone());
            admin.setRefToken(null);
            admin.setType(true);
            if(adminService.save(admin) != null) {
                result.put("message","관리자 회원가입 성공");
            }else{
                result.put("message","회원가입 실패");
            }
        }else{
            result.put("message","잘못된 접근입니다");
        }
        
        
        return ResponseEntity.ok(result);
    }

    
    
}
