package com.sist.backend.service.hotel;

import com.sist.backend.entity.HotelInfo;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sist.backend.repository.hotel.hotelSearchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    
    private final hotelSearchRepository hotelSearchRepository;

    public List<HotelInfo> findByTitle(String title){
        return hotelSearchRepository.findByTitle(title);
    }

    public List<HotelInfo> findAll(){
        return hotelSearchRepository.findAll();
    }
}
