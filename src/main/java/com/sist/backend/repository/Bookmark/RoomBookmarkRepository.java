package com.sist.backend.repository.Bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sist.backend.entity.RoomBookMark;

@Repository
public interface RoomBookmarkRepository extends JpaRepository<RoomBookMark, Integer> {
    
    void deleteByRoomIdxAndCustomerIdx(Integer roomIdx, Integer customerIdx);
}
