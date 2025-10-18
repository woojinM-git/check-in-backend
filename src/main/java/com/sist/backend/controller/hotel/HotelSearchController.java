package com.sist.backend.controller.hotel;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.entity.HotelInfo;
import com.sist.backend.service.hotel.HotelSearchService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/hotel")
public class HotelSearchController {

    @Autowired
    private HotelSearchService hotelSearchService;
    
    @PostMapping("/search")
    public ResponseEntity<List<HotelInfo>> searchHotels(@RequestBody HotelInfo request){
        List<HotelInfo> hotels = hotelSearchService.findAll();
        return ResponseEntity.ok(hotels);
    }

}
