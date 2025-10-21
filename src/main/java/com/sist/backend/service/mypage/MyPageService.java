package com.sist.backend.service.mypage;

import java.util.List;
import java.util.Arrays;
import org.springframework.stereotype.Service;

import com.sist.backend.entity.RoomReservation;
import com.sist.backend.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageService {
    
    private final RoomReservationRepository roomReservationRepository;

    private List<Integer> mapStatusToCodes(String status) {
        switch (status.toLowerCase()) {
            case "upcoming":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{1}); // List<Integer>로 변환됩니다.
    
            case "completed":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{4});
    
            case "cancelled":
                // 💡 해결: Integer[]로 인식되도록 명시적 객체 생성
                return Arrays.asList(new Integer[]{2, 3});
    
            default:
                return Arrays.asList(new Integer[]{0, 1, 2, 3, 4});
        }
    }

    /* 고객 ID와 상태 문자열을 받아 예약 목록을 조회
     * @param customerIdx 고객 ID (로그인 사용자)
     * @param status 프론트엔드 상태 문자열
     * @return RoomReservation 목록
     */
    public List<RoomReservation> getMyReservationsByStatus(Integer customerIdx, String status) {
        // 1. 상태 문자열을 코드로 매핑
        List<Integer> statusCodes = mapStatusToCodes(status);

        // 2. Repository의 findMyReservationsByCustomerAndStatus 메서드를 호출하여 데이터 조회
        return roomReservationRepository.findByReservationsByCustomerAndStatus(customerIdx, statusCodes);
    }
}
