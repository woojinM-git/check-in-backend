package com.sist.backend.controller.mypage;

import java.util.List;
import java.util.Map;

import com.sist.backend.entity.RoomReservation;
import com.sist.backend.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MyPageService myPageService;

    /* 
     * 마이페이지 예약 내역 조회 API
     * 엔드포인트: GET /api/mypage/reservations?status={status}
     */

    /* 예약 내역 조회 */
    @GetMapping("/reservations")
    public ResponseEntity<?> getReservations(@RequestParam(name = "status") String status) {

        // JWT 구현 안되서 임시로 고객ID를 하드코딩
        Integer customerIdx = 1;

        if (customerIdx == null) {
            //인증 정보가 없을 경우 401 에러 반환
            return ResponseEntity.status(401).body(Map.of(
                "message", "인증 정보가 유효하지 않습니다."));
        }
        // 서비스 호출: 고객 ID와 상태 문자열 전달
        List<RoomReservation> reservations = myPageService.getMyReservationsByStatus(customerIdx, status);

        // 프론트엔드 mypage/page.js에서 예상하는 JSON 형식 ({"reservations": [...]})에 맞춰 응답
        return ResponseEntity.ok(Map.of("reservations", reservations));
    }
    /* 프로필 정보 조회 */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // 1. 임시 customerIdx = 1 획득
        Integer customerIdx = 1;
        // 2. 임시 프로필 정보 객체 생성 (실제 DB 구조에 맞게 변경 필요)
        // 실제로는 myPageService.getProfile(customerIdx)를 호출해야 합니다.
        Map<String, Object> mockProfile = Map.of(
            "customerIdx", customerIdx,
            "nickname", "임시닉네임",
            "email", "test@checkin.com",
            "phone", "010-0000-0000"
        );
        // 3. 프로필 정보 반환
        return ResponseEntity.ok(mockProfile);
        
    }
}

