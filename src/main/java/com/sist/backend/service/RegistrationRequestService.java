package com.sist.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sist.backend.dto.admin.HotelEditFormDto;
import com.sist.backend.dto.master.RegistrationRequestDto;
import com.sist.backend.dto.master.RegistrationRequestPlusDto;
import com.sist.backend.entity.RegistrationRequest;
import com.sist.backend.repository.RegistrationRequestRepository;
import com.sist.backend.service.hotel.HotelInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationRequestService {
    
    private final RegistrationRequestRepository rrRepository;
    private final HotelInfoService hotelInfoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* 대시보드의 승인요청 목록 (상위 5개) */
    public List<RegistrationRequestDto> findTop5ByStatusInDashboard() {
        Pageable pageable = Pageable.ofSize(5);
        List<RegistrationRequest> registrationRequests = rrRepository.findTop5ByStatusInDashboard(pageable);
        return registrationRequests.stream()
            .map(RegistrationRequestDto::fromEntity)
            .collect(Collectors.toList());
    }

    /* 승인요청중인 호텔의 수 */
    public Integer findByStatusCount() {
        return rrRepository.findByStatusCount();
    }

    public Page<RegistrationRequestPlusDto> findByStatusDto(Pageable pageable) {
        Page<RegistrationRequest> registrationRequests = rrRepository.findByStatus(pageable);
        return registrationRequests.map(RegistrationRequestPlusDto::fromEntity);
    }

    /* ID로 승인 요청 조회 */
    public RegistrationRequest findById(Integer registrationIdx) {
        Optional<RegistrationRequest> request = rrRepository.findById(registrationIdx);
        if (request.isPresent()) {
            return request.get();
        } else {
            throw new IllegalArgumentException("승인 요청을 찾을 수 없습니다: " + registrationIdx);
        }
    }

    /* 승인 요청 업데이트 */
    @org.springframework.transaction.annotation.Transactional
    public void updateRequest(Integer registrationIdx, Integer status, LocalDateTime approvDate) {
        rrRepository.updateRequest(registrationIdx, status, approvDate);
    }

    /* 거부 요청 업데이트 */
    @org.springframework.transaction.annotation.Transactional
    public void updateRejectRequest(Integer registrationIdx, String refusalMsg, Integer status) {
        rrRepository.updateRejectRequest(registrationIdx, status, refusalMsg);
    }

    /* 오늘 승인된 호텔 수 */
    public Integer findTodayApprovedCount() {
        return rrRepository.findTodayApprovedCount();
    }

    /* 오늘 거부된 호텔 수 */
    public Integer findTodayRejectedCount() {
        return rrRepository.findTodayRejectedCount();
    }

    /**
     * 호텔 승인 처리: HotelDraft의 JSON을 파싱하여 정규화된 테이블에 저장
     * @param registrationIdx 등록 요청 인덱스
     * @param approvDate 승인 날짜
     */
    @Transactional
    public void approveHotelRegistration(Integer registrationIdx, LocalDateTime approvDate) {
        log.info("호텔 승인 처리 시작: registrationIdx={}", registrationIdx);
        
        try {
            // 1. RegistrationRequest 조회
            RegistrationRequest request = findById(registrationIdx);
            log.info("RegistrationRequest 조회 완료: adminIdx={}, draftIdx={}", request.getAdminIdx(), request.getDraftIdx());
            
            // 2. HotelDraft 조회 및 formData 파싱
            if (request.getHotelDraft() == null || request.getHotelDraft().getFormData() == null) {
                throw new IllegalArgumentException("호텔 등록 데이터가 없습니다: draftIdx=" + request.getDraftIdx());
            }
            
            String formDataJson = request.getHotelDraft().getFormData();
            log.info("HotelDraft formData 파싱 시작");
            
            // 3. JSON을 Map으로 파싱
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(Map.class, String.class, Object.class);
            Map<String, Object> formDataMap = objectMapper.readValue(formDataJson, mapType);
            
            // 4. HotelEditFormDto로 변환
            HotelEditFormDto dto = convertFormDataToDto(formDataMap);
            log.info("HotelEditFormDto 변환 완료");
            
            // 5. 정규화된 테이블에 저장
            String contentId = hotelInfoService.createHotelInfoFromDto(request.getAdminIdx(), dto);
            log.info("✅ 정규화된 테이블 저장 완료: contentId={}", contentId);
            
            // 6. 승인 상태 업데이트
            updateRequest(registrationIdx, 1, approvDate);
            log.info("✅ 호텔 승인 처리 완료: registrationIdx={}, contentId={}", registrationIdx, contentId);
            
        } catch (Exception e) {
            log.error("❌ 호텔 승인 처리 실패: registrationIdx={}, error={}", registrationIdx, e.getMessage(), e);
            throw new RuntimeException("호텔 승인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * formData Map을 HotelEditFormDto로 변환
     */
    private HotelEditFormDto convertFormDataToDto(Map<String, Object> formData) {
        HotelEditFormDto.HotelInfoDto hotelInfoDto = null;
        HotelEditFormDto.HotelDetailDto hotelDetailDto = null;
        HotelEditFormDto.AreaDto areaDto = null;
        List<HotelEditFormDto.ImageDto> imageDtos = null;
        List<HotelEditFormDto.RoomDto> roomDtos = null;
        List<HotelEditFormDto.DiningDto> diningDtos = null;
        
        // hotelInfo 파싱
        if (formData.get("hotelInfo") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hotelInfoMap = (Map<String, Object>) formData.get("hotelInfo");
            hotelInfoDto = HotelEditFormDto.HotelInfoDto.builder()
                .title((String) hotelInfoMap.get("title"))
                .adress((String) hotelInfoMap.get("adress"))
                // phone → tel 매핑 (프론트엔드에서 phone으로 보내지만 백엔드는 tel로 저장)
                .tel(hotelInfoMap.get("tel") != null 
                    ? (String) hotelInfoMap.get("tel") 
                    : (String) hotelInfoMap.get("phone"))
                .imageUrl((String) hotelInfoMap.get("imageUrl")) // 대표 이미지 URL
                .build();
        }
        
        // hotelDetail 파싱 (항상 생성하여 roomcount를 저장할 수 있도록 함)
        if (formData.get("hotelDetail") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hotelDetailMap = (Map<String, Object>) formData.get("hotelDetail");
            hotelDetailDto = HotelEditFormDto.HotelDetailDto.builder()
                // description → reservationlodging으로 매핑
                .reservationlodging((String) hotelDetailMap.get("reservationlodging") != null 
                    ? (String) hotelDetailMap.get("reservationlodging") 
                    : (String) hotelDetailMap.get("description"))
                .foodplace((String) hotelDetailMap.get("foodplace"))
                // scale → scalelodging으로 매핑
                .scalelodging((String) hotelDetailMap.get("scalelodging") != null 
                    ? (String) hotelDetailMap.get("scalelodging") 
                    : (String) hotelDetailMap.get("scale"))
                .parkinglodging((String) hotelDetailMap.get("parkinglodging"))
                .build();
        } else {
            // hotelDetail이 없어도 빈 객체로 생성 (roomcount 저장을 위해)
            hotelDetailDto = HotelEditFormDto.HotelDetailDto.builder().build();
        }
        
        // area 파싱
        if (formData.get("area") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> areaMap = (Map<String, Object>) formData.get("area");
            areaDto = HotelEditFormDto.AreaDto.builder()
                .areaCode((String) areaMap.get("areaCode"))
                .build();
        }
        
        // images 파싱
        if (formData.get("images") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> imagesList = (List<Map<String, Object>>) formData.get("images");
            imageDtos = imagesList.stream().map(imageMap -> {
                Object idObj = imageMap.get("id");
                Long id = null;
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                }
                return HotelEditFormDto.ImageDto.builder()
                    .id(id)
                    .originUrl((String) imageMap.get("originUrl"))
                    .smallUrl((String) imageMap.get("smallUrl"))
                    .build();
            }).collect(Collectors.toList());
        }
        
        // rooms 파싱
        if (formData.get("rooms") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roomsList = (List<Map<String, Object>>) formData.get("rooms");
            roomDtos = roomsList.stream().map(roomMap -> {
                List<HotelEditFormDto.RoomImageDto> roomImages = null;
                if (roomMap.get("images") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> imagesList = (List<Map<String, Object>>) roomMap.get("images");
                    roomImages = imagesList.stream().map(imageMap -> {
                        Object orderObj = imageMap.get("imageOrder");
                        Integer order = null;
                        if (orderObj instanceof Number) {
                            order = ((Number) orderObj).intValue();
                        }
                        return HotelEditFormDto.RoomImageDto.builder()
                            .imageUrl((String) imageMap.get("imageUrl"))
                            .imageOrder(order)
                            .build();
                    }).collect(Collectors.toList());
                }
                
                // basePrice 파싱 (price 또는 basePrice 필드 지원)
                Object basePriceObj = roomMap.get("basePrice") != null ? roomMap.get("basePrice") : roomMap.get("price");
                Integer basePrice = null;
                if (basePriceObj instanceof Number) {
                    basePrice = ((Number) basePriceObj).intValue();
                } else if (basePriceObj instanceof String) {
                    try {
                        basePrice = Integer.parseInt((String) basePriceObj);
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }
                
                // status 파싱 (체크박스로 받은 값, 기본값 1)
                Object statusObj = roomMap.get("status");
                Integer status = 1; // 기본값 1 (사용가능)
                if (statusObj instanceof Number) {
                    status = ((Number) statusObj).intValue();
                } else if (statusObj instanceof Boolean) {
                    status = ((Boolean) statusObj) ? 1 : 0;
                }
                
                // roomCount 파싱 (기본값 1)
                Object roomCountObj = roomMap.get("roomCount");
                Integer roomCount = 1; // 기본값 1
                if (roomCountObj instanceof Number) {
                    roomCount = ((Number) roomCountObj).intValue();
                }
                
                // refundable 파싱 (기본값 1, int로 변환: true=1, false=0)
                Object refundableObj = roomMap.get("refundable");
                Integer refundable = 1; // 기본값 1 (환불 가능)
                if (refundableObj instanceof Boolean) {
                    refundable = ((Boolean) refundableObj) ? 1 : 0;
                } else if (refundableObj instanceof Number) {
                    refundable = ((Number) refundableObj).intValue();
                }
                
                // imageUrl 파싱 (객실 대표 이미지)
                String imageUrl = null;
                Object imageUrlObj = roomMap.get("imageUrl");
                if (imageUrlObj != null) {
                    imageUrl = imageUrlObj.toString();
                }
                
                return HotelEditFormDto.RoomDto.builder()
                    .name((String) roomMap.get("name"))
                    .capacity(roomMap.get("capacity") instanceof Number ? ((Number) roomMap.get("capacity")).intValue() : null)
                    .basePrice(basePrice)
                    .refundable(refundable == 1) // Integer를 Boolean으로 변환 (1=true, 0=false)
                    .breakfastIncluded(roomMap.get("breakfastIncluded") instanceof Boolean ? (Boolean) roomMap.get("breakfastIncluded") : null)
                    .smoking(roomMap.get("smoking") instanceof Boolean ? (Boolean) roomMap.get("smoking") : null)
                    .roomCount(roomCount) // 기본값 1로 설정
                    .status(status) // 사용자가 선택한 값 또는 기본값 1
                    .imageUrl(imageUrl) // Room.imageUrl (객실 대표 이미지)
                    .images(roomImages) // RoomImage 리스트 (객실 상세 이미지들)
                    .build();
            }).collect(Collectors.toList());
        }
        
        // hotelDetail에 roomcount 설정 (객실 총 개수)
        if (hotelDetailDto != null && roomDtos != null) {
            hotelDetailDto.setRoomcount(String.valueOf(roomDtos.size()));
        }
        
        // dining 파싱
        if (formData.get("dining") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diningList = (List<Map<String, Object>>) formData.get("dining");
            diningDtos = diningList.stream().map(diningMap -> {
                return HotelEditFormDto.DiningDto.builder()
                    .name((String) diningMap.get("name"))
                    .operatingHours((String) diningMap.get("operatingHours"))
                    .description((String) diningMap.get("description"))
                    .basePrice(diningMap.get("basePrice") instanceof Number ? ((Number) diningMap.get("basePrice")).intValue() : null)
                    .totalSeats(diningMap.get("totalSeats") instanceof Number ? ((Number) diningMap.get("totalSeats")).intValue() : null)
                    .build();
            }).collect(Collectors.toList());
        }
        
        return HotelEditFormDto.builder()
            .hotelInfo(hotelInfoDto)
            .hotelDetail(hotelDetailDto)
            .area(areaDto)
            .images(imageDtos)
            .rooms(roomDtos)
            .dining(diningDtos)
            .build();
    }
}
