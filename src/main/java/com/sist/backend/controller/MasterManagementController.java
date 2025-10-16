package com.sist.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RegistrationRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.CustomerService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.service.MasterManagementService;
import com.sist.backend.service.RegistrationRequestService;
import com.sist.backend.service.RoomPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterManagementController {
    
    /* 서비스 호출 */
    private final MasterManagementService mmService;
    private final HotelInfoService hotelInfoService;
    private final RoomPaymentService roomPaymentService;
    private final RegistrationRequestService registrationRequestService;
    private final CustomerService customerService;

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
    

    @Operation(summary = "마스터 대시보드 호출")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @RequestMapping("/dashboard")
    public Map<String, Object> findAllHotel() {
        List<HotelInfo> HotelList = hotelInfoService.findAllHotel();
        List<RegistrationRequest> pendingRequests = registrationRequestService.findByStatus();
        List<Customer> customerList = customerService.findAll();
        List<Customer> newCustomers = customerService.findByJoinDate();
        Long paymentAmount = roomPaymentService.findByPrice();
        Map<String, Object> map = new HashMap<>();

        if(HotelList != null && !HotelList.isEmpty()) {
            map.put("hotelList", HotelList);
            map.put("hotelCount", HotelList.size());
        }
        if(customerList != null && !customerList.isEmpty()) {
            map.put("customerList", customerList);
            map.put("customerCount", customerList.size());
        }
        if(paymentAmount != null) {
            map.put("paymentAmount", paymentAmount);
        }
        if(newCustomers != null && !newCustomers.isEmpty()) {
            // 오늘 가입한 고객 데이터를 필요한 필드만 Map으로 변환
            List<Map<String, Object>> newCustomerMapList = new ArrayList<>();
            for(Customer customer : newCustomers) {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("customerIdx", customer.getCustomerIdx());
                customerMap.put("name", customer.getName());           // 회원명
                customerMap.put("email", customer.getEmail());         // 이메일
                customerMap.put("joinDate", customer.getJoinDate());   // 가입일
                customerMap.put("totalPrice", customer.getTotalPrice()); // 누적금액
                customerMap.put("status", customer.getStatus());       // 상태
                newCustomerMapList.add(customerMap);
            }
            map.put("newCustomers", newCustomerMapList);
            map.put("newCustomersCount", newCustomers.size());
        }

        if(pendingRequests != null && !pendingRequests.isEmpty()) {
            List<Map<String, Object>> requestList = new ArrayList<>();
            for(RegistrationRequest request : pendingRequests) {
                Map<String, Object> requestMap = new HashMap<>();

                // 호텔 정보
                if (request.getHotelInfo() != null) {
                    requestMap.put("contentId", request.getHotelInfo().getContentId());
                    requestMap.put("title", request.getHotelInfo().getTitle());
                    requestMap.put("adress", request.getHotelInfo().getAdress());
                    requestMap.put("rooms", request.getHotelInfo().getRooms().size());
                    requestMap.put("requestDate", request.getRegiDate());
                    requestMap.put("status", request.getStatus());
                }

                // 사업자 정보
                if(request.getAdmin() != null) {
                    requestMap.put("ownerName", request.getAdmin().getName());
                    requestMap.put("ownerPhone", request.getAdmin().getPhone());
                    requestMap.put("ownerEmail", request.getAdmin().getId());
                }

                
                requestMap.put("registrationIdx", request.getRegistrationIdx());
                requestMap.put("regiDate", request.getRegiDate());
                requestMap.put("status", request.getStatus());

                requestList.add(requestMap);
            }
            map.put("hotelRequestList", requestList);
            map.put("hotelRequestCount", requestList.size());
        }
        return map;
    }

}
