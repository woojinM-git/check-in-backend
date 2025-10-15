package com.sist.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sist.backend.entity.Admin;
import com.sist.backend.repository.AdminManagementRepository;

@Service
public class AdminManagementService {
    
    @Autowired
    AdminManagementRepository adminManagementRepository;

    public List<Admin> findAll(){
        return adminManagementRepository.findAll();
    }

    public Optional<Admin> findByadminIdx(Integer adminIdx){
        return adminManagementRepository.findByadminIdx(adminIdx);
    }
}
