package com.sist.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RegistrationRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.CustomerService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.service.MasterManagementService;
import com.sist.backend.service.RegistrationRequestService;
import com.sist.backend.service.RoomPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @RequestMapping("/customerList")
    public Map<String, Object> findAllCustomer() {
        Map<String, Object> map = new HashMap<>();
        List<Customer> customerList = customerService.findAll();

        if(customerList != null && !customerList.isEmpty()) {
            map.put("customerList", customerList);
            map.put("customerCount", customerList.size());
        }

        return map;
    }

    @RequestMapping("/hotelList")
    @Operation(summary = "마스터 호텔 관리", description = "등록되어 있는 호텔의 목록을 보여줍니다.")
    @ApiResponse(responseCode = "200", description = "호텔관리 성공")
    public ResponseEntity<Page<HotelInfoDto>>  findAllHotelWithDetailsAsDto(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
        @RequestParam(defaultValue = "0") int page, 
        @Parameter(description = "페이지당 데이터 개수", example = "5") 
        @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(hotelInfoService.findAllHotelWithDetailsAsDto(pageable));
    }

    /* 등록되어 있는 호텔의 목록 */
    @Operation(summary = "마스터 호텔 관리 호출")
    @ApiResponse(responseCode = "200", description = "호텔관리 성공")
    @RequestMapping("/hotels")
    public Map<String, Object> findAllHotelWithDetails() {
        Map<String, Object> map = new HashMap<>();
        List<HotelInfo> hotelList = hotelInfoService.findAllHotelWithDetails();

        if(hotelList != null && !hotelList.isEmpty()) {
            // HotelInfo를 Map으로 변환하여 필요한 데이터만 전송
            List<Map<String, Object>> hotelMapList = new ArrayList<>();
            for(HotelInfo hotel : hotelList) {
                Map<String, Object> hotelMap = new HashMap<>();
                hotelMap.put("contentId", hotel.getContentId());
                hotelMap.put("title", hotel.getTitle());
                hotelMap.put("adress", hotel.getAdress());
                hotelMap.put("tel", hotel.getTel());
                hotelMap.put("status", hotel.getStatus());
                hotelMap.put("imageUrl", hotel.getImageUrl());
                
                // HotelDetail에서 객실 수 가져오기
                if (hotel.getHotelDetail() != null) {
                    hotelMap.put("rooms", hotel.getHotelDetail().getRoomcount());
                } else {
                    hotelMap.put("rooms", "0");
                }
                
                // Admin 정보
                if (hotel.getAdmin() != null) {
                    hotelMap.put("adminName", hotel.getAdmin().getName());
                    hotelMap.put("adminEmail", hotel.getAdmin().getId());
                    hotelMap.put("adminPhone", hotel.getAdmin().getPhone());
                }
                
                hotelMapList.add(hotelMap);
            }
            
            map.put("hotelList", hotelMapList);
            map.put("hotelCount", hotelMapList.size());
        }

        return map;
    }

    /* 승인요청을 한 호텔들 */
    @Operation(summary = "호텔승인 화면 호출")
    @ApiResponse(responseCode = "200", description = "호텔승인화면 성공")
    @RequestMapping("/hotelApproval")
    public Map<String, Object> hotelApproval() {
        Map<String, Object> map = new HashMap<>();
        List<RegistrationRequest> hotelApproval = registrationRequestService.findByStatus();

        if(hotelApproval != null && !hotelApproval.isEmpty()) {
            List<Map<String, Object>> requestList = new ArrayList<>();
            for(RegistrationRequest request : hotelApproval) {
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
                    requestMap.put("businessNumber", "사업자번호 예시 더미");
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

    @Operation(summary = "마스터 대시보드 호출")
    @ApiResponse(responseCode = "200", description = "대시보드 성공")
    @RequestMapping("/dashboard")
    public Map<String, Object> dashboard() {
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
