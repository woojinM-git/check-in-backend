package com.sist.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.entity.RegistrationRequest;
import com.sist.backend.repository.RegistrationRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {
    
    private final RegistrationRequestRepository rrRepository;

    /* 대시보드의 승인요청 목록 (상위 5개) */
    public List<RegistrationRequest> findByStatusInDashboard() {
        return rrRepository.findByStatusInDashboard();
    }

    /* status가 0인 등록 요청 (페이징 처리) */
    public Page<RegistrationRequestDto> findByStatusDto(Pageable pageable) {
        Page<RegistrationRequest> registrationRequestPage = rrRepository.findByStatus(pageable);
        return registrationRequestPage.map(RegistrationRequestDto::fromEntity);
    }
}
