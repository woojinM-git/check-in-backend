package com.sist.backend.service;

import com.sist.backend.entity.ReviewImage;
import com.sist.backend.repository.ReviewImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageService {
    
    private final ReviewImageRepository reviewImageRepository;
    
    /**
     * 리뷰 이미지 저장 (2~5장 저장)
     * 1장은 review 테이블의 imageUrl에 저장되므로, 여기서는 2~5장 저장
     * @param reviewIdx 리뷰 인덱스
     * @param contentid 호텔 ID
     * @param imageUrls 이미지 URL 배열 (2번째부터, 최대 4개)
     * @return 저장된 ReviewImage
     */
    @Transactional
    public ReviewImage saveReviewImages(Integer reviewIdx, String contentid, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            log.info("추가 리뷰 이미지가 없어 저장하지 않습니다. reviewIdx: {}", reviewIdx);
            return null;
        }
        
        // 최대 4개까지만 저장 (2~5장)
        int maxImages = Math.min(imageUrls.size(), 4);
        log.info("추가 리뷰 이미지 저장 시작. reviewIdx: {}, contentid: {}, 이미지 개수: {}", reviewIdx, contentid, maxImages);
        
        ReviewImage.ReviewImageBuilder builder = ReviewImage.builder()
            .reviewIdx(reviewIdx)
            .contentid(contentid);
        
        // 각 컬럼에 순서대로 저장
        if (maxImages >= 1) builder.imageUrl2(imageUrls.get(0));
        if (maxImages >= 2) builder.imageUrl3(imageUrls.get(1));
        if (maxImages >= 3) builder.imageUrl4(imageUrls.get(2));
        if (maxImages >= 4) builder.imageUrl5(imageUrls.get(3));
        
        ReviewImage savedImage = reviewImageRepository.save(builder.build());
        log.info("추가 리뷰 이미지 저장 완료. reviewIdx: {}, contentid: {}, 저장된 이미지 개수: {}", reviewIdx, contentid, maxImages);
        
        return savedImage;
    }
    
    /**
     * 특정 리뷰의 이미지 조회
     * @param reviewIdx 리뷰 인덱스
     * @return ReviewImage (있으면 반환, 없으면 null)
     */
    public ReviewImage getReviewImage(Integer reviewIdx) {
        return reviewImageRepository.findByReviewIdx(reviewIdx).orElse(null);
    }
    
    /**
     * 특정 리뷰의 이미지 URL 리스트 조회 (2~5장)
     * @param reviewIdx 리뷰 인덱스
     * @return 이미지 URL 리스트 (2장부터 순서대로)
     */
    public List<String> getReviewImageUrls(Integer reviewIdx) {
        Optional<ReviewImage> reviewImageOpt = reviewImageRepository.findByReviewIdx(reviewIdx);
        
        if (reviewImageOpt.isEmpty()) {
            return List.of();
        }
        
        ReviewImage reviewImage = reviewImageOpt.get();
        List<String> urls = new ArrayList<>();
        
        // NULL이 아닌 것만 순서대로 추가
        if (reviewImage.getImageUrl2() != null) urls.add(reviewImage.getImageUrl2());
        if (reviewImage.getImageUrl3() != null) urls.add(reviewImage.getImageUrl3());
        if (reviewImage.getImageUrl4() != null) urls.add(reviewImage.getImageUrl4());
        if (reviewImage.getImageUrl5() != null) urls.add(reviewImage.getImageUrl5());
        
        return urls;
    }
    
    /**
     * 특정 리뷰의 모든 이미지 URL 리스트 조회 (1장 + 2~5장)
     * @param reviewIdx 리뷰 인덱스
     * @param firstImageUrl review 테이블의 imageUrl (1장)
     * @return 모든 이미지 URL 리스트 (순서대로)
     */
    public List<String> getAllReviewImageUrls(Integer reviewIdx, String firstImageUrl) {
        List<String> allUrls = new ArrayList<>();
        
        // 1장 추가 (review.imageUrl)
        if (firstImageUrl != null && !firstImageUrl.trim().isEmpty()) {
            allUrls.add(firstImageUrl);
        }
        
        // 2~5장 추가 (review_image 테이블)
        List<String> additionalUrls = getReviewImageUrls(reviewIdx);
        allUrls.addAll(additionalUrls);
        
        return allUrls;
    }
    
    /**
     * 리뷰 이미지 삭제 (리뷰 삭제 시 함께 삭제됨 - CASCADE)
     * @param reviewIdx 리뷰 인덱스
     */
    @Transactional
    public void deleteReviewImages(Integer reviewIdx) {
        log.info("리뷰 이미지 삭제 시작. reviewIdx: {}", reviewIdx);
        reviewImageRepository.deleteByReviewIdx(reviewIdx);
        log.info("리뷰 이미지 삭제 완료. reviewIdx: {}", reviewIdx);
    }
}

