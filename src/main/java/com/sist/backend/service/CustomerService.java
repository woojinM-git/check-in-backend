package com.sist.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sist.backend.entity.Customer;
import com.sist.backend.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    
    private final CustomerRepository customerRepository;

    public List<Customer> findAll(){
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(String id){
        return customerRepository.findById(id);
    }
        

    public List<Customer> findByJoinDate(){
        return customerRepository.findByJoinDate();
    }
}
