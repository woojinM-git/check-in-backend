package com.sist.backend.repository.hotel;

import com.sist.backend.entity.HotelLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelLocationRepository extends JpaRepository<HotelLocation, Integer> {
}
