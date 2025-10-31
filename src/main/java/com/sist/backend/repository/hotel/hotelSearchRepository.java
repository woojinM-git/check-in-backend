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
    "SELECT hotel.contentId, hotel.title, hotel.adress, hotel.tel, hotel.hotelCategoryCode, hotel.areaCode, hotel.imageUrl, hotel.status, hotel.adminIdx "+
    "FROM("+
    "SELECT h.*, MATCH(h.title) AGAINST(:searchWord IN NATURAL LANGUAGE MODE) AS score " +
    "FROM hotelInfo h " +
    "WHERE MATCH(h.title) AGAINST(:searchWord IN NATURAL LANGUAGE MODE) " +
    "ORDER BY score DESC "+
    ") hotel",
    nativeQuery = true)
    List<HotelInfo> findByTitle(@Param("searchWord") String searchWord);
}
