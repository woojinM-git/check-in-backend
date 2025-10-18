package com.sist.backend.controller.login;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.entity.Customer;
import com.sist.backend.service.CustomerService;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    

    @Autowired
    private CustomerService customerService;

    
    @PostMapping("/checkId")
    public Map<String, Object> checkId(@RequestBody Customer customer) {
        System.out.println(customer.getId());
        Map<String, Object> result = new HashMap<>();
        Optional<Customer> customer_exist = customerService.findById(customer.getId());
        
        if(customer_exist.isPresent()){
            result.put("message","중복된 아이디입니다" );
        }else{
            result.put("message","사용 가능한 아이디입니다");
        }

        return result;
    }

    
    
}
