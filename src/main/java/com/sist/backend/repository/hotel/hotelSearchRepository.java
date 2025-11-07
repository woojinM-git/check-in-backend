package com.sist.backend.repository.hotel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelInfo;



@Repository
public interface hotelSearchRepository extends JpaRepository<HotelInfo, String> {
    
    Page<HotelInfo> findAll(Pageable pageable);
    
    @Query(value = 
    "SELECT h.contentId, h.title, h.adress, h.tel, h.hotelCategoryCode, h.areaCode, h.imageUrl, h.status, h.adminIdx ,h.count " +
    "FROM hotelInfo h " +
    "WHERE (:searchPattern IS NULL OR :searchPattern = '' OR " +
    "  ((h.title IS NOT NULL AND h.title LIKE :searchPattern) " +
    "    OR (h.adress IS NOT NULL AND h.adress LIKE :searchPattern))) " +
    "  AND (:hasDining IS NULL OR :hasDining = false OR " +
    "    (:hasDining = true AND EXISTS (SELECT 1 FROM dining d WHERE d.contentid = h.contentId AND d.status = 1))) " +
    "ORDER BY " +
    "  CASE " +
    "    WHEN :searchPattern IS NOT NULL AND :searchPattern != '' AND " +
    "         h.title IS NOT NULL AND h.title LIKE :searchPattern " +
    "         AND h.adress IS NOT NULL AND h.adress LIKE :searchPattern THEN 1 " +
    "    WHEN :searchPattern IS NOT NULL AND :searchPattern != '' AND " +
    "         h.title IS NOT NULL AND h.title LIKE :searchPattern THEN 2 " +
    "    WHEN :searchPattern IS NOT NULL AND :searchPattern != '' AND " +
    "         h.adress IS NOT NULL AND h.adress LIKE :searchPattern THEN 3 " +
    "    ELSE 4 " +
    "  END ASC, " +
    "  h.title ASC",
    countQuery = 
    "SELECT COUNT(h.contentId) " +
    "FROM hotelInfo h " +
    "WHERE (:searchPattern IS NULL OR :searchPattern = '' OR " +
    "  ((h.title IS NOT NULL AND h.title LIKE :searchPattern) " +
    "    OR (h.adress IS NOT NULL AND h.adress LIKE :searchPattern))) " +
    "  AND (:hasDining IS NULL OR :hasDining = false OR " +
    "    (:hasDining = true AND EXISTS (SELECT 1 FROM dining d WHERE d.contentid = h.contentId AND d.status = 1)))",
    nativeQuery = true)
    Page<HotelInfo> findByTitleWithPagination(@Param("searchPattern") String searchPattern, @Param("hasDining") Boolean hasDining, Pageable pageable);
}
