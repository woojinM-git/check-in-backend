package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;

import java.util.List;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sist.backend.repository.hotel.HotelInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    
    private final HotelInfoRepository hotelInfoRepository;

    public List<HotelInfo> findAll(){
        return hotelInfoRepository.findAll();
    }
    public List<HotelInfo> findAllPopularHotels() {
        Pageable pageable = PageRequest.of(0, 9);
        return hotelInfoRepository.findTop9WithCategoryAndArea(pageable);
    }
}
