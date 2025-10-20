package com.sist.backend.repository.hotel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.HotelImage;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {

    List<HotelImage> findTop10ByContentIdOrderByIdAsc(String contentId);
}
