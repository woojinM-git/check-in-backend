package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;

import java.util.List;

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
}
