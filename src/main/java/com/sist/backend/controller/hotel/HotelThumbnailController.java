package com.sist.backend.controller.hotel;

import com.sist.backend.entity.HotelImage;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.repository.hotel.HotelImageRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelThumbnailController {

    private final HotelInfoRepository hotelInfoRepository;
    private final HotelImageRepository hotelImageRepository;

    /**
     * 호텔 대표 썸네일 단일 URL로 리다이렉트
     * 우선순위: HotelInfo.imageUrl > HotelImage.smallUrl > HotelImage.originUrl
     */
    @GetMapping("/{contentId}/thumbnail")
    public ResponseEntity<Void> getThumbnail(@PathVariable String contentId) {
        // 1) HotelInfo.imageUrl 우선 사용
        Optional<HotelInfo> infoOpt = hotelInfoRepository.findById(contentId);
        if (infoOpt.isPresent()) {
            String imageUrl = infoOpt.get().getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                return ResponseEntity.status(302).header("Location", imageUrl).build();
            }
        }

        // 2) HotelImage 테이블에서 첫 이미지 사용
        List<HotelImage> images = hotelImageRepository.findTop10ByContentIdOrderByIdAsc(contentId);
        if (images != null && !images.isEmpty()) {
            HotelImage first = images.get(0);
            String candidate = first.getSmallUrl() != null && !first.getSmallUrl().isBlank()
                ? first.getSmallUrl() : first.getOriginUrl();
            if (candidate != null && !candidate.isBlank()) {
                return ResponseEntity.status(302).header("Location", candidate).build();
            }
        }

        // 3) not found
        return ResponseEntity.notFound().build();
    }
}


