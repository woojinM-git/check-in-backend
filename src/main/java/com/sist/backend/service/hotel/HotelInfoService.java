package com.sist.backend.service.hotel;

import com.sist.backend.dto.admin.HotelEditFormDto;
import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.entity.*;
import com.sist.backend.repository.*;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.HotelDetailRepository;
import com.sist.backend.repository.hotel.HotelImageRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import com.sist.backend.repository.RoomImageRepository;
import com.sist.backend.repository.admin.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelInfoService {
    
    private final HotelInfoRepository hotelInfoRepository;
    private final HotelDetailRepository hotelDetailRepository;
    private final DiningRepository diningRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final AdminRepository adminRepository;
    
    /**
     * 상위 10개 호텔 정보 조회 (Category, Area 포함)
     */
    public List<HotelInfo> getTop10Hotels() {
        Pageable pageable = PageRequest.of(0, 10);
        return hotelInfoRepository.findTop9WithCategoryAndArea(pageable);
    }
    
    /**
     * 페이징 처리된 호텔 목록 조회
     */
    public Page<HotelInfo> getHotelsWithPaging(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return hotelInfoRepository.findAllWithCategoryAndArea(pageable);
    }

    /* 대시보드 - 등록되어 있는 호텔 갯수 */
    public Integer findRegistrationHotelCount() {
        return hotelInfoRepository.findRegistrationHotelCount();
    }

    public Page<HotelInfoDto> findAllHotelWithDetailsAsDto(Pageable pageable) {
        Page<HotelInfo> hotelInfoPage = hotelInfoRepository.findAllHotelWithDetailsAsDto(pageable);
        return hotelInfoPage.map(HotelInfoDto::hotelInfoDto);
    }

    public Optional<String> findContentIdByAdminIdx(Integer adminIdx) {
        return hotelInfoRepository.findContentIdByAdminIdx(adminIdx);
    }

    /**
     * 호텔 정지 처리
     * @param contentId 호텔 ID
     * @param reason 정지 사유
     */
    @Transactional
    public void suspendHotel(String contentId, String reason) {
        HotelInfo hotelInfo = hotelInfoRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));
        
        // status를 1로 변경 (운영종료)
        hotelInfo.setStatus(1);
        
        hotelInfoRepository.save(hotelInfo);
    }

    /**
     * adminIdx로 호텔 정보 조회 (HotelDetail 포함)
     */
    public Optional<HotelInfo> findByAdminIdx(Integer adminIdx) {
        return hotelInfoRepository.findByAdminIdxWithDetails(adminIdx);
    }

    /**
     * 정규화된 테이블에서 호텔 정보를 조회하여 등록 폼 구조로 변환
     */
    public HotelEditFormDto getHotelInfoForEdit(String contentId) {
        // HotelInfo 조회
        HotelInfo hotelInfo = hotelInfoRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));
        
        // HotelDetail 조회
        HotelDetail hotelDetail = hotelDetailRepository.findById(contentId)
            .orElse(new HotelDetail());
        
        // Dining 목록 조회
        List<Dining> dinings = diningRepository.findByContentidAndStatus(contentId);
        
        // HotelImage 목록 조회
        List<HotelImage> hotelImages = hotelImageRepository.findTop10ByContentIdOrderByIdAsc(contentId);
        
        // Entity 필드명에 맞춰 DTO 변환
        HotelEditFormDto.HotelInfoDto hotelInfoDto = HotelEditFormDto.HotelInfoDto.builder()
            .title(hotelInfo.getTitle())
            .adress(hotelInfo.getAdress())
            .tel(hotelInfo.getTel())
            .build();
        
        HotelEditFormDto.HotelDetailDto hotelDetailDto = HotelEditFormDto.HotelDetailDto.builder()
            .reservationlodging(hotelDetail.getReservationlodging() != null ? hotelDetail.getReservationlodging() : "")
            .foodplace(hotelDetail.getFoodplace() != null ? hotelDetail.getFoodplace() : "")
            .scalelodging(hotelDetail.getScalelodging() != null ? hotelDetail.getScalelodging() : "")
            .parkinglodging(hotelDetail.getParkinglodging() != null ? hotelDetail.getParkinglodging() : "")
            .build();
        
        HotelEditFormDto.AreaDto areaDto = HotelEditFormDto.AreaDto.builder()
            .areaCode(hotelInfo.getAreaCode() != null ? hotelInfo.getAreaCode() : "")
            .nearbyAttractions("") // 프론트엔드용 (Entity에 없음)
            .transportation("") // 프론트엔드용 (Entity에 없음)
            .build();
        
        List<HotelEditFormDto.ImageDto> imageDtos = hotelImages.stream().map(image ->
            HotelEditFormDto.ImageDto.builder()
                .id(image.getId() != null ? Long.valueOf(image.getId()) : null) // Integer를 Long으로 변환
                .originUrl(image.getOriginUrl())
                .smallUrl(image.getSmallUrl())
                .build()
        ).collect(Collectors.toList());
        
        List<HotelEditFormDto.DiningDto> diningDtos = dinings.stream().map(dining ->
            HotelEditFormDto.DiningDto.builder()
                .diningIdx(dining.getDiningIdx())
                .name(dining.getName())
                .operatingHours(dining.getOpenTime() != null && dining.getCloseTime() != null 
                    ? dining.getOpenTime().toString() + " - " + dining.getCloseTime().toString()
                    : "")
                .description(dining.getDescription())
                .basePrice(dining.getBasePrice())
                .totalSeats(dining.getTotalSeats())
                .build()
        ).collect(Collectors.toList());
        
        return HotelEditFormDto.builder()
            .hotelInfo(hotelInfoDto)
            .hotelDetail(hotelDetailDto)
            .area(areaDto)
            .images(imageDtos)
            .dining(diningDtos)
            .build();
    }

    /**
     * 등록 폼 구조로 받은 데이터를 정규화된 테이블에 저장
     */
    @Transactional
    public void updateHotelInfo(String contentId, HotelEditFormDto dto) {
        // HotelInfo 존재 확인 (title, adress, tel은 변경 불가)
        hotelInfoRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));
        
        // checkInTime, checkOutTime, cancellationPolicy는 Entity에 없으므로 업데이트하지 않음
        // (필요 시 별도 테이블에 저장)
        
        // HotelDetail 업데이트 (Entity 필드명 사용)
        HotelDetail hotelDetail = hotelDetailRepository.findById(contentId)
            .orElseGet(() -> {
                HotelDetail newDetail = new HotelDetail();
                newDetail.setContentid(contentId);
                return newDetail;
            });
        
        if (dto.getHotelDetail() != null) {
            if (dto.getHotelDetail().getReservationlodging() != null) {
                hotelDetail.setReservationlodging(dto.getHotelDetail().getReservationlodging());
            }
            if (dto.getHotelDetail().getFoodplace() != null) {
                hotelDetail.setFoodplace(dto.getHotelDetail().getFoodplace());
            }
            if (dto.getHotelDetail().getScalelodging() != null) {
                hotelDetail.setScalelodging(dto.getHotelDetail().getScalelodging());
            }
            if (dto.getHotelDetail().getParkinglodging() != null) {
                hotelDetail.setParkinglodging(dto.getHotelDetail().getParkinglodging());
            }
        }
        hotelDetailRepository.save(hotelDetail);
        
        // Dining 업데이트 (Entity 필드명 사용)
        if (dto.getDining() != null) {
            for (HotelEditFormDto.DiningDto diningDto : dto.getDining()) {
                if (diningDto.getDiningIdx() != null) {
                    Optional<Dining> diningOpt = diningRepository.findById(diningDto.getDiningIdx());
                    if (diningOpt.isPresent()) {
                        Dining dining = diningOpt.get();
                        
                        if (diningDto.getName() != null) {
                            dining.setName(diningDto.getName());
                        }
                        if (diningDto.getDescription() != null) {
                            dining.setDescription(diningDto.getDescription());
                        }
                        if (diningDto.getBasePrice() != null) {
                            dining.setBasePrice(diningDto.getBasePrice());
                        }
                        if (diningDto.getTotalSeats() != null) {
                            dining.setTotalSeats(diningDto.getTotalSeats());
                        }
                        
                        // operatingHours 파싱 (예: "09:00 - 21:00" → openTime, closeTime)
                        if (diningDto.getOperatingHours() != null && !diningDto.getOperatingHours().isEmpty()) {
                            String[] times = diningDto.getOperatingHours().split(" - ");
                            if (times.length == 2) {
                                try {
                                    dining.setOpenTime(java.time.LocalTime.parse(times[0].trim()));
                                    dining.setCloseTime(java.time.LocalTime.parse(times[1].trim()));
                                } catch (Exception e) {
                                    // 파싱 실패 시 무시
                                }
                            }
                        }
                        
                        diningRepository.save(dining);
                    }
                }
            }
        }
        
        // HotelImage는 이미지 업로드 시 별도로 처리되므로 여기서는 업데이트하지 않음
    }

    /**
     * 호텔 승인 시 JSON에서 파싱한 데이터를 정규화된 테이블에 저장
     * @param adminIdx 관리자 인덱스
     * @param dto 호텔 정보 DTO
     * @return 생성된 contentId
     */
    @Transactional
    public String createHotelInfoFromDto(Integer adminIdx, HotelEditFormDto dto) {
        log.info("호텔 정보 생성 시작: adminIdx={}", adminIdx);
        
        try {
            // 1. contentId 생성 (UUID 사용)
            String contentId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            log.info("생성된 contentId: {}", contentId);
            
            // 2. Admin 조회
            // status = false(0): 활성 상태, status = true(1): 비활성 상태
            Admin admin = adminRepository.findByAdminIdxAndStatus(adminIdx, false)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다: " + adminIdx));
            
            // 3. HotelInfo 생성 및 저장
            HotelInfo hotelInfo = new HotelInfo();
            hotelInfo.setContentId(contentId);
            hotelInfo.setAdmin(admin);
            hotelInfo.setAdminIdx(adminIdx);
            hotelInfo.setTitle(dto.getHotelInfo().getTitle());
            hotelInfo.setAdress(dto.getHotelInfo().getAdress());
            hotelInfo.setTel(dto.getHotelInfo().getTel());
            hotelInfo.setAreaCode(dto.getArea().getAreaCode());
            hotelInfo.setHotelCategoryCode("B02010100"); // 기본값: 호텔
            hotelInfo.setStatus(0); // 승인 완료 상태
            hotelInfoRepository.save(hotelInfo);
            log.info("✅ HotelInfo 저장 완료: contentId={}, title={}", contentId, hotelInfo.getTitle());
            
            // 4. HotelDetail 생성 및 저장
            HotelDetail hotelDetail = new HotelDetail();
            hotelDetail.setContentid(contentId);
            if (dto.getHotelDetail() != null) {
                hotelDetail.setReservationlodging(dto.getHotelDetail().getReservationlodging());
                hotelDetail.setFoodplace(dto.getHotelDetail().getFoodplace());
                hotelDetail.setScalelodging(dto.getHotelDetail().getScalelodging());
                hotelDetail.setParkinglodging(dto.getHotelDetail().getParkinglodging());
            }
            hotelDetailRepository.save(hotelDetail);
            log.info("✅ HotelDetail 저장 완료: contentId={}", contentId);
            
            // 5. Room 생성 및 저장
            if (dto.getRooms() != null && !dto.getRooms().isEmpty()) {
                for (HotelEditFormDto.RoomDto roomDto : dto.getRooms()) {
                    Room room = new Room();
                    // roomIdx는 DB의 AUTO_INCREMENT에 의해 자동으로 생성되므로 명시적으로 설정하지 않습니다.
                    room.setContentId(contentId); // contentId는 호텔의 고유키로 명시적으로 설정
                    room.setName(roomDto.getName());
                    room.setCapacity(roomDto.getCapacity());
                    room.setBasePrice(roomDto.getBasePrice());
                    room.setRefundable(roomDto.getRefundable());
                    room.setBreakfastIncluded(roomDto.getBreakfastIncluded());
                    room.setSmoking(roomDto.getSmoking());
                    room.setRoomCount(roomDto.getRoomCount());
                    room.setStatus(1); // 활성 상태
                    
                    roomRepository.save(room);
                    Integer savedRoomIdx = room.getRoomIdx(); // DB에서 자동 생성된 roomIdx 확인
                    log.info("✅ Room 저장 완료: contentId={}, roomIdx={}, name={}", contentId, savedRoomIdx, room.getName());
                    
                    // 6. RoomImage 저장
                    if (roomDto.getImages() != null && !roomDto.getImages().isEmpty()) {
                        int imageOrder = 1;
                        for (HotelEditFormDto.RoomImageDto imageDto : roomDto.getImages()) {
                            RoomImage roomImage = new RoomImage();
                            roomImage.setRoomIdx(savedRoomIdx); // DB에서 자동 생성된 roomIdx 사용
                            roomImage.setContentId(contentId);
                            roomImage.setImageUrl(imageDto.getImageUrl());
                            roomImage.setImageOrder(imageOrder);
                            roomImageRepository.save(roomImage);
                            log.info("✅ RoomImage 저장 완료: contentId={}, roomIdx={}, imageOrder={}", contentId, savedRoomIdx, imageOrder);
                            imageOrder++;
                        }
                    }
                }
            }
            
            // 7. HotelImage 저장
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                for (HotelEditFormDto.ImageDto imageDto : dto.getImages()) {
                    HotelImage hotelImage = new HotelImage();
                    hotelImage.setContentId(contentId);
                    hotelImage.setOriginUrl(imageDto.getOriginUrl());
                    hotelImage.setSmallUrl(imageDto.getSmallUrl() != null ? imageDto.getSmallUrl() : imageDto.getOriginUrl());
                    hotelImageRepository.save(hotelImage);
                    log.info("✅ HotelImage 저장 완료: contentId={}, originUrl={}", contentId, imageDto.getOriginUrl());
                }
            }
            
            // 8. Dining 저장
            if (dto.getDining() != null && !dto.getDining().isEmpty()) {
                for (HotelEditFormDto.DiningDto diningDto : dto.getDining()) {
                    Dining dining = new Dining();
                    dining.setContentid(contentId);
                    dining.setName(diningDto.getName());
                    dining.setDescription(diningDto.getDescription());
                    dining.setBasePrice(diningDto.getBasePrice());
                    dining.setTotalSeats(diningDto.getTotalSeats());
                    dining.setStatus(1); // 활성 상태
                    
                    // operatingHours 파싱 (예: "09:00 - 21:00" → openTime, closeTime)
                    if (diningDto.getOperatingHours() != null && !diningDto.getOperatingHours().isEmpty()) {
                        String[] times = diningDto.getOperatingHours().split(" - ");
                        if (times.length == 2) {
                            try {
                                dining.setOpenTime(LocalTime.parse(times[0].trim()));
                                dining.setCloseTime(LocalTime.parse(times[1].trim()));
                            } catch (Exception e) {
                                log.warn("영업시간 파싱 실패: {}", diningDto.getOperatingHours());
                            }
                        }
                    }
                    
                    diningRepository.save(dining);
                    log.info("✅ Dining 저장 완료: contentId={}, name={}", contentId, dining.getName());
                }
            }
            
            log.info("✅ 호텔 정보 생성 완료: contentId={}, adminIdx={}", contentId, adminIdx);
            return contentId;
            
        } catch (Exception e) {
            log.error("❌ 호텔 정보 생성 실패: adminIdx={}, error={}", adminIdx, e.getMessage(), e);
            throw new RuntimeException("호텔 정보 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

