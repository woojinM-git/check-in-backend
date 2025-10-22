package com.sist.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    @Query("SELECT COUNT(c) FROM Customer c " +
        "WHERE c.status = 0")
    int findRegistrationCustomerCount();

    @Query(value = "SELECT * FROM customer WHERE joinDate >= CURDATE() AND joinDate < DATE_ADD(CURDATE(), INTERVAL 1 DAY)", nativeQuery = true)
    List<Customer> findByJoinDate();

    Optional<Customer> findById(String id);

    Optional<Customer> findByCustomerIdx(Integer customerIdx);

    @Query("SELECT c FROM Customer c " +
        "LEFT JOIN FETCH c.rankEntity")
    Page<Customer> findCustomerAndRank(Pageable pageable);
}
