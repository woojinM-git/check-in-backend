package com.sist.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomPayment;

@Repository
public interface RoomPaymentRepository extends JpaRepository<RoomPayment, Integer> {

    @Query("SELECT SUM(price) FROM RoomPayment WHERE status = 1")
    Long findByPrice();

}
