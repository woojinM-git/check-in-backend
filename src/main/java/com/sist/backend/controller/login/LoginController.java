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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.sist.backend.dto.login.PasswordResetRequest;
import com.sist.backend.dto.login.PasswordResetSendCodeRequest;
import com.sist.backend.dto.login.PasswordResetVerifyCodeRequest;
import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.Admin;
import com.sist.backend.entity.Customer;
import com.sist.backend.jwt.JwtProvider;
import com.sist.backend.service.CustomerService;
import com.sist.backend.service.admin.AdminService;
import com.sist.backend.util.CookieUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
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

    @Value("${jwt.access-token-expire-time}")
    private int accessTokenExpireTime;

    @Value("${jwt.refresh-token-expire-time}")
    private int refreshTokenExpireTime;

    @Value("${server.domain}")
    private String serverDomain;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    

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
                        accesspayload.put("customerIdx", customer_entity.getCustomerIdx());
                        accesspayload.put("nickname", customer_entity.getNickname());
                        accesspayload.put("cash", customer_entity.getCash());
                        accesspayload.put("point", customer_entity.getPoint());

                        accessToken = jwtProvider.getToken(accesspayload, accessTokenExpireTime);

                        String reTokenID = UUID.randomUUID().toString();

                        Map<String, Object> refreshpayload = new HashMap<>();
                        refreshpayload.put("customerIdx", customer_entity.getCustomerIdx());
                        refreshpayload.put("tokenID",reTokenID);

                        String newRefreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);
                        String domainAttribute = getDomainAttribute();
                        String sameSiteAttribute = getSameSiteAttribute();
                        String secureAttribute = getSecureAttribute();
                        String newRefreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly%s%s%s", newRefreshToken, refreshTokenExpireTime, sameSiteAttribute, secureAttribute, domainAttribute);
                        response.addHeader("Set-Cookie", newRefreshTokenCookieHeader);

                        customer_entity.setRefToken(reTokenID);
                        customer_entity.setRefTokenUpdatedAt(LocalDateTime.now());
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
                    refreshpayload.put("customerIdx", customer_exist_entity.getCustomerIdx());

                    String refreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);

                    String domainAttribute = getDomainAttribute();
                    String sameSiteAttribute = getSameSiteAttribute();
                    String secureAttribute = getSecureAttribute();
                    String accessTokenCookieHeader = String.format("accessToken=%s; Max-Age=%d; Path=/; HttpOnly%s%s%s", accessToken, refreshTokenExpireTime, sameSiteAttribute, secureAttribute, domainAttribute);
                    String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly%s%s%s", refreshToken, refreshTokenExpireTime, sameSiteAttribute, secureAttribute, domainAttribute);

                    response.setHeader("Set-Cookie", accessTokenCookieHeader);
                    response.addHeader("Set-Cookie", refreshTokenCookieHeader);

                    customer_exist_entity.setRefToken(uuid);
                    customer_exist_entity.setRefTokenUpdatedAt(LocalDateTime.now());
                    customerService.save(customer_exist_entity);
                }
            }else{
            }
        }else if(customerAdminSignupDTO.getRole().equals("admin")){
            Optional<Admin> admin_exist= adminService.findByIdAndStatus(customerAdminSignupDTO.getId(),false);
            if(admin_exist.isEmpty()){
                return ResponseEntity.ok(accessToken);
            }
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
                    refreshpayload.put("adminIdx", admin_exist_entity.getAdminIdx());
                    

                    String refreshToken = jwtProvider.getToken(refreshpayload, refreshTokenExpireTime);

                    String domainAttribute = getDomainAttribute();
                    String sameSiteAttribute = getSameSiteAttribute();
                    String secureAttribute = getSecureAttribute();
                    String accessTokenCookieHeader = String.format("accessToken=%s; Max-Age=%d; Path=/; HttpOnly%s%s%s", accessToken, refreshTokenExpireTime, sameSiteAttribute, secureAttribute, domainAttribute);
                    String refreshTokenCookieHeader = String.format("refreshToken=%s; Max-Age=%d; Path=/; HttpOnly%s%s%s", refreshToken, refreshTokenExpireTime, sameSiteAttribute, secureAttribute, domainAttribute);

                    response.setHeader("Set-Cookie", accessTokenCookieHeader);
                    response.addHeader("Set-Cookie", refreshTokenCookieHeader);

                    admin_exist_entity.setRefToken(uuid);
                    admin_exist_entity.setRefTokenUpdatedAt(LocalDateTime.now());
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
            Optional<Customer> customer_exist = customerService.findById(customerAdminSignupDTO.getId());
            
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
            // 전화번호 포맷팅 (010xxxxxxxx → 010-xxxx-xxxx)
            String formattedPhone = formatPhoneNumber(customerAdminSignupDTO.getPhone());
            customer.setPhone(formattedPhone);
            customer.setEmail(customerAdminSignupDTO.getEmail());
            customer.setBirthday(customerAdminSignupDTO.getBirthday());
            customer.setCash(0);
            customer.setStatus(0);
            customer.setTotalPrice(0);
            customer.setPoint(0);
            customer.setRefToken(null);
            customer.setProvider(null);
            customer.setRank("Traveler");
            if(customerService.findByEmailAndStatus(customerAdminSignupDTO.getEmail(),1).isPresent()){
                Customer customer_exist = customerService.findByEmailAndStatus(customerAdminSignupDTO.getEmail(),1).get();
                customer.setCustomerIdx(customer_exist.getCustomerIdx());
                customer.setCash(customer_exist.getCash());
                customer.setTotalPrice(customer_exist.getTotalPrice());
                customer.setPoint(customer_exist.getPoint());
                customer.setProvider(customer_exist.getProvider());
                customer.setRank(customer_exist.getRank());
            }
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
            // 전화번호 포맷팅 (010xxxxxxxx → 010-xxxx-xxxx)
            String formattedPhone = formatPhoneNumber(customerAdminSignupDTO.getPhone());
            admin.setPhone(formattedPhone);
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
        String inputemail = customerAdminSignupDTO.getEmail(); 
        Optional<Customer> customer_exist = customerService.findByEmailAndStatus(inputemail,0);
        if(customer_exist.isPresent()){
            result.put("message","현재 사용 중인 이메일입니다.");
            result.put("status","fail");
            if(customer_exist.get().getProvider() != null){
                result.put("message","소셜 로그인 사용자입니다.");
                result.put("status","fail");
            }

            return ResponseEntity.ok(result);
        }
    try{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        int code = (int) (Math.random() * 900000) + 100000;
        
        // Redis에 이메일과 코드 저장 (5분 만료)
        String key = "email:verification:" + inputemail;
        redisTemplate.opsForValue().set(key, String.valueOf(code), 5, TimeUnit.MINUTES);
        
        helper.setFrom(fromEmail);
        helper.setTo(inputemail);
        helper.setSubject("[Check-In] 이메일 인증 코드 발송");
        
        helper.setText("인증 코드:   "+code);
    
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

    @PostMapping("/findid")
    @Operation(summary = "아이디 찾기", description = "이름과 이메일을 확인한 뒤 인증코드를 발송하거나 검증하여 아이디를 반환합니다")
    public ResponseEntity<Map<String, Object>> findId(@RequestBody FindIdRequest request) {
        Map<String, Object> result = new HashMap<>();
        Optional<Customer> customerOpt = customerService.findByEmail(request.getEmail());
        if (customerOpt.isEmpty() || customerOpt.get().getName() == null || !customerOpt.get().getName().equals(request.getName())) {
            result.put("status", "fail");
            result.put("message", "이름 또는 이메일이 일치하는 회원이 없습니다.");
            return ResponseEntity.ok(result);
        }

        String key = buildFindIdRedisKey(request.getEmail());

        if (request.getCode() == null || request.getCode().isEmpty()) {
            int verificationCode = (int) (Math.random() * 900000) + 100000;
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(request.getEmail());
                helper.setSubject("[Check-In] 아이디 찾기 인증 코드");
                helper.setText("아이디 찾기 인증 코드: " + verificationCode);

                mailSender.send(message);
                redisTemplate.opsForValue().set(key, String.valueOf(verificationCode), 5, TimeUnit.MINUTES);

                result.put("status", "success");
                result.put("message", "인증 코드가 이메일로 발송되었습니다.");
            } catch (MessagingException e) {
                log.error("아이디 찾기 인증 메일 발송 실패 - email: {}", request.getEmail(), e);
                result.put("status", "fail");
                result.put("message", "인증 코드 발송 중 오류가 발생했습니다.");
            }
            return ResponseEntity.ok(result);
        }

        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            result.put("status", "fail");
            result.put("message", "인증 코드가 만료되었거나 존재하지 않습니다.");
            return ResponseEntity.ok(result);
        }

        if (!storedCode.equals(request.getCode())) {
            result.put("status", "fail");
            result.put("message", "인증 코드가 일치하지 않습니다.");
            return ResponseEntity.ok(result);
        }

        redisTemplate.delete(key);
        result.put("status", "success");
        Customer customer = customerOpt.get();
        if (customer.getProvider() == null) {
            result.put("message", "아이디 찾기에 성공했습니다.");
            result.put("id", customer.getId());
        } else {
            result.put("message", "소셜 로그인 사용자입니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/findpassword/send-code")
    @Operation(summary = "비밀번호 재설정 코드 발송", description = "입력한 회원 정보를 검증하고 인증 코드를 이메일로 발송합니다.")
    public ResponseEntity<Map<String, Object>> sendPasswordResetCode(@RequestBody PasswordResetSendCodeRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (!hasBasicPasswordResetInfo(request.getName(), request.getUserId(), request.getEmail())) {
            result.put("status", "fail");
            result.put("message", "필수 정보를 모두 입력해주세요.");
            return ResponseEntity.ok(result);
        }

        Optional<Customer> customerOpt = customerService.findByIdAndEmailAndName(
            request.getUserId().trim(),
            request.getEmail().trim(),
            request.getName().trim()
        );

        if (customerOpt.isEmpty()) {
            result.put("status", "fail");
            result.put("message", "일치하는 정보가 없습니다.");
            return ResponseEntity.ok(result);
        }

        Customer customer = customerOpt.get();
        if (customer.getStatus() != null && customer.getStatus() == 1) {
            result.put("status", "fail");
            result.put("message", "탈퇴한 회원입니다. 다시 회원가입을 진행해주세요.");
            return ResponseEntity.ok(result);
        }

        if (customer.getProvider() != null) {
            result.put("status", "fail");
            result.put("message", "소셜 로그인 사용자입니다. 해당 플랫폼에서 비밀번호를 변경해주세요.");
            return ResponseEntity.ok(result);
        }

        int verificationCode = (int) (Math.random() * 900000) + 100000;
        String codeKey = buildPasswordResetCodeKey(request.getUserId(), request.getEmail());
        String verifiedKey = buildPasswordResetVerifiedKey(request.getUserId());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(request.getEmail().trim());
            helper.setSubject("[Check-In] 비밀번호 재설정 인증 코드");
            helper.setText("비밀번호 재설정 인증 코드: " + verificationCode);

            mailSender.send(message);
            redisTemplate.opsForValue().set(codeKey, String.valueOf(verificationCode), 5, TimeUnit.MINUTES);
            redisTemplate.delete(verifiedKey);

            result.put("status", "success");
            result.put("message", "인증 코드가 발송되었습니다.");
        } catch (MessagingException exception) {
            log.error("비밀번호 재설정 인증 메일 발송 실패 - userId: {}, email: {}", request.getUserId(), request.getEmail(), exception);
            result.put("status", "fail");
            result.put("message", "인증 코드 발송 중 오류가 발생했습니다.");
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/findpassword/verify-code")
    @Operation(summary = "비밀번호 재설정 코드 검증", description = "입력한 인증 코드를 검증합니다.")
    public ResponseEntity<Map<String, Object>> verifyPasswordResetCode(@RequestBody PasswordResetVerifyCodeRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (!hasBasicPasswordResetInfo(request.getName(), request.getUserId(), request.getEmail())) {
            result.put("status", "fail");
            result.put("message", "필수 정보를 모두 입력해주세요.");
            return ResponseEntity.ok(result);
        }

        if (isBlank(request.getCode())) {
            result.put("status", "fail");
            result.put("message", "인증 코드를 입력해주세요.");
            return ResponseEntity.ok(result);
        }

        Optional<Customer> customerOpt = customerService.findByIdAndEmailAndName(
            request.getUserId().trim(),
            request.getEmail().trim(),
            request.getName().trim()
        );

        if (customerOpt.isEmpty()) {
            result.put("status", "fail");
            result.put("message", "일치하는 정보가 없습니다.");
            return ResponseEntity.ok(result);
        }

        Customer customer = customerOpt.get();
        if (customer.getStatus() != null && customer.getStatus() == 1) {
            result.put("status", "fail");
            result.put("message", "탈퇴한 회원입니다. 다시 회원가입을 진행해주세요.");
            return ResponseEntity.ok(result);
        }

        if (customer.getProvider() != null) {
            result.put("status", "fail");
            result.put("message", "소셜 로그인 사용자입니다. 해당 플랫폼에서 비밀번호를 변경해주세요.");
            return ResponseEntity.ok(result);
        }

        String codeKey = buildPasswordResetCodeKey(request.getUserId(), request.getEmail());
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            result.put("status", "fail");
            result.put("message", "인증 코드가 만료되었거나 존재하지 않습니다.");
            return ResponseEntity.ok(result);
        }

        if (!storedCode.equals(request.getCode().trim())) {
            result.put("status", "fail");
            result.put("message", "인증 코드가 올바르지 않습니다.");
            return ResponseEntity.ok(result);
        }

        redisTemplate.delete(codeKey);
        String verifiedKey = buildPasswordResetVerifiedKey(request.getUserId());
        redisTemplate.opsForValue().set(verifiedKey, request.getEmail().trim(), 10, TimeUnit.MINUTES);

        result.put("status", "success");
        result.put("message", "인증이 완료되었습니다. 새 비밀번호를 설정해주세요.");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/findpassword/reset")
    @Operation(summary = "비밀번호 재설정", description = "인증 이후 새 비밀번호로 변경합니다.")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody PasswordResetRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (isBlank(request.getUserId()) || isBlank(request.getNewPassword())) {
            result.put("status", "fail");
            result.put("message", "필수 정보를 모두 입력해주세요.");
            return ResponseEntity.ok(result);
        }

        String userId = request.getUserId().trim();
        String newPassword = request.getNewPassword().trim();

        if (newPassword.length() < 8) {
            result.put("status", "fail");
            result.put("message", "비밀번호는 8자 이상이어야 합니다.");
            return ResponseEntity.ok(result);
        }

        Optional<Customer> customerOpt = customerService.findById(userId);
        if (customerOpt.isEmpty()) {
            result.put("status", "fail");
            result.put("message", "회원 정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        Customer customer = customerOpt.get();
        if (customer.getStatus() != null && customer.getStatus() == 1) {
            result.put("status", "fail");
            result.put("message", "탈퇴한 회원입니다. 다시 회원가입을 진행해주세요.");
            return ResponseEntity.ok(result);
        }

        if (customer.getProvider() != null) {
            result.put("status", "fail");
            result.put("message", "소셜 로그인 사용자입니다. 해당 플랫폼에서 비밀번호를 변경해주세요.");
            return ResponseEntity.ok(result);
        }

        String verifiedKey = buildPasswordResetVerifiedKey(userId);
        String verifiedValue = redisTemplate.opsForValue().get(verifiedKey);
        if (verifiedValue == null) {
            result.put("status", "fail");
            result.put("message", "인증이 완료되지 않았습니다.");
            return ResponseEntity.ok(result);
        }

        customer.setPassword(passwordEncoder.encode(newPassword));
        customer.setRefToken(null);
        customer.setRefTokenUpdatedAt(null);
        customerService.save(customer);

        redisTemplate.delete(verifiedKey);

        result.put("status", "success");
        result.put("message", "비밀번호가 성공적으로 변경되었습니다.");
        return ResponseEntity.ok(result);
    }
 
    @GetMapping("/logout")
    @Operation(summary="로그아웃", description="쿠키 삭제")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        Authentication Authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) Authentication.getPrincipal();
        System.out.println("principal: "+principal);
        if(principal.getRole().equals("customer")){
            System.out.println("customer 로그아웃");
            Optional<Customer> customer = customerService.findByCustomerIdxAndStatus(principal.getCustomerIdx(),0);
            if(customer.isPresent()){
                customer.get().setRefToken(null);
                customer.get().setRefTokenUpdatedAt(null);
                customerService.save(customer.get());
            }
        }else if(principal.getRole().equals("admin")){
            System.out.println("admin 로그아웃");
            Optional<Admin> admin = adminService.findByAdminIdxAndStatus(principal.getAdminIdx(),false);
            if(admin.isPresent()){
                admin.get().setRefToken(null);
                adminService.save(admin.get());
            }
        }
        // 쿠키 삭제: 기존과 동일한 Path/Domain/SameSite/Secure 조합 유지
        String domainAttribute = getDomainAttribute();
        String sameSiteAttribute = getSameSiteAttribute();
        String secureAttribute = getSecureAttribute();
        String delAccess = String.format("accessToken=; Max-Age=0; Path=/; HttpOnly%s%s%s", sameSiteAttribute, secureAttribute, domainAttribute);
        String delRefresh = String.format("refreshToken=; Max-Age=0; Path=/; HttpOnly%s%s%s", sameSiteAttribute, secureAttribute, domainAttribute);
        response.setHeader("Set-Cookie", delAccess);
        response.addHeader("Set-Cookie", delRefresh);

        return ResponseEntity.ok(result);
    }
    
    /**
     * 전화번호에서 숫자만 추출하여 반환
     * 하이픈 등 구분자는 모두 제거하여 01012345678 형태로 저장
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        String digitsOnly = phone.replaceAll("[^0-9]", "");
        return digitsOnly.isEmpty() ? phone : digitsOnly;
    }

    private boolean hasBasicPasswordResetInfo(String name, String userId, String email) {
        return !isBlank(name) && !isBlank(userId) && !isBlank(email);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildPasswordResetCodeKey(String userId, String email) {
        return "password:reset:code:" + userId.trim() + ":" + email.trim();
    }

    private String buildPasswordResetVerifiedKey(String userId) {
        return "password:reset:verified:" + userId.trim();
    }

    private String getDomainAttribute() {
        return CookieUtils.buildDomainAttribute(serverDomain);
    }

    private String getSameSiteAttribute() {
        return String.format("; SameSite=%s", cookieSameSite);
    }

    private String getSecureAttribute() {
        return cookieSecure ? "; Secure" : "";
    }

    private String buildFindIdRedisKey(String email) {
        return "email:findid:" + email;
    }

    @SuppressWarnings("unused")
    private static class FindIdRequest {
        private String name;
        private String email;
        private String code;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
