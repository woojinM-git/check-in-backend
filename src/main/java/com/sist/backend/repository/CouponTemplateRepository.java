package com.sist.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.CouponTemplate;

@Repository
public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Integer> {

    /* 모든 템플릿 조회 */
    List<CouponTemplate> findAll();

    /* 활성화 되어 있는 템플릿 조회 */
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.status = 1")
    List<CouponTemplate> findByStatus();

    /* 활성화 되어 있고 특정 타입인 템플릿 조회 */
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.status = 1 AND ct.type = :type")
    List<CouponTemplate> findByStatusAndType(@org.springframework.data.repository.query.Param("type") Integer type);

    /* 비활성화 되어 있는 템플릿 조회 */
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.status = 0 ORDER BY ct.createdAt DESC")
    List<CouponTemplate> findByInactiveStatus();
}
