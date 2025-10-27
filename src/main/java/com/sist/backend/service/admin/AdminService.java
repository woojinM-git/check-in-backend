package com.sist.backend.service.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sist.backend.entity.Admin;
import com.sist.backend.repository.admin.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    public Admin save(Admin admin){
        return adminRepository.save(admin);
    }

    public Optional<Admin> findByIdAndStatus(String id, Boolean status){
        return adminRepository.findByIdAndStatus(id, status);
    }
}
