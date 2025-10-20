package com.sist.backend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sist.backend.dto.master.CustomerDto;
import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RegistrationRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.service.CustomerService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.service.RegistrationRequestService;
import com.sist.backend.service.RoomPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterManagementController {
    
    /* 서비스 호출 */
    private final HotelInfoService hotelInfoService;
    private final RoomPaymentService roomPaymentService;
    private final RegistrationRequestService registrationRequestService;
    private final CustomerService customerService;

   
    /* 등록되어 있는 회원의 목록 */
    @GetMapping("/customers")
    @Operation(summary = "마스터 회원 관리", description = "등록되어 있는 회원의 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<CustomerDto>>  findCustomerAndRank(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(customerService.findCustomerAndRankDto(pageable));
    }

    @GetMapping("/hotels")
    @Operation(summary = "마스터 호텔 관리", description = "등록되어 있는 호텔의 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<HotelInfoDto>>  findAllHotelWithDetailsAsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(hotelInfoService.findAllHotelWithDetailsAsDto(pageable));
    }

    /* 승인요청을 한 호텔들 */
    @RequestMapping("/hotelApproval")
    @Operation(summary = "승인요청 관리", description = "승인요청을 한 호텔 목록을 보여줍니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<RegistrationRequestDto>> findAllHotelWithDetailsDto(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") 
            @RequestParam(value = "page", defaultValue = "0") int page, 
            @Parameter(description = "페이지당 데이터 개수", example = "5") 
            @RequestParam(value = "size", defaultValue = "5") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(registrationRequestService.findByStatusDto(pageable));
    }

    /* 대시보드 */
    @RequestMapping("/dashboard")
    @Operation(summary = "대시보드 마스터", description = "마스터 대시보드 화면")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 조회됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public Map<String, Object> dashboard() {
        int HotelCount = hotelInfoService.findRegistrationHotelCount();
        List<RegistrationRequest> pendingRequests = registrationRequestService.findByStatusInDashboard();
        int CustomerCount = customerService.findRegistrationCustomerCount();
        List<Customer> newCustomers = customerService.findByJoinDate();
        Long paymentAmount = roomPaymentService.findByPrice();
        Map<String, Object> map = new HashMap<>();

        if(HotelCount >= 0) {
            map.put("hotelCount", HotelCount);
        }
        if(CustomerCount >= 0) {
            map.put("customerCount", CustomerCount);
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
