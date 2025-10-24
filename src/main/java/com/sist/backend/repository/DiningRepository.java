package com.sist.backend.repository;

import com.sist.backend.entity.Dining;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface DiningRepository extends JpaRepository<Dining, Integer> {
    
    /**
     * 활성화된 다이닝 목록 조회 (업데이트 시간 순)
     */
    @Query("SELECT d FROM Dining d " +
           "WHERE d.status = 1 " +
           "ORDER BY d.updatedAt DESC")
    Page<Dining> findActiveDinings(Pageable pageable);
    
    /**
     * 호텔별 다이닝 목록 조회
     */
    @Query("SELECT d FROM Dining d " +
           "WHERE d.contentid = :contentid " +
           "AND d.status = 1 " +
           "ORDER BY d.openTime ASC")
    List<Dining> findByContentidAndStatus(@Param("contentid") String contentid);
    
    /**
     * 다이닝 검색 (호텔 주소, 호텔 이름, 다이닝 이름으로 검색)
     */
    @Query("SELECT d FROM Dining d " +
           "LEFT JOIN d.hotelInfo h " +
           "WHERE d.status = 1 " +
           "AND (:destination IS NULL OR " +
           "     h.adress LIKE CONCAT('%', :destination, '%') OR " +
           "     h.title LIKE CONCAT('%', :destination, '%') OR " +
           "     d.name LIKE CONCAT('%', :destination, '%')) " +
           "ORDER BY d.updatedAt DESC")
    Page<Dining> searchDinings(@Param("destination") String destination, Pageable pageable);
    
    /**
     * 가격 범위로 다이닝 검색
     */
    @Query("SELECT d FROM Dining d " +
           "WHERE d.status = 1 " +
           "AND (:priceMin IS NULL OR d.basePrice >= :priceMin) " +
           "AND (:priceMax IS NULL OR d.basePrice <= :priceMax) " +
           "ORDER BY d.basePrice ASC")
    Page<Dining> findByPriceRange(@Param("priceMin") Integer priceMin, 
                                  @Param("priceMax") Integer priceMax, 
                                  Pageable pageable);
    
    /**
     * 식사 시간대별 다이닝 검색
     */
    @Query("SELECT d FROM Dining d " +
           "WHERE d.status = 1 " +
           "AND (:mealType IS NULL OR " +
           "     (:mealType = 'breakfast' AND d.openTime <= :time AND d.closeTime >= :time) OR " +
           "     (:mealType = 'lunch' AND d.openTime <= :time AND d.closeTime >= :time) OR " +
           "     (:mealType = 'dinner' AND d.openTime <= :time AND d.closeTime >= :time)) " +
           "ORDER BY d.openTime ASC")
    Page<Dining> findByMealType(@Param("mealType") String mealType,
                                @Param("time") LocalTime time,
                                Pageable pageable);
    
    /**
     * 복합 조건 검색 (지역 + 가격 + 식사시간)
     */
    @Query("SELECT d FROM Dining d " +
           "LEFT JOIN d.hotelInfo h " +
           "WHERE d.status = 1 " +
           "AND (:destination IS NULL OR " +
           "     h.adress LIKE CONCAT('%', :destination, '%') OR " +
           "     h.title LIKE CONCAT('%', :destination, '%') OR " +
           "     d.name LIKE CONCAT('%', :destination, '%')) " +
           "AND (:priceMin IS NULL OR d.basePrice >= :priceMin) " +
           "AND (:priceMax IS NULL OR d.basePrice <= :priceMax) " +
           "AND (:mealType IS NULL OR " +
           "     (:mealType = 'breakfast' AND d.openTime <= :time AND d.closeTime >= :time) OR " +
           "     (:mealType = 'lunch' AND d.openTime <= :time AND d.closeTime >= :time) OR " +
           "     (:mealType = 'dinner' AND d.openTime <= :time AND d.closeTime >= :time)) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'price' AND :sortDirection = 'asc' THEN d.basePrice END ASC, " +
           "CASE WHEN :sortBy = 'price' AND :sortDirection = 'desc' THEN d.basePrice END DESC, " +
           "CASE WHEN :sortBy = 'time' AND :sortDirection = 'asc' THEN d.openTime END ASC, " +
           "CASE WHEN :sortBy = 'time' AND :sortDirection = 'desc' THEN d.openTime END DESC, " +
           "d.updatedAt DESC")
    Page<Dining> searchDiningsWithFilters(@Param("destination") String destination,
                                         @Param("priceMin") Integer priceMin,
                                         @Param("priceMax") Integer priceMax,
                                         @Param("mealType") String mealType,
                                         @Param("time") LocalTime time,
                                         @Param("sortBy") String sortBy,
                                         @Param("sortDirection") String sortDirection,
                                         Pageable pageable);
}