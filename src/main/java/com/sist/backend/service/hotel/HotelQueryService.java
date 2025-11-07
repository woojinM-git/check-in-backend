package com.sist.backend.service.hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sist.backend.dto.hotel.HotelImageResponse;
import com.sist.backend.dto.hotel.HotelResponse;
import com.sist.backend.dto.hotel.ReviewResponse;
import com.sist.backend.dto.hotel.RoomAvailabilityResponse;
import com.sist.backend.dto.hotel.RoomResponse;
import com.sist.backend.entity.Customer;
import com.sist.backend.entity.HotelDetail;
import com.sist.backend.entity.HotelImage;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.Review;
import com.sist.backend.entity.Room;
import com.sist.backend.mapper.hotel.RoomAdvancedMapper;
import com.sist.backend.repository.CustomerRepository;
import com.sist.backend.repository.ReviewImageRepository;
import com.sist.backend.repository.ReviewRepository;
import com.sist.backend.repository.hotel.HotelImageRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.service.ReviewImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class HotelQueryService {

    private final ReviewImageRepository reviewImageRepository;

    //용준 사용 호텔 디테일 창에서 객실 정보 편의시설 정보 이미지등
    //불러 올때 사용
    private final HotelInfoRepository hotelInfoRepository;
    private final RoomRepository roomRepository;
    private final RoomAdvancedMapper roomAdvancedMapper;
    private final HotelImageRepository hotelImageRepository;
    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ReviewImageService reviewImageService;

    // 호텔 상세 조회 (JPA)
    public Optional<HotelResponse> getHotel(String contentId) {
        return hotelInfoRepository.findById(contentId)
                .map(this::mapHotel);
    }

    // 객실 목록 조회 - 날짜가 제공되면 예약 가능성 포함하여 조회
    public List<RoomAvailabilityResponse> getRooms(String contentId, String name, LocalDate checkinDate, LocalDate checkoutDate) {
        // 날짜가 제공되면 예약 가능성 조회
        if (checkinDate != null && checkoutDate != null) {
            return getRoomAvailability(contentId, checkinDate, checkoutDate);
        }

        // 날짜가 없으면 기존 방식 (간단한 변환 필요)
        List<Room> rooms;
        if (name == null || name.isBlank()) {
            rooms = roomRepository.findByContentId(contentId);
        } else {
            rooms = roomRepository.findByContentIdAndNameContainingIgnoreCase(contentId, name);
        }

        // Room을 RoomAvailabilityResponse로 변환 (예약 가능성 없이)
        return rooms.stream().map(room -> {
            // status에 따른 메시지 설정
            String availabilityMessage;
            if (room.getStatus() == 0) {
                availabilityMessage = "숙소 측 요청으로 사용 불가능한 방입니다";
            } else {
                availabilityMessage = "예약 가능한 방입니다";
            }

            return RoomAvailabilityResponse.builder()
                    .roomIdx(room.getRoomIdx())
                    .contentId(room.getContentId())
                    .name(room.getName())
                    .capacity(room.getCapacity())
                    .basePrice(room.getBasePrice())
                    .refundable(room.getRefundable())
                    .breakfastIncluded(room.getBreakfastIncluded())
                    .smoking(room.getSmoking())
                    .imageUrl(room.getImageUrl())
                    .status(room.getStatus())
                    .availabilityMessage(availabilityMessage)
                    .roomCount(room.getRoomCount())
                    .build();
        }).collect(Collectors.toList());
    }

    // 객실 고급 검색 (MyBatis) - 동적 조건(이름/최소/최대 수용인원)
    public List<RoomResponse> searchRoomsAdvanced(String contentId, String name, Integer minCapacity, Integer maxCapacity) {
        List<Room> rooms = roomAdvancedMapper.searchRooms(contentId, name, minCapacity, maxCapacity);
        return rooms.stream().map(this::mapRoom).collect(Collectors.toList());
    }

    // 호텔 이미지 목록 조회 (최대 10장)
    @Transactional(readOnly = true)
    public List<HotelImageResponse> getHotelImages(String contentId) {
        List<HotelImage> images = hotelImageRepository.findTop10ByContentIdOrderByIdAsc(contentId);
        return images.stream().map(this::mapHotelImage).collect(Collectors.toList());
    }

    // 객실 예약 가능성 조회 (MyBatis) - 날짜 기반 예약 가능 여부 포함
    public List<RoomAvailabilityResponse> getRoomAvailability(String contentId, LocalDate checkinDate, LocalDate checkoutDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("contentId", contentId);
        params.put("checkinDate", checkinDate);
        params.put("checkoutDate", checkoutDate);

        return roomAdvancedMapper.findAvailableRooms(params);
    }

    // 엔티티(HotelInfo, HotelDetail) -> 응답 DTO 매핑
    private HotelResponse mapHotel(HotelInfo info) {
        HotelDetail detail = info.getHotelDetail();
        return HotelResponse.builder()
                .contentId(info.getContentId())
                .title(info.getTitle())
                .adress(info.getAdress())
                .imageUrl(info.getImageUrl())
                .areaCode(info.getAreaCode())
                .roomcount(detail != null ? detail.getRoomcount() : null)
                .foodplace(detail != null ? detail.getFoodplace() : null)
                .parkinglodging(detail != null ? detail.getParkinglodging() : null)
                .reservationlodging(detail != null ? detail.getReservationlodging() : null)
                .scalelodging(detail != null ? detail.getScalelodging() : null)
                .build();
    }

    private RoomResponse mapRoom(Room room) {
        // 조식 판단 규칙:
        // - HotelDetail.foodplace 가 null/빈값이면 "조식 없음(false)"
        // - 값이 존재하면 "조식 가능(true)"
        // - HotelDetail 이 없고, Room.breakfastIncluded 가 있으면 그 값을 사용
        Boolean breakfast = null;
        HotelDetail detail = room.getHotelInfo() != null ? room.getHotelInfo().getHotelDetail() : null;
        if (detail != null) {
            breakfast = (detail.getFoodplace() != null && !detail.getFoodplace().isBlank());
        } else if (room.getBreakfastIncluded() != null) {
            // fallback to room flag if present
            breakfast = room.getBreakfastIncluded();
        }

        // 흡연 가능 여부:
        // - 현재 정확한 판별이 어려움 → Room.smoking 값이 있으면 사용, 없으면 null 유지
        Boolean smoking = (room.getSmoking() != null) ? room.getSmoking() : null;

        return RoomResponse.builder()
                .roomIdx(room.getRoomIdx())
                .contentId(room.getContentId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .basePrice(room.getBasePrice())
                .refundable(room.getRefundable())
                .breakfastIncluded(breakfast)
                .smoking(smoking)
                .imageUrl(room.getImageUrl())
                .status(room.getStatus())
                .build();
    }

    // 엔티티(HotelImage) -> 응답 DTO 매핑
    private HotelImageResponse mapHotelImage(HotelImage image) {
        return HotelImageResponse.builder()
                .id(image.getId())
                .contentId(image.getContentId())
                .originUrl(image.getOriginUrl())
                .smallUrl(image.getSmallUrl())
                .build();
    }

    // 호텔 리뷰 목록 조회
    public List<ReviewResponse> getReviews(String contentId) {
        List<Review> reviews = reviewRepository.findByContentId(contentId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return reviews.stream().map(review -> {
            // Customer 정보 조회
            String userName = "익명";
            Optional<Customer> customerOpt = customerRepository.findById(review.getCustomerIdx());
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                userName = customer.getName() != null && !customer.getName().isBlank()
                        ? customer.getName()
                        : (customer.getNickname() != null && !customer.getNickname().isBlank()
                        ? customer.getNickname()
                        : "익명");
            }

            // Room 정보 조회
            String roomType = null;
            Optional<Room> roomOpt = roomRepository.findById(review.getRoomIdx());
            if (roomOpt.isPresent()) {
                roomType = roomOpt.get().getName();
            }

            // star를 0-10 스케일로 변환 (DB는 0-5 스케일)
            Double rating = null;
            if (review.getStar() != null) {
                rating = review.getStar().doubleValue() * 2.0; // 0-5를 0-10으로 변환
            }

            // 날짜 포맷팅
            String date = review.getCreatedAt() != null
                    ? review.getCreatedAt().format(formatter)
                    : null;

            // 대표이미지 1장
            String firstImageUrl = review.getImageUrl();
            log.info("firstImageUrl: {}", firstImageUrl);
            // 추가 이미지 2~5장 조회
            List<String> imageUrls = reviewImageService.getReviewImageUrls(review.getReviewIdx());

            // 대표 + 추가 이미지 합치기
            List<String> allImageUrls = new ArrayList<>();
            if(firstImageUrl != null && !firstImageUrl.isBlank()) {
                allImageUrls.add(firstImageUrl);
            }
            allImageUrls.addAll(imageUrls);

            return ReviewResponse.builder()
                    .id(review.getReviewIdx())
                    .userName(userName)
                    .date(date)
                    .roomType(roomType)
                    .rating(rating)
                    .comment(review.getContent())
                    .imageUrl(firstImageUrl)
                    .imageUrls(allImageUrls)
                    .build();
        }).collect(Collectors.toList());
    }

    // 호텔 평균 평점 및 리뷰 개수 조회
    public Map<String, Object> getReviewSummary(String contentId) {
        BigDecimal avgRating = reviewRepository.findAverageRatingByContentId(contentId);
        Long reviewCount = reviewRepository.countFeedbackByContentId(contentId);

        // 평점을 0-10 스케일로 변환
        Double rating = avgRating != null ? avgRating.doubleValue() * 2.0 : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("rating", rating);
        summary.put("reviewCount", reviewCount != null ? reviewCount.intValue() : 0);
        return summary;
    }
}
