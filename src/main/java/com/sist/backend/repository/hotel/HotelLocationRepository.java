package com.sist.backend.repository.hotel;

import com.sist.backend.entity.HotelLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelLocationRepository extends JpaRepository<HotelLocation, Integer> {
    Optional<HotelLocation> findByContentId(String contentId);

    List<HotelLocation> findByContentIdIn(List<String> contentIds);
}
