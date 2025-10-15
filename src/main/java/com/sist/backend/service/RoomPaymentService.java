package com.sist.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sist.backend.repository.RoomPaymentRepository;

@Service
public class RoomPaymentService {

    @Autowired
    RoomPaymentRepository roomPaymentRepository;

    public Long findByPrice() {
        return roomPaymentRepository.findByPrice();
    }
}
