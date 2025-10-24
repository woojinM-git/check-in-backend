package com.sist.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
