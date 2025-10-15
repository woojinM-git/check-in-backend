package com.sist.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.CustomerService;
import com.sist.backend.service.HotelInfoService;
import com.sist.backend.service.MasterManagementService;
import com.sist.backend.service.RoomPaymentService;


@RestController
@RequestMapping("/api/master")
public class MasterManagementController {
    
    @Autowired
    MasterManagementService mmService;

    @Autowired
    HotelInfoService hotelInfoService;

    @Autowired
    RoomPaymentService roomPaymentService;

    @Autowired
    CustomerService customerService;

    /* 마스터 화면에서 확인할 수 있는 관리자 목록 */
    @RequestMapping("/adminList")
    public Object findAllAdmin(@RequestParam("type") Boolean type) {
        boolean chk = true;
        if(type == false) 
            chk = false;
        return mmService.findAllAdmin(chk);
    }

    /* 등록되어 있는 회원의 목록 */
    @RequestMapping("customerList")
    public Object findAllCustomer() {
        return customerService.findAll();
    }
    

    /* 등록되어 있는 호텔의 목록 */
    @RequestMapping("/dashboard")
    public Map<String, Object> findAllHotel() {
        List<HotelInfo> HotelList = hotelInfoService.findAllHotel();
        // List<Customer> customerList = customerService.findAll();
        Map<String, Object> map = new HashMap<>();

        if(HotelList != null && !HotelList.isEmpty()) {
            map.put("hotelList", HotelList);
            map.put("hotelCount", HotelList.size());
            // map.put("customerList", customerList);
            // map.put("customerCount", customerList.size());
        }
        
        return map;
    }

    /* 결제내역의 쌓인 금액 */
    @RequestMapping("/paymentAmount")
    public Object findByPrice() {
        return roomPaymentService.findByPrice();
    }



}
