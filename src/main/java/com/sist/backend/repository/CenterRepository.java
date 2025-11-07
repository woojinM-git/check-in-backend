package com.sist.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.sist.backend.entity.Center;

@Repository
public interface CenterRepository extends JpaRepository<Center, Integer> {
    
    // 카테고리별 조회
    Page<Center> findByMainCategory(String mainCategory, Pageable pageable);
    
    // 세부 카테고리별 조회
    Page<Center> findBySubCategory(String subCategory, Pageable pageable);
    
    // 상태별 조회
    Page<Center> findByStatus(Integer status, Pageable pageable);
    
    // 우선순위별 조회
    Page<Center> findByPriority(Integer priority, Pageable pageable);
    
    // 고객별 조회
    Page<Center> findByCustomerIdx(Integer customerIdx, Pageable pageable);
    
    // 관리자별 조회
    Page<Center> findByAdminIdx(Integer adminIdx, Pageable pageable);
    
    // 제목 검색
    @Query("SELECT c FROM Center c WHERE c.title LIKE %:title%")
    Page<Center> findByTitleContaining(@Param("title") String title, Pageable pageable);
    
    // 내용 검색
    @Query("SELECT c FROM Center c WHERE c.content LIKE %:content%")
    Page<Center> findByContentContaining(@Param("content") String content, Pageable pageable);
    
    // 복합 검색 (contentId가 "NULL" 문자열이면 IS NULL 조건, contentIdList가 있으면 IN 절, 단일 contentId 값이면 = 조건, null이면 필터링 없음)
    // customerIdx가 -1이면 필터링 없음, 값이 있으면 해당 customerIdx만, null이면 아무것도 반환하지 않음
    @Query("SELECT c FROM Center c WHERE " +
           "(:mainCategory IS NULL OR c.mainCategory = :mainCategory) AND " +
           "(:subCategory IS NULL OR c.subCategory = :subCategory) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:customerIdx = -1 OR c.customerIdx = :customerIdx) AND " +
           "(:adminIdx IS NULL OR c.adminIdx = :adminIdx) AND " +
           "(" +
           "  (:contentIdList IS NOT NULL AND c.contentId IN :contentIdList) OR " +
           "  (:contentIdList IS NULL AND :contentId = 'NULL' AND c.contentId IS NULL) OR " +
           "  (:contentIdList IS NULL AND :contentId IS NOT NULL AND :contentId != 'NULL' AND c.contentId = :contentId) OR " +
           "  (:contentIdList IS NULL AND :contentId IS NULL)" +
           ") AND " +
           "(:title IS NULL OR c.title LIKE %:title% OR c.content LIKE %:title%)")
    Page<Center> findByMultipleConditions(
        @Param("mainCategory") String mainCategory,
        @Param("subCategory") String subCategory,
        @Param("status") Integer status,
        @Param("priority") Integer priority,
        @Param("customerIdx") Integer customerIdx,
        @Param("adminIdx") Integer adminIdx,
        @Param("contentId") String contentId,
        @Param("contentIdList") List<String> contentIdList,
        @Param("title") String title,
        Pageable pageable
    );
    
    // 특정 호텔에 대한 신고 존재 여부 확인 (고객별)
    @Query("SELECT COUNT(c) > 0 FROM Center c WHERE c.mainCategory = '신고' AND c.contentId = :contentId AND c.customerIdx = :customerIdx")
    boolean existsReportByContentIdAndCustomerIdx(@Param("contentId") String contentId, @Param("customerIdx") Integer customerIdx);
}
