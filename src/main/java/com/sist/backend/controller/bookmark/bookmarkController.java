package com.sist.backend.controller.bookmark;


import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.HotelBookMark;
import com.sist.backend.entity.RoomBookMark;
import com.sist.backend.service.bookmark.HoterBookmarkService;
import com.sist.backend.service.bookmark.RoomBookmarkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/bookmark")
@Tag(name = "북마크", description = "북마크 API")
@RequiredArgsConstructor
@Slf4j
public class bookmarkController {
    
    @Value("${bookmark.numPerPage}")
    private int numPerPage;
    
    private final HoterBookmarkService hotelBookmarkService;
    private final RoomBookmarkService roomBookmarkService;
    
    @GetMapping("/hotelbookmark/save")
    @Operation(summary = "호텔 즐겨찾기 저장", description = "호텔 즐겨찾기를 저장합니다.")
    public ResponseEntity<String> saveHotelBookmark(@RequestParam("contentId") String contentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        HotelBookMark hotelBookMark = new HotelBookMark();
        hotelBookMark.setCustomerIdx(customer.getCustomerIdx());
        hotelBookMark.setContentId(contentId);
        HotelBookMark savedHotelBookMark = hotelBookmarkService.saveHotelBookmark(hotelBookMark);
        if(savedHotelBookMark != null) {
            return ResponseEntity.ok("hotelbookmark save success");
        } else {
            return ResponseEntity.badRequest().body("hotelbookmark save failed");
        }
    }

    @GetMapping("/hotelbookmark/delete")
    @Operation(summary = "호텔 즐겨찾기 삭제", description = "호텔 즐겨찾기를 삭제합니다.")
    public ResponseEntity<String> deleteHotelBookmark(@RequestParam("contentId") String contentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        hotelBookmarkService.deleteHotelBookmark(contentId, customer.getCustomerIdx());
        return ResponseEntity.ok("hotelbookmark delete success");
    }

    @GetMapping("/hotelbookmark/list")
    @Operation(summary = "호텔 즐겨찾기 목록", description = "호텔 즐겨찾기 목록을 조회합니다.")
    public ResponseEntity<List<HotelBookMark>> getHotelBookmarkList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        List<HotelBookMark> hotelBookmarkList = hotelBookmarkService.getHotelBookmarkList(customer.getCustomerIdx());
        return ResponseEntity.ok(hotelBookmarkList);
    }

    @GetMapping("/hotelbookmark/pagelist")
    @Operation(summary = "호텔 즐겨찾기 페이지별 목록", description = "호텔 즐겨찾기 페이지별 목록을 조회합니다.")
    public ResponseEntity<Page<HotelBookMark>> getHotelBookmarkPagePerList(@RequestParam("page") int page) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Page<HotelBookMark> hotelBookmarkPage = hotelBookmarkService.getHotelBookmarkPagePerList(customer.getCustomerIdx(), page, numPerPage);
        // int totalPages = hotelBookmarkPage.getTotalPages();
        // long totalElements = hotelBookmarkPage.getTotalElements();
        return ResponseEntity.ok(hotelBookmarkPage);
    }

    @GetMapping("/roombookmark/save")
    @Operation(summary = "방 즐겨찾기 저장", description = "방 즐겨찾기를 저장합니다.")
    public ResponseEntity<String> saveRoomBookmark(@RequestParam("roomIdx") Integer roomIdx,@RequestParam("contentId") String contentid) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        RoomBookMark roomBookMark = new RoomBookMark();
        roomBookMark.setCustomerIdx(customer.getCustomerIdx());
        roomBookMark.setRoomIdx(roomIdx);
        roomBookMark.setContentid(contentid);
        RoomBookMark savedRoomBookMark = roomBookmarkService.saveRoomBookmark(roomBookMark);
        if(savedRoomBookMark != null) {
            return ResponseEntity.ok("roombookmark save success");
        } else {
            return ResponseEntity.badRequest().body("roombookmark save failed");
        }
    }

    @GetMapping("/roombookmark/delete")
    @Operation(summary = "방 즐겨찾기 삭제", description = "방 즐겨찾기를 삭제합니다.")
    public ResponseEntity<String> deleteRoomBookmark(@RequestParam("roomIdx") Integer roomIdx) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        roomBookmarkService.deleteRoomBookmark(roomIdx, customer.getCustomerIdx());
        return ResponseEntity.ok("roombookmark delete success");
    }

    @GetMapping("/roombookmark/list")
    @Operation(summary = "방 즐겨찾기 목록", description = "방 즐겨찾기 목록을 조회합니다.")
    public ResponseEntity<List<RoomBookMark>> getRoomBookmarkList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        List<RoomBookMark> roomBookmarkList = roomBookmarkService.getRoomBookmarkList(customer.getCustomerIdx());
        return ResponseEntity.ok(roomBookmarkList);
    }

    @GetMapping("/roombookmark/onelist")
    @Operation(summary = "한 호텔에 있는 방 목록", description = "해당 호텔에 대한 방 목록을 반환합니다")
    public ResponseEntity<List<RoomBookMark>> getRoomBookmarkOneList(@RequestParam("contentId") String contentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO customer = (CustomerAdminSignupDTO) authentication.getPrincipal();
        List<RoomBookMark> roomBookmarkList = roomBookmarkService.getRoomBookmarkOneList(contentId, customer.getCustomerIdx());
        return ResponseEntity.ok(roomBookmarkList);
    }
    



}
