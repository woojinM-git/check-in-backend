package com.sist.backend.repository.hotel;

import com.sist.backend.entity.Room;
import com.sist.backend.entity.RoomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, RoomId> {
    // contentId(호텔 기본키)로 객실 전체 조회

    List<Room> findByContentId(String contentId);

    // 이름 부분일치(대소문자 무시)로 객실 검색
    List<Room> findByContentIdAndNameContainingIgnoreCase(String contentId, String name);
}
