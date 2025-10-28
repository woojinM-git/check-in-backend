package com.sist.backend.repository;

import com.sist.backend.dto.admin.CouponDto;
import com.sist.backend.entity.Coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer>{
    
    // 고객별 쿠폰 조회
    List<Coupon> findByCustomerIdx(Integer customerIdx);
    
    // 템플릿별 쿠폰 조회
    List<Coupon> findByTemplateIdx(Integer templateIdx);
    
    // 상태별 쿠폰 조회
    List<Coupon> findByStatus(Boolean status);

    // 관리자별 쿠폰 조회
    @Query("SELECT c FROM Coupon c " +
            "LEFT JOIN FETCH c.couponTemplate " +
            "LEFT JOIN FETCH c.customer " +
            "WHERE c.adminIdx = :adminIdx")
    Page<Coupon> findByAdminIdx(Integer adminIdx, Pageable pageable);
}
