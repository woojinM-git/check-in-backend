package com.sist.backend.service.hotel;

import com.sist.backend.dto.admin.HotelEditFormDto;
import com.sist.backend.dto.master.HotelInfoDto;
import com.sist.backend.entity.*;
import com.sist.backend.repository.*;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.HotelDetailRepository;
import com.sist.backend.repository.hotel.HotelImageRepository;
import com.sist.backend.repository.hotel.HotelLocationRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private final HotelLocationRepository hotelLocationRepository;
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

    public Page<HotelInfoDto> findAllHotelWithDetailsAsDto(String search, Pageable pageable) {
        // search가 null이거나 빈 문자열이면 단순 조회 쿼리 사용 (성능 최적화)
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        Page<HotelInfo> hotelInfoPage;
        if (searchTerm == null) {
            // 검색어가 없을 때: WHERE 절이 없는 단순 쿼리 사용
            hotelInfoPage = hotelInfoRepository.findAllHotelWithDetailsAsDto(pageable);
        } else {
            // 검색어가 있을 때: WHERE 절이 있는 검색 쿼리 사용
            hotelInfoPage = hotelInfoRepository.findAllHotelWithDetailsAsDtoWithSearch(searchTerm, pageable);
        }
        
        return hotelInfoPage.map(HotelInfoDto::hotelInfoDto);
    }

    public Optional<String> findContentIdByAdminIdx(Integer adminIdx) {
        return hotelInfoRepository.findContentIdByAdminIdx(adminIdx);
    }

    /**
     * contentId로 호텔 정보 조회
     * @param contentId 호텔 ID
     * @return HotelInfo
     * @throws IllegalArgumentException 호텔을 찾을 수 없을 때
     */
    public HotelInfo findById(String contentId) {
        return hotelInfoRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));
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
     * 정규화된 테이블에서 호텔 정보를 조회하여 등록 폼 구조로 변환 (병렬 처리)
     */
    public HotelEditFormDto getHotelInfoForEdit(String contentId) {
        try {
            // 병렬로 모든 데이터 조회
            CompletableFuture<HotelInfo> hotelInfoFuture = CompletableFuture.supplyAsync(() ->
                hotelInfoRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."))
            );
            
            CompletableFuture<HotelDetail> hotelDetailFuture = CompletableFuture.supplyAsync(() ->
                hotelDetailRepository.findById(contentId)
                    .orElse(new HotelDetail())
            );
            
            CompletableFuture<List<Dining>> diningsFuture = CompletableFuture.supplyAsync(() ->
                diningRepository.findAllByContentid(contentId)
            );
            
            CompletableFuture<List<HotelImage>> hotelImagesFuture = CompletableFuture.supplyAsync(() ->
                hotelImageRepository.findTop10ByContentIdOrderByIdAsc(contentId)
            );
            
            CompletableFuture<Optional<HotelLocation>> hotelLocationFuture = CompletableFuture.supplyAsync(() ->
                hotelLocationRepository.findByContentId(contentId)
            );
            
            CompletableFuture<List<Room>> roomsFuture = CompletableFuture.supplyAsync(() ->
                roomRepository.findByContentId(contentId).stream()
                    .filter(room -> room.getStatus() != null && room.getStatus() == 1)
                    .collect(Collectors.toList())
            );
            
            // 모든 조회 완료 대기
            CompletableFuture.allOf(
                hotelInfoFuture, hotelDetailFuture, diningsFuture, 
                hotelImagesFuture, hotelLocationFuture, roomsFuture
            ).join();
            
            // 결과 가져오기
            HotelInfo hotelInfo = hotelInfoFuture.join();
            HotelDetail hotelDetail = hotelDetailFuture.join();
            List<Dining> dinings = diningsFuture.join();
            List<HotelImage> hotelImages = hotelImagesFuture.join();
            Optional<HotelLocation> hotelLocationOpt = hotelLocationFuture.join();
            List<Room> rooms = roomsFuture.join();
            
            // RoomImage 병렬 조회 (각 Room에 대해)
            List<CompletableFuture<Map<Integer, List<RoomImage>>>> roomImageFutures = rooms.stream()
                .map(room -> CompletableFuture.supplyAsync(() -> {
                    List<RoomImage> images = roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(
                        room.getRoomIdx(), contentId
                    );
                    return Map.of(room.getRoomIdx(), images);
                }))
                .collect(Collectors.toList());
            
            // 모든 RoomImage 조회 완료 대기
            CompletableFuture.allOf(roomImageFutures.toArray(new CompletableFuture[0])).join();
            
            // RoomImage 결과를 Map으로 합치기
            Map<Integer, List<RoomImage>> roomImageMap = roomImageFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            
            // Entity 필드명에 맞춰 DTO 변환
            HotelEditFormDto.HotelInfoDto hotelInfoDto = HotelEditFormDto.HotelInfoDto.builder()
                .title(hotelInfo.getTitle())
                .adress(hotelInfo.getAdress())
                .tel(hotelInfo.getTel())
                .imageUrl(hotelInfo.getImageUrl())
                .latitude(hotelLocationOpt.map(loc -> loc.getMapY() != null ? loc.getMapY().toString() : "").orElse(""))
                .longitude(hotelLocationOpt.map(loc -> loc.getMapX() != null ? loc.getMapX().toString() : "").orElse(""))
                .build();
            
            HotelEditFormDto.HotelDetailDto hotelDetailDto = HotelEditFormDto.HotelDetailDto.builder()
                .reservationlodging(hotelDetail.getReservationlodging() != null ? hotelDetail.getReservationlodging() : "")
                .foodplace(hotelDetail.getFoodplace() != null ? hotelDetail.getFoodplace() : "")
                .scalelodging(hotelDetail.getScalelodging() != null ? hotelDetail.getScalelodging() : "")
                .parkinglodging(hotelDetail.getParkinglodging() != null ? hotelDetail.getParkinglodging() : "")
                .roomcount(hotelDetail.getRoomcount() != null ? hotelDetail.getRoomcount() : "")
                .build();
            
            HotelEditFormDto.AreaDto areaDto = HotelEditFormDto.AreaDto.builder()
                .areaCode(hotelInfo.getAreaCode() != null ? hotelInfo.getAreaCode() : "")
                .nearbyAttractions("") // 프론트엔드용 (Entity에 없음)
                .transportation("") // 프론트엔드용 (Entity에 없음)
                .build();
            
            List<HotelEditFormDto.ImageDto> imageDtos = hotelImages.stream().map(image ->
                HotelEditFormDto.ImageDto.builder()
                    .id(Long.valueOf(image.getId())) // id는 항상 존재 (AUTO_INCREMENT PRIMARY KEY)
                    .originUrl(image.getOriginUrl())
                    .smallUrl(image.getSmallUrl())
                    .build()
            ).collect(Collectors.toList());
            
            List<HotelEditFormDto.DiningDto> diningDtos = dinings.stream()
                .filter(dining -> dining.getStatus() == null || dining.getStatus() == 0 || dining.getStatus() == 1) // 모든 상태 포함
                .map(dining ->
                    HotelEditFormDto.DiningDto.builder()
                        .diningIdx(dining.getDiningIdx())
                        .name(dining.getName())
                        .operatingHours(dining.getOpenTime() != null && dining.getCloseTime() != null 
                            ? dining.getOpenTime().toString() + " - " + dining.getCloseTime().toString()
                            : "")
                        .description(dining.getDescription())
                        .content(dining.getContent())
                        .basePrice(dining.getBasePrice())
                        .totalSeats(dining.getTotalSeats())
                        .slotDuration(dining.getSlotDuration())
                        .maxGuestsPerSlot(dining.getMaxGuestsPerSlot())
                        .status(dining.getStatus())
                        .build()
                ).collect(Collectors.toList());
            
            // Room DTO 변환 (RoomImage 포함)
            List<HotelEditFormDto.RoomDto> roomDtos = rooms.stream().map(room -> {
                List<RoomImage> roomImages = roomImageMap.getOrDefault(room.getRoomIdx(), List.of());
                List<HotelEditFormDto.RoomImageDto> roomImageDtos = roomImages.stream()
                    .map(img -> HotelEditFormDto.RoomImageDto.builder()
                        .roomImageIdx(img.getRoomImageIdx()) // 고유키 포함
                        .imageUrl(img.getImageUrl())
                        .imageOrder(img.getImageOrder())
                        .build())
                    .collect(Collectors.toList());
                
                return HotelEditFormDto.RoomDto.builder()
                    .roomIdx(room.getRoomIdx())
                    .name(room.getName())
                    .capacity(room.getCapacity())
                    .basePrice(room.getBasePrice())
                    .refundable(room.getRefundable() != null ? room.getRefundable() : false)
                    .breakfastIncluded(room.getBreakfastIncluded() != null ? room.getBreakfastIncluded() : false)
                    .smoking(room.getSmoking() != null ? room.getSmoking() : false)
                    .roomCount(room.getRoomCount() != null ? room.getRoomCount() : 1)
                    .status(room.getStatus() != null ? room.getStatus() : 1)
                    .imageUrl(room.getImageUrl())
                    .images(roomImageDtos)
                    .build();
            }).collect(Collectors.toList());
            
            return HotelEditFormDto.builder()
                .hotelInfo(hotelInfoDto)
                .hotelDetail(hotelDetailDto)
                .area(areaDto)
                .images(imageDtos)
                .dining(diningDtos)
                .rooms(roomDtos)
                .build();
        } catch (Exception e) {
            log.error("호텔 정보 조회 실패: contentId={}", contentId, e);
            throw new RuntimeException("호텔 정보를 불러오는데 실패했습니다.", e);
        }
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
        
        // HotelLocation 업데이트 (좌표 정보)
        if (dto.getHotelInfo() != null && dto.getHotelInfo().getLatitude() != null && 
            dto.getHotelInfo().getLongitude() != null && 
            !dto.getHotelInfo().getLatitude().isEmpty() && 
            !dto.getHotelInfo().getLongitude().isEmpty()) {
            HotelLocation hotelLocation = hotelLocationRepository.findByContentId(contentId)
                .orElseGet(() -> {
                    HotelLocation newLocation = new HotelLocation();
                    newLocation.setContentId(contentId);
                    return newLocation;
                });
            
            try {
                // 위도(latitude) → mapY, 경도(longitude) → mapX
                hotelLocation.setMapY(new java.math.BigDecimal(dto.getHotelInfo().getLatitude()));
                hotelLocation.setMapX(new java.math.BigDecimal(dto.getHotelInfo().getLongitude()));
                hotelLocationRepository.save(hotelLocation);
                log.info("✅ HotelLocation 업데이트 완료: contentId={}, mapY={}, mapX={}", 
                    contentId, hotelLocation.getMapY(), hotelLocation.getMapX());
            } catch (NumberFormatException e) {
                log.warn("좌표 파싱 실패: contentId={}, latitude={}, longitude={}", 
                    contentId, dto.getHotelInfo().getLatitude(), dto.getHotelInfo().getLongitude());
            }
        }
        
        // HotelInfo.imageUrl 업데이트 (대표 이미지)
        if (dto.getHotelInfo() != null && dto.getHotelInfo().getImageUrl() != null) {
            HotelInfo hotelInfo = hotelInfoRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));
            hotelInfo.setImageUrl(dto.getHotelInfo().getImageUrl());
            hotelInfoRepository.save(hotelInfo);
            log.info("✅ HotelInfo.imageUrl 업데이트 완료: contentId={}, imageUrl={}", 
                contentId, dto.getHotelInfo().getImageUrl());
        }
        
        // HotelDetail.roomcount 업데이트 (객실 총 개수)
        if (dto.getRooms() != null) {
            hotelDetail.setRoomcount(String.valueOf(dto.getRooms().size()));
            hotelDetailRepository.save(hotelDetail);
            log.info("✅ HotelDetail.roomcount 업데이트 완료: contentId={}, roomcount={}", 
                contentId, hotelDetail.getRoomcount());
        }
        
        // Room 업데이트/생성/삭제 (Soft Delete 원칙 적용)
        if (dto.getRooms() != null) {
            // 기존 활성 객실 목록 조회 (status = 1인 객실만)
            List<Room> existingRooms = roomRepository.findByContentId(contentId);
            List<Integer> existingActiveRoomIdxs = existingRooms.stream()
                .filter(room -> room.getStatus() != null && room.getStatus() == 1)
                .map(Room::getRoomIdx)
                .collect(Collectors.toList());
            
            // 전송된 객실의 roomIdx 목록 (null이 아닌 것만)
            List<Integer> submittedRoomIdxs = dto.getRooms().stream()
                .map(HotelEditFormDto.RoomDto::getRoomIdx)
                .filter(roomIdx -> roomIdx != null)
                .collect(Collectors.toList());
            
            // Soft Delete 처리: 기존에는 있지만 전송된 데이터에는 없는 활성 객실은 status = 0으로 변경
            List<Integer> roomsToDeactivate = existingActiveRoomIdxs.stream()
                .filter(roomIdx -> !submittedRoomIdxs.contains(roomIdx))
                .collect(Collectors.toList());
            
            // 객실 Soft Delete (status = 0으로 변경)
            for (Integer roomIdx : roomsToDeactivate) {
                roomRepository.findByRoomIdx(roomIdx).ifPresent(room -> {
                    room.setStatus(0); // 0 = 사용불가 (삭제됨)
                    roomRepository.save(room);
                    log.info("✅ Room Soft Delete 완료: contentId={}, roomIdx={}, name={}", 
                        contentId, roomIdx, room.getName());
                });
            }
            
            // 객실 업데이트/생성
            for (HotelEditFormDto.RoomDto roomDto : dto.getRooms()) {
                Room room;
                
                if (roomDto.getRoomIdx() != null && existingActiveRoomIdxs.contains(roomDto.getRoomIdx())) {
                    // 기존 객실 업데이트 (roomIdx가 있고 기존 활성 객실 목록에 있는 경우)
                    room = roomRepository.findByRoomIdx(roomDto.getRoomIdx())
                        .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다: " + roomDto.getRoomIdx()));
                } else {
                    // 신규 객실 생성 (roomIdx가 없거나 기존 활성 객실 목록에 없는 경우)
                    room = new Room();
                    room.setContentId(contentId);
                    room.setStatus(1); // 신규 생성 시 기본값: 활성 상태
                }
                
                // 객실 정보 업데이트
                if (roomDto.getName() != null) {
                    room.setName(roomDto.getName());
                }
                if (roomDto.getCapacity() != null) {
                    room.setCapacity(roomDto.getCapacity());
                }
                if (roomDto.getBasePrice() != null) {
                    room.setBasePrice(roomDto.getBasePrice());
                }
                if (roomDto.getRefundable() != null) {
                    room.setRefundable(roomDto.getRefundable());
                } else {
                    room.setRefundable(true); // 기본값
                }
                if (roomDto.getBreakfastIncluded() != null) {
                    room.setBreakfastIncluded(roomDto.getBreakfastIncluded());
                } else {
                    room.setBreakfastIncluded(false); // 기본값
                }
                if (roomDto.getSmoking() != null) {
                    room.setSmoking(roomDto.getSmoking());
                } else {
                    room.setSmoking(false); // 기본값
                }
                if (roomDto.getRoomCount() != null) {
                    room.setRoomCount(roomDto.getRoomCount());
                } else {
                    room.setRoomCount(1); // 기본값
                }
                if (roomDto.getStatus() != null) {
                    room.setStatus(roomDto.getStatus());
                } else {
                    room.setStatus(1); // 기본값
                }
                if (roomDto.getImageUrl() != null) {
                    room.setImageUrl(roomDto.getImageUrl());
                }
                
                roomRepository.save(room);
                Integer savedRoomIdx = room.getRoomIdx();
                log.info("✅ Room 저장 완료: contentId={}, roomIdx={}, name={}", 
                    contentId, savedRoomIdx, room.getName());
                
                // RoomImage 업데이트 (고유키 기반 소프트 삭제 및 신규 추가)
                if (roomDto.getImages() != null) {
                    // 기존 활성 이미지 목록 조회
                    List<RoomImage> existingActiveImages = roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(savedRoomIdx, contentId);
                    List<Integer> existingActiveImageIdxs = existingActiveImages.stream()
                        .map(RoomImage::getRoomImageIdx)
                        .collect(Collectors.toList());
                    
                    // 전송된 이미지의 고유키 목록 (null이 아닌 것만)
                    List<Integer> submittedImageIdxs = roomDto.getImages().stream()
                        .map(HotelEditFormDto.RoomImageDto::getRoomImageIdx)
                        .filter(idx -> idx != null)
                        .collect(Collectors.toList());
                    
                    // 제거된 이미지: 기존에는 있지만 전송되지 않은 이미지 → 소프트 삭제
                    List<Integer> imagesToDelete = existingActiveImageIdxs.stream()
                        .filter(idx -> !submittedImageIdxs.contains(idx))
                        .collect(Collectors.toList());
                    
                    for (Integer imageIdx : imagesToDelete) {
                        roomImageRepository.findById(imageIdx).ifPresent(image -> {
                            image.setStatus(0);
                            image.setDeletedAt(java.time.LocalDateTime.now());
                            // UNIQUE 제약조건 충돌 방지를 위해 imageOrder를 roomImageIdx + 10000으로 변경
                            // (각 삭제된 이미지마다 고유한 imageOrder 보장, 활성 이미지 범위(1-10)와 충돌 방지)
                            image.setImageOrder(imageIdx + 10000); // roomImageIdx + 10000을 imageOrder로 사용
                            roomImageRepository.save(image);
                            log.info("✅ RoomImage 소프트 삭제 완료: contentId={}, roomIdx={}, roomImageIdx={}, imageOrder={}", 
                                contentId, savedRoomIdx, imageIdx, imageIdx + 10000);
                        });
                    }
                    
                    // 먼저 기존 이미지의 순서를 업데이트 (신규 이미지 INSERT 전에 충돌 방지)
                    for (HotelEditFormDto.RoomImageDto imageDto : roomDto.getImages()) {
                        if (imageDto.getImageUrl() != null && !imageDto.getImageUrl().isEmpty()) {
                            if (imageDto.getRoomImageIdx() != null) {
                                // 기존 이미지: 순서만 먼저 업데이트
                                Integer imageOrder = imageDto.getImageOrder() != null ? imageDto.getImageOrder() : 1;
                                roomImageRepository.findById(imageDto.getRoomImageIdx()).ifPresent(existingImage -> {
                                    if (!existingImage.getImageOrder().equals(imageOrder)) {
                                        existingImage.setImageOrder(imageOrder);
                                        roomImageRepository.save(existingImage);
                                        log.info("✅ RoomImage 순서 업데이트: contentId={}, roomIdx={}, roomImageIdx={}, imageOrder={}", 
                                            contentId, savedRoomIdx, imageDto.getRoomImageIdx(), imageOrder);
                                    }
                                });
                            }
                        }
                    }
                    
                    // 그 다음 신규 이미지 INSERT (기존 이미지 순서 업데이트 완료 후)
                    for (HotelEditFormDto.RoomImageDto imageDto : roomDto.getImages()) {
                        if (imageDto.getImageUrl() != null && !imageDto.getImageUrl().isEmpty()) {
                            if (imageDto.getRoomImageIdx() == null) {
                                // 신규 이미지: INSERT (프론트엔드에서 전달한 imageOrder 사용)
                                Integer imageOrder = imageDto.getImageOrder() != null ? imageDto.getImageOrder() : 1;
                                
                                // UNIQUE 제약조건 충돌 방지: 해당 imageOrder가 이미 사용 중인지 확인
                                // (활성 이미지만 확인, 삭제된 이미지는 imageOrder >= 10000이므로 충돌 없음)
                                final Integer finalImageOrder = imageOrder; // final 변수로 복사
                                boolean orderExists = roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(savedRoomIdx, contentId)
                                    .stream()
                                    .anyMatch(img -> img.getImageOrder().equals(finalImageOrder) && img.getStatus() == 1);
                                
                                Integer adjustedImageOrder = imageOrder;
                                if (orderExists) {
                                    // 충돌 발생 시 사용 가능한 다음 순서 찾기
                                    List<Integer> usedOrders = roomImageRepository.findByRoomIdxAndContentIdOrderByImageOrderAsc(savedRoomIdx, contentId)
                                        .stream()
                                        .filter(img -> img.getStatus() == 1)
                                        .map(RoomImage::getImageOrder)
                                        .collect(Collectors.toList());
                                    
                                    for (int i = 1; i <= 10; i++) {
                                        if (!usedOrders.contains(i)) {
                                            adjustedImageOrder = i;
                                            break;
                                        }
                                    }
                                    log.warn("⚠️ RoomImage imageOrder 충돌 감지, 자동 조정: contentId={}, roomIdx={}, imageOrder={}", 
                                        contentId, savedRoomIdx, adjustedImageOrder);
                                }
                                
                                RoomImage roomImage = new RoomImage();
                                roomImage.setRoomIdx(savedRoomIdx);
                                roomImage.setContentId(contentId);
                                roomImage.setImageUrl(imageDto.getImageUrl());
                                roomImage.setImageOrder(adjustedImageOrder);
                                roomImage.setStatus(1); // 활성 상태
                                roomImageRepository.save(roomImage);
                                log.info("✅ RoomImage 신규 저장 완료: contentId={}, roomIdx={}, imageOrder={}, imageUrl={}", 
                                    contentId, savedRoomIdx, adjustedImageOrder, imageDto.getImageUrl());
                            }
                        }
                    }
                }
            }
        }
        
        // HotelImage 업데이트 (고유키 기반 소프트 삭제 및 신규 추가)
        if (dto.getImages() != null) {
            // 기존 활성 이미지 목록 조회
            List<HotelImage> existingActiveImages = hotelImageRepository.findTop10ByContentIdOrderByIdAsc(contentId);
            List<Integer> existingActiveImageIds = existingActiveImages.stream()
                .map(HotelImage::getId)
                .collect(Collectors.toList());
            
            // 전송된 이미지의 고유키 목록 (null이 아닌 것만)
            List<Integer> submittedImageIds = dto.getImages().stream()
                .map(HotelEditFormDto.ImageDto::getId)
                .filter(id -> id != null)
                .map(Long::intValue) // Long을 Integer로 변환
                .collect(Collectors.toList());
            
            // 제거된 이미지: 기존에는 있지만 전송되지 않은 이미지 → 소프트 삭제
            List<Integer> imagesToDelete = existingActiveImageIds.stream()
                .filter(id -> !submittedImageIds.contains(id))
                .collect(Collectors.toList());
            
            for (Integer imageId : imagesToDelete) {
                hotelImageRepository.findById(imageId).ifPresent(image -> {
                    image.setStatus(0);
                    image.setDeletedAt(java.time.LocalDateTime.now());
                    hotelImageRepository.save(image);
                    log.info("✅ HotelImage 소프트 삭제 완료: contentId={}, id={}, originUrl={}", 
                        contentId, imageId, image.getOriginUrl());
                });
            }
            
            // 새 이미지 저장 (고유키가 없는 이미지만 INSERT)
            for (HotelEditFormDto.ImageDto imageDto : dto.getImages()) {
                if (imageDto.getOriginUrl() != null && !imageDto.getOriginUrl().isEmpty()) {
                    if (imageDto.getId() == null) {
                        // 신규 이미지: INSERT
                        HotelImage hotelImage = new HotelImage();
                        hotelImage.setContentId(contentId);
                        hotelImage.setOriginUrl(imageDto.getOriginUrl());
                        hotelImage.setSmallUrl(imageDto.getSmallUrl() != null ? imageDto.getSmallUrl() : imageDto.getOriginUrl());
                        hotelImage.setStatus(1); // 활성 상태
                        hotelImageRepository.save(hotelImage);
                        log.info("✅ HotelImage 신규 저장 완료: contentId={}, originUrl={}", 
                            contentId, imageDto.getOriginUrl());
                    }
                    // 기존 이미지는 그대로 유지 (이미 활성 상태)
                }
            }
        }
        
        // Dining 업데이트/생성/삭제
        if (dto.getDining() != null) {
            // 기존 다이닝 목록 조회 (모든 상태 포함 - 비활성화된 다이닝도 포함)
            List<Dining> existingDinings = diningRepository.findAllByContentid(contentId);
            List<Integer> existingDiningIdxs = existingDinings.stream()
                .map(Dining::getDiningIdx)
                .collect(Collectors.toList());
            
            // 전송된 다이닝의 diningIdx 목록
            List<Integer> submittedDiningIdxs = dto.getDining().stream()
                .map(HotelEditFormDto.DiningDto::getDiningIdx)
                .filter(diningIdx -> diningIdx != null)
                .collect(Collectors.toList());
            
            // 삭제할 다이닝 찾기 (기존에는 있지만 전송된 데이터에는 없는 다이닝)
            List<Integer> diningsToDelete = existingDiningIdxs.stream()
                .filter(diningIdx -> !submittedDiningIdxs.contains(diningIdx))
                .collect(Collectors.toList());
            
            // 다이닝 삭제 (soft delete: status를 1로 변경)
            for (Integer diningIdx : diningsToDelete) {
                diningRepository.findById(diningIdx).ifPresent(dining -> {
                    dining.setStatus(1); // 1 = 삭제됨
                    diningRepository.save(dining);
                    log.info("✅ Dining 삭제 완료: contentId={}, diningIdx={}", contentId, diningIdx);
                });
            }
            
            // 다이닝 업데이트/생성
            for (HotelEditFormDto.DiningDto diningDto : dto.getDining()) {
                Dining dining;
                
                if (diningDto.getDiningIdx() != null && existingDiningIdxs.contains(diningDto.getDiningIdx())) {
                    // 기존 다이닝 업데이트
                    dining = diningRepository.findById(diningDto.getDiningIdx())
                        .orElseThrow(() -> new IllegalArgumentException("다이닝을 찾을 수 없습니다: " + diningDto.getDiningIdx()));
                } else {
                    // 신규 다이닝 생성
                    dining = new Dining();
                    dining.setContentid(contentId);
                    dining.setStatus(0); // 0 = 활성
                }
                
                if (diningDto.getName() != null) {
                    dining.setName(diningDto.getName());
                }
                // description은 null이 아니면 항상 업데이트 (빈 문자열도 허용)
                dining.setDescription(diningDto.getDescription() != null ? diningDto.getDescription() : "");
                // content는 null이 아니면 항상 업데이트 (빈 문자열도 허용)
                dining.setContent(diningDto.getContent() != null ? diningDto.getContent() : "");
                if (diningDto.getBasePrice() != null) {
                    dining.setBasePrice(diningDto.getBasePrice());
                }
                if (diningDto.getTotalSeats() != null) {
                    dining.setTotalSeats(diningDto.getTotalSeats());
                }
                if (diningDto.getSlotDuration() != null) {
                    dining.setSlotDuration(diningDto.getSlotDuration());
                }
                if (diningDto.getMaxGuestsPerSlot() != null) {
                    dining.setMaxGuestsPerSlot(diningDto.getMaxGuestsPerSlot());
                }
                // status 업데이트 (비활성화된 다이닝을 다시 활성화할 수 있도록)
                if (diningDto.getStatus() != null) {
                    dining.setStatus(diningDto.getStatus());
                }
                
                // operatingHours 파싱 (예: "09:00 - 21:00" → openTime, closeTime)
                if (diningDto.getOperatingHours() != null && !diningDto.getOperatingHours().isEmpty()) {
                    // " - " 또는 "-" 형식 모두 지원
                    String[] times = diningDto.getOperatingHours().split("\\s*-\\s*");
                    if (times.length == 2) {
                        try {
                            dining.setOpenTime(java.time.LocalTime.parse(times[0].trim()));
                            dining.setCloseTime(java.time.LocalTime.parse(times[1].trim()));
                            log.info("✅ 운영시간 파싱 성공: openTime={}, closeTime={}", 
                                dining.getOpenTime(), dining.getCloseTime());
                        } catch (Exception e) {
                            log.warn("운영시간 파싱 실패: contentId={}, operatingHours={}, error={}", 
                                contentId, diningDto.getOperatingHours(), e.getMessage());
                            // 파싱 실패 시 null로 설정
                            dining.setOpenTime(null);
                            dining.setCloseTime(null);
                        }
                    } else if (times.length == 1 && !times[0].trim().isEmpty()) {
                        // 단일 시간만 있는 경우 openTime으로만 설정
                        try {
                            dining.setOpenTime(java.time.LocalTime.parse(times[0].trim()));
                            dining.setCloseTime(null);
                            log.info("✅ 운영시간 파싱 성공 (단일 시간): openTime={}", dining.getOpenTime());
                        } catch (Exception e) {
                            log.warn("운영시간 파싱 실패: contentId={}, operatingHours={}, error={}", 
                                contentId, diningDto.getOperatingHours(), e.getMessage());
                            dining.setOpenTime(null);
                            dining.setCloseTime(null);
                        }
                    } else {
                        dining.setOpenTime(null);
                        dining.setCloseTime(null);
                    }
                } else {
                    // operatingHours가 없으면 null로 설정
                    dining.setOpenTime(null);
                    dining.setCloseTime(null);
                }
                
                diningRepository.save(dining);
                log.info("✅ Dining 저장 완료: contentId={}, diningIdx={}, name={}", 
                    contentId, dining.getDiningIdx(), dining.getName());
            }
        }
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
            
            // 2. Admin 존재 여부 확인 (유효성 검증)
            if (!adminRepository.findByAdminIdxAndStatus(adminIdx, false).isPresent()) {
                throw new IllegalArgumentException("관리자를 찾을 수 없습니다: " + adminIdx);
            }
            
            // 3. HotelInfo 생성 및 저장
            HotelInfo hotelInfo = new HotelInfo();
            hotelInfo.setContentId(contentId);
            hotelInfo.setAdminIdx(adminIdx); // 직접 adminIdx 설정 (성능 최적화)
            hotelInfo.setTitle(dto.getHotelInfo().getTitle());
            
            // 주소 처리: baseAddress와 detailAddress가 있으면 합쳐서 저장, 없으면 기존 adress 사용
            String finalAddress;
            if (dto.getHotelInfo().getBaseAddress() != null && !dto.getHotelInfo().getBaseAddress().isEmpty()) {
                // baseAddress와 detailAddress를 합쳐서 저장
                String baseAddr = dto.getHotelInfo().getBaseAddress();
                String detailAddr = (dto.getHotelInfo().getDetailAddress() != null && !dto.getHotelInfo().getDetailAddress().isEmpty()) 
                    ? " " + dto.getHotelInfo().getDetailAddress() 
                    : "";
                finalAddress = baseAddr + detailAddr;
            } else {
                // 하위 호환성: 기존 adress 필드 사용
                finalAddress = dto.getHotelInfo().getAdress() != null ? dto.getHotelInfo().getAdress() : "";
            }
            hotelInfo.setAdress(finalAddress);
            hotelInfo.setTel(dto.getHotelInfo().getTel());
            hotelInfo.setImageUrl(dto.getHotelInfo().getImageUrl()); // 대표 이미지 URL 설정
            // areaCode 설정 (area가 null이면 null 또는 빈 문자열로 설정)
            if (dto.getArea() != null && dto.getArea().getAreaCode() != null) {
                hotelInfo.setAreaCode(dto.getArea().getAreaCode());
            } else {
                hotelInfo.setAreaCode(null); // area가 없으면 null로 설정
            }
            hotelInfo.setHotelCategoryCode("B02010100"); // 기본값: 호텔
            hotelInfo.setStatus(0); // 승인 완료 상태
            hotelInfoRepository.save(hotelInfo);
            log.info("✅ HotelInfo 저장 완료: contentId={}, title={}, imageUrl={}", contentId, hotelInfo.getTitle(), hotelInfo.getImageUrl());
            
               // 4. HotelDetail 생성 및 저장
            HotelDetail hotelDetail = new HotelDetail();
            hotelDetail.setContentid(contentId);
            if (dto.getHotelDetail() != null) {
                hotelDetail.setReservationlodging(dto.getHotelDetail().getReservationlodging());
                hotelDetail.setFoodplace(dto.getHotelDetail().getFoodplace());
                hotelDetail.setScalelodging(dto.getHotelDetail().getScalelodging());
                hotelDetail.setParkinglodging(dto.getHotelDetail().getParkinglodging());
            }
            // roomcount는 객실 총 개수로 설정 (dto.getRooms()의 크기)
            if (dto.getRooms() != null && !dto.getRooms().isEmpty()) {
                hotelDetail.setRoomcount(String.valueOf(dto.getRooms().size()));
            } else {
                hotelDetail.setRoomcount("0");
            }
            hotelDetailRepository.save(hotelDetail);
            log.info("✅ HotelDetail 저장 완료: contentId={}, roomcount={}", contentId, hotelDetail.getRoomcount());
            
            // 5. HotelLocation 생성 및 저장 (좌표 정보)
            if (dto.getHotelInfo() != null && dto.getHotelInfo().getLatitude() != null && 
                dto.getHotelInfo().getLongitude() != null && 
                !dto.getHotelInfo().getLatitude().isEmpty() && 
                !dto.getHotelInfo().getLongitude().isEmpty()) {
                try {
                    HotelLocation hotelLocation = new HotelLocation();
                    hotelLocation.setContentId(contentId);
                    // 위도(latitude) → mapY, 경도(longitude) → mapX
                    hotelLocation.setMapY(new java.math.BigDecimal(dto.getHotelInfo().getLatitude()));
                    hotelLocation.setMapX(new java.math.BigDecimal(dto.getHotelInfo().getLongitude()));
                    hotelLocationRepository.save(hotelLocation);
                    log.info("✅ HotelLocation 저장 완료: contentId={}, mapX={}, mapY={}", 
                        contentId, hotelLocation.getMapX(), hotelLocation.getMapY());
                } catch (Exception e) {
                    log.warn("⚠️ HotelLocation 저장 실패: contentId={}, error={}", contentId, e.getMessage());
                }
            }
            
            // 6. Room 생성 및 저장
            if (dto.getRooms() != null && !dto.getRooms().isEmpty()) {
                for (HotelEditFormDto.RoomDto roomDto : dto.getRooms()) {
                    Room room = new Room();
                    // roomIdx는 DB의 AUTO_INCREMENT에 의해 자동으로 생성되므로 명시적으로 설정하지 않습니다.
                    room.setContentId(contentId); // contentId는 호텔의 고유키로 명시적으로 설정
                    room.setName(roomDto.getName());
                    room.setCapacity(roomDto.getCapacity());
                    room.setBasePrice(roomDto.getBasePrice());
                    // refundable: Boolean이지만 DB에는 int로 저장됨 (true=1, false=0), 기본값 true (환불 가능)
                    room.setRefundable(roomDto.getRefundable() != null ? roomDto.getRefundable() : true);
                    room.setBreakfastIncluded(roomDto.getBreakfastIncluded());
                    room.setSmoking(roomDto.getSmoking());
                    // roomCount는 기본값 1로 설정 (null이면 1)
                    room.setRoomCount(roomDto.getRoomCount() != null ? roomDto.getRoomCount() : 1);
                    // status는 사용자가 선택한 값 또는 기본값 1
                    room.setStatus(roomDto.getStatus() != null ? roomDto.getStatus() : 1);
                    // imageUrl은 객실 대표 이미지 (1장)
                    room.setImageUrl(roomDto.getImageUrl());
                    
                    roomRepository.save(room);
                    Integer savedRoomIdx = room.getRoomIdx(); // DB에서 자동 생성된 roomIdx 확인
                    log.info("✅ Room 저장 완료: contentId={}, roomIdx={}, name={}, imageUrl={}", contentId, savedRoomIdx, room.getName(), room.getImageUrl());
                    
                    // 6. RoomImage 저장
                    if (roomDto.getImages() != null && !roomDto.getImages().isEmpty()) {
                        int imageOrder = 1;
                        for (HotelEditFormDto.RoomImageDto imageDto : roomDto.getImages()) {
                            RoomImage roomImage = new RoomImage();
                            roomImage.setRoomIdx(savedRoomIdx); // DB에서 자동 생성된 roomIdx 사용
                            roomImage.setContentId(contentId);
                            roomImage.setImageUrl(imageDto.getImageUrl());
                            roomImage.setImageOrder(imageOrder);
                            roomImage.setStatus(1); // 활성 상태
                            roomImageRepository.save(roomImage);
                            log.info("✅ RoomImage 저장 완료: contentId={}, roomIdx={}, imageOrder={}", contentId, savedRoomIdx, imageOrder);
                            imageOrder++;
                        }
                    }
                }
            }
            
            // 7. HotelImage 저장 또는 업데이트
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                for (HotelEditFormDto.ImageDto imageDto : dto.getImages()) {
                    if (imageDto.getId() != null) {
                        // 이미 DB에 저장된 이미지 (등록 페이지에서 업로드한 이미지): contentId만 업데이트
                        hotelImageRepository.findById(imageDto.getId().intValue()).ifPresent(existingImage -> {
                            if (existingImage.getContentId() == null) {
                                existingImage.setContentId(contentId);
                                hotelImageRepository.save(existingImage);
                                log.info("✅ HotelImage contentId 업데이트 완료: id={}, contentId={}, originUrl={}", 
                                    existingImage.getId(), contentId, existingImage.getOriginUrl());
                            }
                        });
                    } else {
                        // 신규 이미지: INSERT
                        HotelImage hotelImage = new HotelImage();
                        hotelImage.setContentId(contentId);
                        hotelImage.setOriginUrl(imageDto.getOriginUrl());
                        hotelImage.setSmallUrl(imageDto.getSmallUrl() != null ? imageDto.getSmallUrl() : imageDto.getOriginUrl());
                        hotelImage.setStatus(1); // 활성 상태
                        hotelImageRepository.save(hotelImage);
                        log.info("✅ HotelImage 신규 저장 완료: contentId={}, originUrl={}", 
                            contentId, imageDto.getOriginUrl());
                    }
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

