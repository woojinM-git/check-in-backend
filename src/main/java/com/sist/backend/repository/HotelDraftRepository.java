package com.sist.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelDraft;

@Repository
public interface HotelDraftRepository extends JpaRepository<HotelDraft, Integer> {
    
    // admin_idx로 임시저장 데이터 조회
    Optional<HotelDraft> findByAdminIdx(Integer adminIdx);
    
    // admin_idx로 임시저장 데이터 존재 여부 확인
    boolean existsByAdminIdx(Integer adminIdx);
    
    // admin_idx로 임시저장 데이터 삭제
    @Modifying
    @Query("DELETE FROM HotelDraft hd WHERE hd.adminIdx = :adminIdx")
    void deleteByAdminIdx(@Param("adminIdx") Integer adminIdx);
}
