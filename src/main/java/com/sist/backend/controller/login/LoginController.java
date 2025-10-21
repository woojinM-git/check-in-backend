package com.sist.backend.controller.login;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/login")
@Tag(name="로그인/회원가입", description="로그인/회원가입 관련 API")
public class LoginController {
    

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Customer customer) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("====================================="+customer+"========================================");
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        customer.setJoinDate(LocalDateTime.now());
        customer.setStatus(0);
        customer.setTotalPrice(0);
        customer.setPoint(0);
        customer.setRefToken(null);
        customer.setProvider(null);
        customer.setCustomrank("Traveler");
        System.out.println("====================================="+customer+"========================================");
        if(customerService.save(customer) != null) {
            result.put("message","회원가입 성공");
        }else{
            result.put("message","회원가입 실패");
        }
        
        return ResponseEntity.ok(result);
    }

    
    
}
