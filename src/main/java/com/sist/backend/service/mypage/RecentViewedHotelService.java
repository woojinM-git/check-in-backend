package com.sist.backend.service.mypage;

import com.sist.backend.dto.mypage.RecentViewedHotelResponse;
import com.sist.backend.entity.HotelInfo;
import com.sist.backend.entity.RecentViewedHotel;
import com.sist.backend.repository.RecentViewedHotelRepository;
import com.sist.backend.repository.hotel.HotelInfoRepository;
import com.sist.backend.repository.hotel.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentViewedHotelService {

    private static final int MAX_RECENT_VIEWED = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RecentViewedHotelRepository recentViewedHotelRepository;
    private final HotelInfoRepository hotelInfoRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public RecentViewedHotel recordHotelView(Integer customerIdx, String contentId) {
        LocalDateTime now = LocalDateTime.now();

        RecentViewedHotel entity = recentViewedHotelRepository
            .findByCustomerIdxAndContentId(customerIdx, contentId)
            .map(existing -> {
                existing.setViewedAt(now);
                return existing;
            })
            .orElseGet(() -> RecentViewedHotel.builder()
                .customerIdx(customerIdx)
                .contentId(contentId)
                .viewedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build()
            );

        entity.setUpdatedAt(now);
        RecentViewedHotel saved = recentViewedHotelRepository.save(entity);

        enforceMaximumHistory(customerIdx);
        return saved;
    }

    public Page<RecentViewedHotelResponse> getRecentViewedHotels(Integer customerIdx, Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : PageRequest.of(0, DEFAULT_PAGE_SIZE);
        Page<RecentViewedHotel> page = recentViewedHotelRepository
            .findAllByCustomerIdxOrderByViewedAtDesc(customerIdx, effectivePageable);

        List<RecentViewedHotelResponse> content = page.getContent().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return new PageImpl<>(content, effectivePageable, page.getTotalElements());
    }

    public long countRecentViewedHotels(Integer customerIdx) {
        return recentViewedHotelRepository.countByCustomerIdx(customerIdx);
    }

    @Transactional
    public void deleteRecentViewedHotel(Integer customerIdx, Integer recentViewedIdx) {
        RecentViewedHotel target = recentViewedHotelRepository
            .findByRecentViewedIdxAndCustomerIdx(recentViewedIdx, customerIdx)
            .orElseThrow(() -> new IllegalArgumentException("최근 본 호텔 기록을 찾을 수 없습니다."));

        recentViewedHotelRepository.delete(target);
    }

    @Transactional
    public void deleteAllRecentViewedHotels(Integer customerIdx) {
        recentViewedHotelRepository.deleteByCustomerIdx(customerIdx);
    }

    @Transactional
    public long deleteRecentViewedHotelsOlderThan(LocalDateTime cutoff) {
        return recentViewedHotelRepository.deleteAllByCreatedAtBefore(cutoff);
    }

    private RecentViewedHotelResponse mapToResponse(RecentViewedHotel entity) {
        RecentViewedHotelResponse.RecentViewedHotelResponseBuilder builder = RecentViewedHotelResponse.builder()
            .recentViewedIdx(entity.getRecentViewedIdx())
            .customerIdx(entity.getCustomerIdx())
            .contentId(entity.getContentId())
            .viewedAt(entity.getViewedAt());

        Optional<HotelInfo> hotelInfoOpt = Optional.ofNullable(entity.getHotelInfo());
        HotelInfo hotelInfo = hotelInfoOpt.orElseGet(() ->
            hotelInfoRepository.findById(entity.getContentId()).orElse(null)
        );

        if (hotelInfo != null) {
            builder.hotelName(hotelInfo.getTitle())
                .address(hotelInfo.getAdress())
                .imageUrl(hotelInfo.getImageUrl());

            List<BigDecimal> basePrices = roomRepository.findBasePricesByContentId(entity.getContentId());
            basePrices.stream()
                .filter(price -> price != null)
                .map(BigDecimal::intValue)
                .min(Integer::compareTo)
                .ifPresent(builder::minPrice);
        }

        return builder.build();
    }

    private void enforceMaximumHistory(Integer customerIdx) {
        long count = recentViewedHotelRepository.countByCustomerIdx(customerIdx);
        if (count <= MAX_RECENT_VIEWED) {
            return;
        }

        int excess = (int) (count - MAX_RECENT_VIEWED);
        for (int i = 0; i < excess; i++) {
            recentViewedHotelRepository.findFirstByCustomerIdxOrderByViewedAtAsc(customerIdx)
                .ifPresent(oldest -> {
                    try {
                        recentViewedHotelRepository.delete(oldest);
                    } catch (Exception e) {
                        log.warn("최근 본 호텔 기록 삭제 실패 - idx: {}", oldest.getRecentViewedIdx(), e);
                    }
                });
        }
    }
}

