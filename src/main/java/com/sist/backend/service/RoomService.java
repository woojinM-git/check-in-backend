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
}
