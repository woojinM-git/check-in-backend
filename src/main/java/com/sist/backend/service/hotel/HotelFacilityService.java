package com.sist.backend.service.hotel;

import com.sist.backend.dto.hotel.HotelFacilityResponse;
import com.sist.backend.entity.HotelDetail;
import com.sist.backend.entity.HotelLocation;
import com.sist.backend.repository.hotel.HotelDetailRepository;
import com.sist.backend.repository.hotel.HotelLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelFacilityService {

    private final HotelDetailRepository hotelDetailRepository;
    private final HotelLocationRepository hotelLocationRepository;

    /**
     * 호텔 편의시설 및 위치 정보 조회
     *
     * @param contentId 호텔 contentId
     * @return 호텔 편의시설 및 위치 정보
     */
    public Optional<HotelFacilityResponse> getHotelFacilities(String contentId) {
        // HotelDetail 조회
        Optional<HotelDetail> hotelDetailOpt = hotelDetailRepository.findById(contentId);
        if (hotelDetailOpt.isEmpty()) {
            return Optional.empty();
        }

        HotelDetail hotelDetail = hotelDetailOpt.get();

        // HotelLocation 조회
        List<HotelLocation> locations = hotelLocationRepository.findAll();
        Optional<HotelLocation> hotelLocationOpt = locations.stream()
                .filter(location -> contentId.equals(location.getContentId()))
                .findFirst();

        // DTO 매핑
        HotelFacilityResponse response = HotelFacilityResponse.builder()
                .contentId(hotelDetail.getContentid())
                .parkinglodging(hotelDetail.getParkinglodging())
                .foodplace(hotelDetail.getFoodplace())
                .reservationlodging(hotelDetail.getReservationlodging())
                .scalelodging(hotelDetail.getScalelodging())
                .mapX(hotelLocationOpt.map(HotelLocation::getMapX).orElse(null))
                .mapY(hotelLocationOpt.map(HotelLocation::getMapY).orElse(null))
                .build();

        return Optional.of(response);
    }
}
