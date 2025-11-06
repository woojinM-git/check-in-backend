package com.sist.backend.service.bookmark;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.entity.RoomBookMark;
import com.sist.backend.repository.Bookmark.RoomBookmarkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomBookmarkService {

    private final RoomBookmarkRepository roomBookmarkRepository;

    public RoomBookMark saveRoomBookmark(RoomBookMark roomBookMark) {
        return roomBookmarkRepository.save(roomBookMark);
    }

    @Transactional
    public void deleteRoomBookmark(Integer roomIdx, Integer customerIdx) {
        roomBookmarkRepository.deleteByRoomIdxAndCustomerIdx(roomIdx, customerIdx);
    }
}
