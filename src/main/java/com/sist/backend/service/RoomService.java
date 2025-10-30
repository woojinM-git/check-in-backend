package com.sist.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sist.backend.entity.Room;
import com.sist.backend.repository.hotel.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

     /* room 목록 */
    public List<Room> findByContentIdAdmin(String contentid) {
        return roomRepository.findByContentIdAdmin(contentid);
    }

    /* room 정보 수정 */
    public Room updateRoom(Integer roomIdx, String name, Integer capacity, Integer basePrice) {
        Room room = roomRepository.findByRoomIdx(roomIdx).orElseThrow(() -> new RuntimeException("객실을 찾을 수 없습니다."));
        room.setName(name);
        room.setCapacity(capacity);
        room.setBasePrice(basePrice);
        return roomRepository.save(room);
    }
    
    /* room 비활성화 */
    public Room updateRoomStatus(Integer roomIdx, Integer status) {
        Room room = roomRepository.findByRoomIdx(roomIdx).orElseThrow(() -> new RuntimeException("객실을 찾을 수 없습니다."));
        room.setStatus(status);
        return roomRepository.save(room);
    }
}
