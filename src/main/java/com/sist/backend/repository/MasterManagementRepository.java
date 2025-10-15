package com.sist.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Admin;

@Repository
public interface MasterManagementRepository extends JpaRepository<Admin, Integer> {
    
}
