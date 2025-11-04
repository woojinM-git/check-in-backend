package com.sist.backend.service;

import com.sist.backend.entity.RoomImage;
import com.sist.backend.repository.RoomImageRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.util.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomImageService {
    
    private final RoomImageRepository roomImageRepository;
    private final RoomRepository roomRepository;
    private final S3ImageService s3ImageService;
    
    private static final int MAX_IMAGES_PER_ROOM = 10;
    private static final String S3_FOLDER_PATH = "hotelroom"; // 객실 이미지 저장 폴더 (프론트엔드와 일치)
    
    /**
     * 객실 이미지 업로드 (단일)
     * @param roomIdx 객실 인덱스
     * @param contentId 호텔 콘텐츠 ID
     * @param image 업로드할 이미지 파일
     * @param imageOrder 이미지 순서 (1-10)
     * @return 저장된 RoomImage 엔티티
     */
    @Transactional
    public RoomImage uploadRoomImage(Integer roomIdx, String contentId, MultipartFile image, Integer imageOrder) {
        // 객실 존재 여부 확인
        roomRepository.findByRoomIdx(roomIdx)
            .orElseThrow(() -> new RuntimeException("객실을 찾을 수 없습니다."));
        
        // 이미지 순서 유효성 검사
        if (imageOrder < 1 || imageOrder > MAX_IMAGES_PER_ROOM) {
            throw new IllegalArgumentException("이미지 순서는 1부터 " + MAX_IMAGES_PER_ROOM + "까지 가능합니다.");
        }
        
        // 해당 순서에 이미지가 이미 존재하는지 확인
        roomImageRepository.findByRoomIdxAndContentIdAndImageOrder(roomIdx, contentId, imageOrder)
            .ifPresent(existing -> {
                // 기존 이미지 삭제
                s3ImageService.deleteImageFromS3(existing.getImageUrl());
                roomImageRepository.delete(existing);
            });
        
        // S3에 이미지 업로드 (파일명만 반환)
        String imageFileName = s3ImageService.upload(image, S3_FOLDER_PATH, true);
        
        // DB에 저장 (파일명만 저장)
        RoomImage roomImage = new RoomImage();
        roomImage.setRoomIdx(roomIdx);
        roomImage.setContentId(contentId);
        roomImage.setImageUrl(imageFileName); // 파일명만 저장 (예: hotel_deluxe.jpg)
        roomImage.setImageOrder(imageOrder);
        
        return roomImageRepository.save(roomImage);
    }
    
    /**
     * 객실 이미지 여러 개 업로드
     * @param roomIdx 객실 인덱스
     * @param contentId 호텔 콘텐츠 ID
     * @param images 업로드할 이미지 파일들
     * @return 저장된 RoomImage 엔티티 리스트
     */
    @Transactional
    public List<RoomImage> uploadRoomImages(Integer roomIdx, String contentId, List<MultipartFile> images) {
        // 현재 저장된 이미지 개수 확인
        Long currentImageCount = roomImageRepository.countByRoomIdxAndContentId(roomIdx, contentId);
        
        if (currentImageCount + images.size() > MAX_IMAGES_PER_ROOM) {
            throw new IllegalArgumentException(
                String.format("객실당 최대 %d개의 이미지만 저장 가능합니다. (현재: %d개, 추가 시도: %d개)", 
                    MAX_IMAGES_PER_ROOM, currentImageCount, images.size())
            );
        }
        
        // 사용 가능한 순서 찾기
        List<Integer> availableOrders = IntStream.rangeClosed(1, MAX_IMAGES_PER_ROOM)
            .filter(order -> {
                return roomImageRepository.findByRoomIdxAndContentIdAndImageOrder(roomIdx, contentId, order)
                    .isEmpty();
            })
            .boxed()
            .limit(images.size())
            .toList();
        
        if (availableOrders.size() < images.size()) {
            throw new IllegalArgumentException("저장 가능한 이미지 순서가 부족합니다.");
        }
        
        // 각 이미지 업로드 및 저장
        return IntStream.range(0, images.size())
            .mapToObj(i -> {
                try {
                    return uploadRoomImage(roomIdx, contentId, images.get(i), availableOrders.get(i));
                } catch (Exception e) {
                    log.error("이미지 업로드 실패: {}", e.getMessage());
                    throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
                }
            })
            .toList();
    }
    
    /**
     * 객실의 모든 이미지 조회
     */
    public List<RoomImage> getRoomImages(Integer roomIdx, String contentId) {
        return roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(roomIdx, contentId);
    }
    
    /**
     * 특정 객실 이미지 삭제
     */
    @Transactional
    public void deleteRoomImage(Integer roomImageIdx) {
        RoomImage roomImage = roomImageRepository.findById(roomImageIdx)
            .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
        
        // S3에서 이미지 삭제 (파일명을 전체 경로로 변환하여 삭제)
        String s3Key = S3_FOLDER_PATH + "/" + roomImage.getImageUrl();
        String fullUrl = "https://sist-checkin.s3.ap-northeast-2.amazonaws.com/" + s3Key;
        s3ImageService.deleteImageFromS3(fullUrl);
        
        // DB에서 삭제
        roomImageRepository.delete(roomImage);
    }
    
    /**
     * 객실의 모든 이미지 삭제
     */
    @Transactional
    public void deleteAllRoomImages(Integer roomIdx, String contentId) {
        List<RoomImage> images = roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(roomIdx, contentId);
        
        // S3에서 모든 이미지 삭제 (파일명을 전체 경로로 변환하여 삭제)
        images.forEach(image -> {
            try {
                String s3Key = S3_FOLDER_PATH + "/" + image.getImageUrl();
                String fullUrl = "https://sist-checkin.s3.ap-northeast-2.amazonaws.com/" + s3Key;
                s3ImageService.deleteImageFromS3(fullUrl);
            } catch (Exception e) {
                log.error("S3 이미지 삭제 실패: {}", image.getImageUrl(), e);
            }
        });
        
        // DB에서 삭제
        roomImageRepository.deleteAll(images);
    }
}

