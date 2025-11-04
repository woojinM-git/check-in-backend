package com.sist.backend.repository.hotel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelInfo;



@Repository
public interface hotelSearchRepository extends JpaRepository<HotelInfo, String> {
    
    List<HotelInfo> findAll();

    @Query(value = 
    "SELECT h.contentId, h.title, h.adress, h.tel, h.hotelCategoryCode, h.areaCode, h.imageUrl, h.status, h.adminIdx " +
    "FROM hotelInfo h " +
    "WHERE (:searchPattern IS NOT NULL AND :searchPattern != '') " +
    "  AND (" +
    "    (h.title IS NOT NULL AND h.title LIKE :searchPattern) " +
    "    OR (h.adress IS NOT NULL AND h.adress LIKE :searchPattern)" +
    "  ) " +
    "  AND (:hasDining IS NULL OR :hasDining = false OR " +
    "    (:hasDining = true AND EXISTS (SELECT 1 FROM dining d WHERE d.contentid = h.contentId AND d.status = 1))) " +
    "ORDER BY " +
    "  CASE " +
    "    WHEN h.title IS NOT NULL AND h.title LIKE :searchPattern " +
    "         AND h.adress IS NOT NULL AND h.adress LIKE :searchPattern THEN 1 " +
    "    WHEN h.title IS NOT NULL AND h.title LIKE :searchPattern THEN 2 " +
    "    WHEN h.adress IS NOT NULL AND h.adress LIKE :searchPattern THEN 3 " +
    "    ELSE 4 " +
    "  END ASC, " +
    "  h.title ASC",
    nativeQuery = true)
    List<HotelInfo> findByTitle(@Param("searchPattern") String searchPattern, @Param("hasDining") Boolean hasDining);
}
