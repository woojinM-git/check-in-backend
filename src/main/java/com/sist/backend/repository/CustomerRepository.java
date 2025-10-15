package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    List<Customer> findAll();
}
