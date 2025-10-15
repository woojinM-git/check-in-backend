package com.sist.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.Admin;

@Repository
public interface AdminManagementRepository extends JpaRepository<Admin, Integer> {
    /*
     * 관리자 화면(admin폴더)에 있는 정보들을 표현하 위해 데이터베이스와 통신하는 객체
     */
    Optional<Admin> findByadminIdx(Integer adminIdx);
}
