package com.sist.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.admin.CustomerListDto;
import com.sist.backend.dto.master.CustomerDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.RoomPaymentRepository;
import com.sist.backend.repository.RoomReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final RoomPaymentRepository roomPaymentRepository;

    public int findRegistrationCustomerCount(){
        return customerRepository.findRegistrationCustomerCount();
    }
    public Optional<Customer> findById(String id){
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByIdAndStatus(String id, Integer status){
        return customerRepository.findByIdAndStatus(id, status);
    }
    public Optional<Customer> findByNicknameAndStatus(String nickname, Integer status){
        return customerRepository.findByNicknameAndStatus(nickname, status);
    }

    public Optional<Customer> findByRefTokenAndId(String tokenID, String id){
        return customerRepository.findByRefTokenAndId(tokenID, id);
    }

    public Optional<Customer> findByCustomerIdx(Integer customerIdx){
        return customerRepository.findByCustomerIdx(customerIdx);
    }
        
    /* 대시보드의 최근 가입한 고객 목록 (상위 5개) */
    public List<Customer> findByJoinDate(){
        return customerRepository.findByJoinDate();
    }

    public Page<CustomerDto> findCustomerAndRankDto(Pageable pageable){
        Page<Customer> customerPage = customerRepository.findCustomerAndRank(pageable);
        return customerPage.map(CustomerDto::fromEntity);
    }

    public Customer save(Customer customer){
        return customerRepository.save(customer);
    }

    public List<Customer> findAll(){
        return customerRepository.findAll();
    }
    
    // 닉네임으로 고객 검색
    public List<Customer> findByNicknameContaining(String nickname) {
        return customerRepository.findByNicknameContaining(nickname);
    }

    // 해당 호텔을 이용한 고객 중에서 검색어로 필터링
    public List<Customer> findByContentIdAndSearchTerm(String contentId, String searchTerm) {
        return customerRepository.findByContentIdAndSearchTerm(contentId, searchTerm);
    }

    public Optional<Customer> findByEmailAndStatus(String email, Integer status) {
        return customerRepository.findByEmailAndStatus(email, status);
    }
    public Optional<Customer> findByCustomerIdxAndStatus(Integer customerIdx, Integer status) {
        return customerRepository.findByCustomerIdxAndStatus(customerIdx, status);
    }

    /* 특정 호텔을 이용한 고객 목록 조회 (예약 통계 포함) */
    public List<CustomerListDto> findCustomersByContentId(String contentId) {
        // 특정 호텔을 이용한 고객들의 customerIdx 목록 조회
        List<Integer> customerIdxList = roomReservationRepository.findByStatus(contentId)
            .stream()
            .map(r -> r.getCustomerIdx())
            .distinct()
            .collect(Collectors.toList());

        return customerIdxList.stream()
            .map(customerIdx -> {
                Optional<Customer> customerOpt = customerRepository.findByCustomerIdx(customerIdx);
                if (customerOpt.isEmpty()) {
                    return null;
                }
                Customer customer = customerOpt.get();

                // 예약 횟수
                long reservationCount = roomReservationRepository.findByCustomerIdxAndStatus(customerIdx, 1)
                    .stream()
                    .filter(r -> r.getContentid().equals(contentId))
                    .count();

                // 총 결제 금액 (RoomPayment에서 가져오기)
                Long totalPaymentAmount = roomPaymentRepository.findAllByContentIdWithReservations(contentId)
                    .stream()
                    .filter(rp -> rp.getCustomerIdx().equals(customerIdx) && rp.getStatus() == 1)
                    .mapToLong(rp -> rp.getPrice() != null ? rp.getPrice() : 0L)
                    .sum();

                // 최근 방문 날짜
                java.time.LocalDate lastVisitDate = roomReservationRepository
                    .findLastVisitDateByCustomerAndContentId(customerIdx, contentId);

                // 체크인 날짜 목록
                List<java.time.LocalDate> visitedDates = roomReservationRepository
                    .findCheckinDatesByCustomerAndContentId(customerIdx, contentId);

                CustomerListDto dto = new CustomerListDto();
                dto.setCustomerIdx(customer.getCustomerIdx());
                dto.setId(customer.getId());
                dto.setName(customer.getName());
                dto.setEmail(customer.getEmail());
                dto.setPhone(customer.getPhone());
                dto.setReservationCount(reservationCount);
                dto.setTotalPaymentAmount(totalPaymentAmount);
                dto.setLastVisitDate(lastVisitDate);
                dto.setRank(customer.getRank());
                dto.setVisitedDates(visitedDates);

                return dto;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
}
