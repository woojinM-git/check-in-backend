package com.sist.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.master.CustomerDto;
import com.sist.backend.entity.Customer;
import com.sist.backend.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;

    public int findRegistrationCustomerCount(){
        return customerRepository.findRegistrationCustomerCount();
    }

    public Optional<Customer> findById(String id){
        return customerRepository.findById(id);
    }
        
    /* 대시보드의 최근 가입한 고객 목록 (상위 5개) */
    @Query("SELECT c FROM Customer c " +
        "WHERE c.joinDate >= CURDATE() AND c.joinDate < DATE_ADD(CURDATE(), INTERVAL 1 DAY) " +
        "ORDER BY c.joinDate DESC")
    public List<Customer> findByJoinDate(){
        return customerRepository.findByJoinDate();
    }

    public Page<CustomerDto> findCustomerAndRankDto(Pageable pageable){
        Page<Customer> customerPage = customerRepository.findCustomerAndRank(pageable);
        return customerPage.map(CustomerDto::fromEntity);
    }
}
