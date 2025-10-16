package com.sist.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sist.backend.entity.RegistrationRequest;
import com.sist.backend.repository.RegistrationRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {
    
    private final RegistrationRequestRepository rrRepository;

    // 승인 대기 중인 등록 요청 조회 (호텔 + 사업자)
    public List<RegistrationRequest> findByStatus() {
        return rrRepository.findByStatus();
    }
}
