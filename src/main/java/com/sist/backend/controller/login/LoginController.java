package com.sist.backend.controller.login;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
    public ResponseEntity<String> login(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO, HttpServletResponse response) {
        String accessToken =null;
       if(customerAdminSignupDTO.getRole().equals("customer")){
            Optional<Customer> customer_exist= customerService.findByIdAndStatus(customerAdminSignupDTO.getId(), 0);
       
            Customer customer_exist_entity = new Customer();


            if(customer_exist.isPresent()&& customer_exist.get().getStatus()==0){
                customer_exist_entity = customer_exist.get();

                if(passwordEncoder.matches(customerAdminSignupDTO.getPassword(), customer_exist.get().getPassword())){

                    String uuid = UUID.randomUUID().toString();



                    Map<String, Object> accesspayload = new HashMap<>();
                    Map<String, Object> refreshpayload = new HashMap<>();

                    accesspayload.put("id", customer_exist_entity.getId()); // id 필드 추가
                    accesspayload.put("customerIdx", customer_exist_entity.getCustomerIdx());
                    accesspayload.put("nickname", customer_exist_entity.getNickname());
                    accesspayload.put("cash", customer_exist_entity.getCash());
                    accesspayload.put("point", customer_exist_entity.getPoint());
                    accesspayload.put("role", customerAdminSignupDTO.getRole());

                    accessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime);


                    refreshpayload.put("id",customer_exist_entity.getId());
                    refreshpayload.put("tokenID",uuid);
                    refreshpayload.put("role", customerAdminSignupDTO.getRole());

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
        }else if(customerAdminSignupDTO.getRole().equals("admin")){
            Optional<Admin> admin_exist= adminService.findById(customerAdminSignupDTO.getId());

            Admin admin_exist_entity = new Admin();


            admin_exist_entity = admin_exist.get();
            
                if(passwordEncoder.matches(customerAdminSignupDTO.getPassword(), admin_exist.get().getPw())){

                    String uuid = UUID.randomUUID().toString();



                    Map<String, Object> accesspayload = new HashMap<>();
                    Map<String, Object> refreshpayload = new HashMap<>();

                    accesspayload.put("adminIdx", admin_exist_entity.getAdminIdx());
                    accesspayload.put("role", customerAdminSignupDTO.getRole());

                    accessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime);


                    refreshpayload.put("id",admin_exist_entity.getId());
                    refreshpayload.put("tokenID",uuid);
                    refreshpayload.put("role", customerAdminSignupDTO.getRole());

                    String refreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);


                    String accessTokenCookieHeader = String.format("accessToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",accessToken,accessTokenExpireTime);
                    String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",refreshToken,refreshTokenExpireTime);

                    response.setHeader("Set-Cookie", accessTokenCookieHeader);
                    response.addHeader("Set-Cookie", refreshTokenCookieHeader);

                    admin_exist_entity.setRefToken(uuid);
                    adminService.save(admin_exist_entity);
                }
            
        }

        return ResponseEntity.ok(accessToken);
    }
    
    @PostMapping("/checkId")
    @Operation(summary="아이디 중복 체크", description="아이디와 역할을 가져와서 중복 검사하기")
    public ResponseEntity<Map<String, Object>> checkId(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        
        Map<String, Object> result = new HashMap<>();
        if(customerAdminSignupDTO.getRole().equals("customer")){
            Optional<Customer> customer_exist = customerService.findByIdAndStatus(customerAdminSignupDTO.getId(),0);
            
            if(customer_exist.isPresent()){
                result.put("message","중복된 아이디입니다" );
                result.put("status","fail" );
            }else{
                result.put("message","사용 가능한 아이디입니다");
                result.put("status","success");
            }
        }else if(customerAdminSignupDTO.getRole().equals("admin")){
            Optional<Admin> admin_exist = adminService.findById(customerAdminSignupDTO.getId());
            if(admin_exist.isPresent()){
                result.put("message","중복된 아이디입니다" );
                result.put("status","fail");
            }else{
                result.put("message","사용 가능한 아이디입니다");
                result.put("status","success");
            }
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/checkNickname")
    @Operation(summary="닉네임 중복 체크", description="닉네임을 가져와서 중복 검사하기")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        
        Map<String, Object> result = new HashMap<>();
        
        Optional<Customer> customer_exist = customerService.findByNicknameAndStatus(customerAdminSignupDTO.getNickname(),0);
        
        if(customer_exist.isPresent()){
            result.put("message","중복된 닉네임입니다" );
            result.put("status","fail" );
        }else{
            result.put("message","사용 가능한 닉네임입니다");
            result.put("status","success");
        }
        

        return ResponseEntity.ok(result);
    }

    @PostMapping("/signup")
    @Operation(summary="회원가입" , description="회원가입 창에서 사용자가 입력한 값을 가져오기")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        Map<String, Object> result = new HashMap<>();
        if(customerAdminSignupDTO.getRole().equals("customer")){
            Customer customer = new Customer();
            customer.setPassword(passwordEncoder.encode(customerAdminSignupDTO.getPassword()));
            customer.setJoinDate(LocalDateTime.now());
            customer.setId(customerAdminSignupDTO.getId());
            customer.setNickname(customerAdminSignupDTO.getNickname());
            customer.setName(customerAdminSignupDTO.getName());
            customer.setGender(customerAdminSignupDTO.getGender());
            customer.setPhone(customerAdminSignupDTO.getPhone());
            customer.setEmail(customerAdminSignupDTO.getEmail());
            customer.setBirthday(customerAdminSignupDTO.getBirthday());
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
    


    @PostMapping("/send-verification-code")
    @Operation(summary="이메일 인증 코드 발송", description="회원가입 시 이메일 인증 코드를 발송합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, Object>> sendHotelReservationEmail(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        Map<String, Object> result = new HashMap<>();
    try{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        int code = (int) (Math.random() * 900000) + 100000;
        
        // Redis에 이메일과 코드 저장 (5분 만료)
        String key = "email:verification:" + customerAdminSignupDTO.getEmail();
        redisTemplate.opsForValue().set(key, String.valueOf(code), 5, TimeUnit.MINUTES);
        
        helper.setFrom(fromEmail);
        helper.setTo(customerAdminSignupDTO.getEmail());
        helper.setSubject("[Check-In] 이메일 인증 코드 발송");
        
        helper.setText("인증 코드:"+code);
    
        mailSender.send(message);
        result.put("message","이메일 인증 코드 발송 성공");
        result.put("status","success");
    }catch(MessagingException e){
        
        result.put("message","이메일 인증 코드 발송 실패");
        result.put("status","fail");
        return ResponseEntity.ok(result);
    }
    return ResponseEntity.ok(result);
    }
    
    @PostMapping("/verify-email")
    @Operation(summary="이메일 인증 코드 검증", description="발송된 인증 코드를 검증합니다")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody CustomerAdminSignupDTO customerAdminSignupDTO) {
        Map<String, Object> result = new HashMap<>();
        
        String key = "email:verification:" + customerAdminSignupDTO.getEmail();
        String storedCode = redisTemplate.opsForValue().get(key);
        System.out.println("========================================="+storedCode+"=========================================");
        System.out.println("========================================="+customerAdminSignupDTO.getCode()+"=========================================");
        System.out.println("========================================="+key+"=========================================");
        
        if (storedCode == null) {
            result.put("message", "인증 코드가 만료되었거나 존재하지 않습니다");
            result.put("status", "fail");
            return ResponseEntity.ok(result);
        }
        
        // 입력된 코드와 저장된 코드 비교
        String inputCode = customerAdminSignupDTO.getCode();
        if (storedCode.equals(inputCode)) {
            // 인증 성공 시 Redis에서 삭제
            redisTemplate.delete(key);
            result.put("message", "이메일 인증이 완료되었습니다");
            result.put("status", "success");
        } else {
            result.put("message", "인증 코드가 올바르지 않습니다");
            result.put("status", "fail");
        }
        
        return ResponseEntity.ok(result);
    }
}
