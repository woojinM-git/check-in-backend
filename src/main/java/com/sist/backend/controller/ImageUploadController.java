package com.sist.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sist.backend.dto.signup.CustomerAdminSignupDTO;
import com.sist.backend.entity.HotelImage;
import com.sist.backend.repository.hotel.HotelImageRepository;
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
    private final HotelImageRepository hotelImageRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestPart(value = "image", required = false) MultipartFile image){
        String imageUrl = imageService.upload(image);
        return ResponseEntity.ok(imageUrl);
    }

    // ==================== 호텔 등록용 이미지 업로드 (contentId 없음) ====================
    
    @PostMapping("/hotel/images/register")
    @Operation(summary = "호텔 등록 시 이미지 업로드", description = "호텔 등록 시 이미지를 S3에 업로드하고 DB에 저장합니다. (contentId는 NULL)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 업로드됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> uploadHotelImagesForRegistration(
        @Parameter(description = "업로드할 이미지 파일들", required = true)
        @RequestPart("images") List<MultipartFile> images,
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        // 인증 체크
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
        }
        
        try {
            // S3에 업로드하고 전체 URL 반환
            List<Map<String, Object>> uploadedImages = new java.util.ArrayList<>();
            String folderPath = "hotelmain/hotel"; // 호텔 메인 이미지 폴더
            
            for (MultipartFile image : images) {
                String imageUrl = imageService.upload(image, folderPath, false); // 전체 URL 반환
                
                Map<String, Object> imageInfo = new HashMap<>();
                
                // 등록 페이지: DB에 저장하되 contentId는 NULL (호텔 등록 시 업데이트됨)
                HotelImage hotelImage = new HotelImage();
                hotelImage.setContentId(null); // 등록 페이지에서는 contentId 없음
                hotelImage.setOriginUrl(imageUrl);
                hotelImage.setSmallUrl(imageUrl);
                hotelImage.setStatus(1); // 활성 상태
                HotelImage savedImage = hotelImageRepository.save(hotelImage);
                
                if (savedImage.getId() == null) {
                    throw new IllegalStateException("호텔 이미지 저장 후 id가 생성되지 않았습니다. 데이터베이스 오류일 수 있습니다.");
                }
                
                imageInfo.put("id", savedImage.getId()); // 실제 DB id 반환
                imageInfo.put("originUrl", imageUrl);
                imageInfo.put("smallUrl", imageUrl);
                
                uploadedImages.add(imageInfo);
            }
            
            map.put("success", true);
            map.put("message", "이미지가 성공적으로 업로드되었습니다.");
            map.put("images", uploadedImages);
            
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(map);
        }
    }
    

    // ==================== 호텔 등록용 객실 이미지 업로드 (roomIdx 없이) ====================

    @PostMapping("/hotel/room/images")
    @Operation(summary = "호텔 등록 시 객실 이미지 업로드", description = "호텔 등록 시 객실 이미지를 S3에 업로드합니다. (roomIdx 없이)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 업로드됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> uploadHotelRoomImages(
        @Parameter(description = "업로드할 이미지 파일들", required = true)
        @RequestPart("images") List<MultipartFile> images,
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        // 호텔 등록 시에는 인증만 체크 (contentId 불필요)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer adminIdx = principal.getAdminIdx();
        
        if (adminIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
        }
        
        try {
            // 이미지 파일 검증
            if (images == null || images.isEmpty()) {
                map.put("success", false);
                map.put("message", "업로드할 이미지가 없습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            // S3에 업로드하고 파일명만 반환 (객실 이미지는 파일명만 저장)
            List<Map<String, Object>> uploadedImages = new java.util.ArrayList<>();
            String folderPath = "hotelroom"; // 객실 이미지 폴더
            
            for (MultipartFile image : images) {
                // 파일이 비어있는지 확인
                if (image == null || image.isEmpty()) {
                    continue;
                }
                
                String imageFileName = imageService.upload(image, folderPath, true); // 파일명만 반환
                
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("id", System.currentTimeMillis() + uploadedImages.size()); // 임시 ID (타임스탬프)
                imageInfo.put("imageUrl", imageFileName); // 파일명만 저장
                imageInfo.put("imageOrder", uploadedImages.size() + 1); // 순서
                
                uploadedImages.add(imageInfo);
            }
            
            if (uploadedImages.isEmpty()) {
                map.put("success", false);
                map.put("message", "업로드할 수 있는 이미지가 없습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            map.put("success", true);
            map.put("message", "이미지가 성공적으로 업로드되었습니다.");
            map.put("images", uploadedImages);
            
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            map.put("success", false);
            map.put("message", "잘못된 요청: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        } catch (com.sist.backend.exception.S3UploadException e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 실패: " + e.getMessage());
            e.printStackTrace(); // 스택 트레이스 출력
            return ResponseEntity.internalServerError().body(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace(); // 스택 트레이스 출력
            return ResponseEntity.internalServerError().body(map);
        }
    }

    // ==================== 리뷰 이미지 업로드 ====================

    @PostMapping("/review/images")
    @Operation(summary = "리뷰 이미지 업로드", description = "리뷰 작성 시 이미지를 S3에 업로드합니다. (최대 5장)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공적으로 업로드됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> uploadReviewImages(
        @Parameter(description = "업로드할 이미지 파일들 (최대 5장)", required = true)
        @RequestPart("images") List<MultipartFile> images,
        HttpServletRequest request) {
        
        Map<String, Object> map = new HashMap<>();
        
        // 인증 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomerAdminSignupDTO principal = (CustomerAdminSignupDTO) authentication.getPrincipal();
        Integer customerIdx = principal.getCustomerIdx();
        
        if (customerIdx == null) {
            map.put("success", false);
            map.put("message", "인증 정보가 유효하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(map);
        }
        
        try {
            // 이미지 파일 검증
            if (images == null || images.isEmpty()) {
                map.put("success", false);
                map.put("message", "업로드할 이미지가 없습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            // 최대 5장 제한
            if (images.size() > 5) {
                map.put("success", false);
                map.put("message", "이미지는 최대 5장까지 업로드 가능합니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            // S3에 업로드하고 전체 URL 반환
            List<String> uploadedImageUrls = new java.util.ArrayList<>();
            String folderPath = "review"; // 리뷰 이미지 폴더
            
            for (MultipartFile image : images) {
                // 파일이 비어있는지 확인
                if (image == null || image.isEmpty()) {
                    continue;
                }
                
                // S3에 업로드 (전체 URL 반환)
                String imageUrl = imageService.upload(image, folderPath, false);
                uploadedImageUrls.add(imageUrl);
            }
            
            if (uploadedImageUrls.isEmpty()) {
                map.put("success", false);
                map.put("message", "업로드할 수 있는 이미지가 없습니다.");
                return ResponseEntity.badRequest().body(map);
            }
            
            map.put("success", true);
            map.put("message", "이미지가 성공적으로 업로드되었습니다.");
            map.put("imageUrls", uploadedImageUrls); // 전체 URL 배열 반환
            
            return ResponseEntity.ok(map);
        } catch (IllegalArgumentException e) {
            map.put("success", false);
            map.put("message", "잘못된 요청: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        } catch (com.sist.backend.exception.S3UploadException e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(map);
        } catch (Exception e) {
            map.put("success", false);
            map.put("message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(map);
        }
    }

}
