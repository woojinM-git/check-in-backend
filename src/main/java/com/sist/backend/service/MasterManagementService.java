package com.sist.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sist.backend.entity.Admin;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.repository.HotelInfoRepository;
import com.sist.backend.repository.MasterManagementRepository;

@Service
public class MasterManagementService {
    
    @Autowired
    MasterManagementRepository masterManagementRepository;
    
    /* 마스터 화면에서 확인할 수 있는 관리자 목록 */
    public List<Admin> findAllAdmin(Boolean type) {
        return masterManagementRepository.findByType(type);
    }

    
}
