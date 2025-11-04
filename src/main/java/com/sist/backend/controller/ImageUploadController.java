package com.sist.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.RoomImage;
import com.sist.backend.service.RoomImageService;
import com.sist.backend.service.hotel.HotelInfoService;
import com.sist.backend.util.S3ImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/imageUpload")
@RequiredArgsConstructor
@Tag(name = "이미지 업로드", description = "이미지 업로드 관련 API")
public class ImageUploadController {
    private final S3ImageService imageService;
    private final RoomImageService roomImageService;
    private final HotelInfoService hotelInfoService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestPart(value = "image", required = false) MultipartFile image){
        String imageUrl = imageService.upload(image);
        return ResponseEntity.ok(imageUrl);
    }

    // ==================== 객실 이미지 관리 ====================

    @PostMapping("/admin/room/{roomIdx}/images")
    @Operation(summary = "객실 이미지 업로드", description = "특정 객실에 이미지를 업로드합니다. (최대 10개)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 업로드됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> uploadRoomImages(
        @Parameter(description = "객실 인덱스", required = true)
        @PathVariable Integer roomIdx,
        @Parameter(description = "이미지 순서 (1-10, 선택사항)", required = false)
        @RequestParam(required = false) Integer imageOrder,
        @Parameter(description = "업로드할 이미지 파일들", required = true)
        @RequestPart("images") List<MultipartFile> images,
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        String contentid = getContentIdOrRedirect();
        if (contentid == null) {
            return createRedirectResponse();
        }
        
        try {
            List<RoomImage> uploadedImages;
            
            if (images.size() == 1 && imageOrder != null) {
                // 단일 이미지, 순서 지정
                RoomImage roomImage = roomImageService.uploadRoomImage(roomIdx, contentid, images.get(0), imageOrder);
                uploadedImages = List.of(roomImage);
            } else {
                // 여러 이미지, 자동 순서 할당
                uploadedImages = roomImageService.uploadRoomImages(roomIdx, contentid, images);
            }
            
            map.put("success", true);
            map.put("message", "이미지가 성공적으로 업로드되었습니다.");
            map.put("images", uploadedImages);
            
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(map);
        }
    }


    /**
     * JWT에서 adminIdx를 추출하고 contentId를 조회
     * contentId가 없으면 메인 화면으로 리다이렉트
     * @return contentId 문자열, 리다이렉트가 필요한 경우 null (이 경우 즉시 리다이렉트 응답 반환 필요)
     */
    private String getContentIdOrRedirect() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        
        if (adminIdx == null) {
            return null; // 리다이렉트 필요
        }
        
        Optional<String> contentIdOpt = hotelInfoService.findContentIdByAdminIdx(adminIdx);
        if (contentIdOpt.isEmpty()) {
            // contentId가 없으면 호텔이 등록되지 않은 관리자이므로 메인 화면으로 리다이렉트
            return null; // 리다이렉트 필요
        }
        
        return contentIdOpt.get();
    }

    /**
     * contentId가 없을 때 프론트엔드에서 리다이렉트할 수 있도록 403 Forbidden 반환
     * 모든 엔드포인트에서 사용할 수 있는 공통 에러 응답
     */
    private ResponseEntity<Map<String, Object>> createRedirectResponse() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("redirect", true);
        map.put("message", "호텔이 등록되지 않은 관리자입니다. 메인 화면으로 이동합니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(map);
    }
}
