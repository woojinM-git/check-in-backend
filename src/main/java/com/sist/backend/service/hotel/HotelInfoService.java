package com.sist.backend.service.hotel;

import com.sist.backend.dto.admin.HotelEditFormDto;
import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.entity.*;
import com.sist.backend.repository.*;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.HotelDetailRepository;
import com.sist.backend.repository.hotel.HotelImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelInfoService {
    
    private final HotelInfoRepository hotelInfoRepository;
    private final HotelDetailRepository hotelDetailRepository;
    private final DiningRepository diningRepository;
    private final HotelImageRepository hotelImageRepository;
    
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
}

