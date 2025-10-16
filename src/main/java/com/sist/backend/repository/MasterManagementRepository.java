package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Admin;

@Repository
public interface MasterManagementRepository extends JpaRepository<Admin, Integer> {
    List<Admin> findByType(Boolean chk);
}
