package com.sist.backend.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomPayment;

@Repository
public interface RoomPaymentRepository extends JpaRepository<RoomPayment, Integer> {

    @Query("SELECT SUM(price) FROM RoomPayment WHERE status = 1")
    Long findByPrice();
    
    @Query("SELECT rp FROM RoomPayment rp WHERE rp.paymentKey = :paymentKey AND rp.status = 1")
    Optional<RoomPayment> findByPaymentKeyAndStatus(String paymentKey);

}
