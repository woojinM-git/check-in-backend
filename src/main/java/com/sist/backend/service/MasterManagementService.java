package com.sist.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sist.backend.repository.MasterManagementRepository;

@Service
public class MasterManagementService {
    
    @Autowired
    MasterManagementRepository masterManagementRepository;
}
