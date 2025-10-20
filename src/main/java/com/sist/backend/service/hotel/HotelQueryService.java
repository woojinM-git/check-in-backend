package com.sist.backend.service.hotel;

import com.sist.backend.dto.hotel.HotelResponse;
import com.sist.backend.dto.hotel.RoomResponse;
import com.sist.backend.entity.HotelDetail;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.Room;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.mapper.hotel.RoomAdvancedMapper;
import com.sist.backend.repository.hotel.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelQueryService {

    private final HotelInfoRepository hotelInfoRepository;
    private final RoomRepository roomRepository;
    private final RoomAdvancedMapper roomAdvancedMapper;

    // 호텔 상세 조회 (JPA)
    public Optional<HotelResponse> getHotel(String contentId) {
        return hotelInfoRepository.findById(contentId)
                .map(this::mapHotel);
    }

    // 객실 목록 조회 (JPA) - 이름(name)으로 부분검색 가능
    public List<RoomResponse> getRooms(String contentId, String name) {
        List<Room> rooms;
        if (name == null || name.isBlank()) {
            rooms = roomRepository.findByContentId(contentId);
        } else {
            rooms = roomRepository.findByContentIdAndNameContainingIgnoreCase(contentId, name);
        }
        return rooms.stream().map(this::mapRoom).collect(Collectors.toList());
    }

    // 객실 고급 검색 (MyBatis) - 동적 조건(이름/최소/최대 수용인원)
    public List<RoomResponse> searchRoomsAdvanced(String contentId, String name, Integer minCapacity, Integer maxCapacity) {
        List<Room> rooms = roomAdvancedMapper.searchRooms(contentId, name, minCapacity, maxCapacity);
        return rooms.stream().map(this::mapRoom).collect(Collectors.toList());
    }

    // 엔티티(HotelInfo, HotelDetail) -> 응답 DTO 매핑
    private HotelResponse mapHotel(HotelInfo info) {
        HotelDetail detail = info.getHotelDetail();
        return HotelResponse.builder()
                .contentId(info.getContentId())
                .title(info.getTitle())
                .adress(info.getAdress())
                .imageUrl(info.getImageUrl())
                .areaCode(info.getAreaCode())
                .roomcount(detail != null ? detail.getRoomcount() : null)
                .foodplace(detail != null ? detail.getFoodplace() : null)
                .parkinglodging(detail != null ? detail.getParkinglodging() : null)
                .reservationlodging(detail != null ? detail.getReservationlodging() : null)
                .scalelodging(detail != null ? detail.getScalelodging() : null)
                .build();
    }

    private RoomResponse mapRoom(Room room) {
        // 조식 판단 규칙:
        // - HotelDetail.foodplace 가 null/빈값이면 "조식 없음(false)"
        // - 값이 존재하면 "조식 가능(true)"
        // - HotelDetail 이 없고, Room.breakfastIncluded 가 있으면 그 값을 사용
        Boolean breakfast = null;
        HotelDetail detail = room.getHotelInfo() != null ? room.getHotelInfo().getHotelDetail() : null;
        if (detail != null) {
            breakfast = (detail.getFoodplace() != null && !detail.getFoodplace().isBlank());
        } else if (room.getBreakfastIncluded() != null) {
            // fallback to room flag if present
            breakfast = room.getBreakfastIncluded();
        }

        // 흡연 가능 여부:
        // - 현재 정확한 판별이 어려움 → Room.smoking 값이 있으면 사용, 없으면 null 유지
        Boolean smoking = (room.getSmoking() != null) ? room.getSmoking() : null;

        return RoomResponse.builder()
                .contentId(room.getContentId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .refundable(room.getRefundable())
                .breakfastIncluded(breakfast)
                .smoking(smoking)
                .imageUrl(room.getImageUrl())
                .build();
    }
}
