package com.sist.backend.repository.hotel;

import com.sist.backend.entity.HotelDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelDetailRepository extends JpaRepository<HotelDetail, String> {
}
