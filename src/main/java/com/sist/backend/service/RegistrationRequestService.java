package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.dto.master.RegistrationRequestPlusDto;
import com.sist.backend.entity.RegistrationRequest;
import com.sist.backend.repository.RegistrationRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {
    
    private final RegistrationRequestRepository rrRepository;

    /* 대시보드의 승인요청 목록 (상위 5개) */
    public List<RegistrationRequestDto> findTop5ByStatusInDashboard() {
        Pageable pageable = Pageable.ofSize(5);
        List<RegistrationRequest> registrationRequests = rrRepository.findTop5ByStatusInDashboard(pageable);
        return registrationRequests.stream()
            .map(RegistrationRequestDto::fromEntity)
            .collect(Collectors.toList());
    }

    /* 승인요청중인 호텔의 수 */
    public Integer findByStatusCount() {
        return rrRepository.findByStatusCount();
    }

    public Page<RegistrationRequestPlusDto> findByStatusDto(Pageable pageable) {
        Page<RegistrationRequest> registrationRequests = rrRepository.findByStatus(pageable);
        return registrationRequests.map(RegistrationRequestPlusDto::fromEntity);
    }

    /* ID로 승인 요청 조회 */
    public RegistrationRequest findById(Integer registrationIdx) {
        Optional<RegistrationRequest> request = rrRepository.findById(registrationIdx);
        if (request.isPresent()) {
            return request.get();
        } else {
            throw new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + registrationIdx);
        }
    }

    /* 승인 요청 업데이트 */
    @org.springframework.transaction.annotation.Transactional
    public void updateRequest(Integer registrationIdx, Integer status, LocalDateTime approvDate) {
        rrRepository.updateRequest(registrationIdx, status, approvDate);
    }

    /* 거부 요청 업데이트 */
    @org.springframework.transaction.annotation.Transactional
    public void updateRejectRequest(Integer registrationIdx, String refusalMsg, Integer status) {
        rrRepository.updateRejectRequest(registrationIdx, status, refusalMsg);
    }

    /* 오늘 승인된 호텔 수 */
    public Integer findTodayApprovedCount() {
        return rrRepository.findTodayApprovedCount();
    }

    /* 오늘 거부된 호텔 수 */
    public Integer findTodayRejectedCount() {
        return rrRepository.findTodayRejectedCount();
    }
}
