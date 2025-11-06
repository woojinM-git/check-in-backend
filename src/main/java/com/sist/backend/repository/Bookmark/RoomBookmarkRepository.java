package com.sist.backend.repository.Bookmark;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomBookMark;

@Repository
public interface RoomBookmarkRepository extends JpaRepository<RoomBookMark, Integer> {
    
    void deleteByRoomIdxAndCustomerIdx(Integer roomIdx, Integer customerIdx);

    List<RoomBookMark> findAllByCustomerIdx(Integer customerIdx);

    List<RoomBookMark> findByContentidAndCustomerIdx(String contentid, Integer customerIdx);
}

