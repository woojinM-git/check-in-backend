📄 docs/hotel-view-redis.md
# 🏨 Hotel View (Redis 기반 실시간 조회수 기능)

## 📌 개요
호텔 상세 페이지 진입 시, 현재 몇 명이 이 호텔을 보고 있는지를 실시간으로 보여주는 기능이다.  
서버 재시작이나 다중 서버 환경에서도 동기화가 되도록 Redis를 사용했다.

> - Redis TTL = 5분
> - 프론트는 10초마다 조회 API를 폴링
> - DB(`hotelInfo`)에는 접근하지 않음 (캐시성 데이터 전용)

---

## ⚙️ 기술 스택
- **Spring Boot 3.5.6**
- **Redis (Docker: redis:7.2)**
- **Java 17**
- **Swagger (springdoc-openapi)**
- **React (Next.js)**
- **Axios (프론트 통신)**

---

## 🧩 관련 파일 구조

| 구분 | 경로 | 설명 |
|------|------|------|
| Controller | `src/main/java/com/example/hotel/controller/hotel/HotelViewRedisController.java` | API 요청 진입점 (`/view`, `/views`) |
| Service | `src/main/java/com/example/hotel/service/hotel/HotelViewRedisService.java` | Redis 세션 등록 / 조회 로직 |
| DTO | `src/main/java/com/example/hotel/dto/hotel/HotelViewRedisResponse.java` | 응답 포맷 (message, data) |
| Config | `src/main/java/com/example/hotel/config/RedisConfig.java` | Redis 연결 설정, 직렬화 세팅 |
| Front | `src/components/hotel/LiveViewerCount.js` | 실시간 조회수 표시 컴포넌트 |

---

## 🧠 기능 동작 흐름

1. 사용자가 호텔 상세 페이지 진입
   → **POST** `/api/hotels/{contentId}/view` 호출
   → Redis에 세션 등록 (`hotel:view:{contentId}:{sessionId}`)

2. 프론트가 10초마다
   → **GET** `/api/hotels/{contentId}/views` 호출
   → Redis에서 현재 세션 key 개수 조회 후 반환

3. TTL 5분이 지나면 Redis에서 자동 삭제
   → 세션이 만료되면 실시간 조회자 수 감소

---

## 🧱 Redis Key 구조

| Key | Value | TTL |
|------|--------|------|
| `hotel:view:{contentId}:{sessionId}` | `"1"` | 300초 (5분) |

---

## 🧾 API 명세

### POST `/api/hotels/{contentId}/view`
호텔 상세 진입 시 호출

| 항목 | 내용 |
|------|------|
| Method | POST |
| Path Variable | `contentId` (VARCHAR(50)) |
| Response | `{ "message": "success" }` |

---

### GET `/api/hotels/{contentId}/views`
현재 이 호텔을 보고 있는 사람 수 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Path Variable | `contentId` |
| Response | `{ "message": "success", "data": { "views": 3 } }` |

---

## 🧩 주요 코드 정리

### 🎯 `HotelViewRedisService.java`
```java
private static final String PREFIX = "hotel:view:";

public void addActiveViewer(String contentId, String sessionId) {
    String key = PREFIX + contentId + ":" + sessionId;
    redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.MINUTES);
}

public int getActiveViewerCount(String contentId) {
    String pattern = PREFIX + contentId + ":*";
    Set<Object> keys = redisTemplate.keys(pattern);
    return (keys != null) ? keys.size() : 0;
}

🎯 LiveViewerCount.js
useEffect(() => {
  hotelAPI.incrementHotelView(contentId);
  const interval = setInterval(() => {
    hotelAPI.getHotelViews(contentId).then(data => setViewCount(data.views));
  }, 10000);
  return () => clearInterval(interval);
}, [contentId]);

✅ 테스트 결과
요청	응답
POST /hotels/20241016/view	{ "message": "success" }
GET /hotels/20241016/views	{ "message": "success", "data": { "views": 3 } }

Redis 내부:

127.0.0.1:6379> keys hotel:view:*
1) "hotel:view:20241016:SESSION-ABC123"
2) "hotel:view:20241016:SESSION-XYZ456"

📌 비고

contentId는 DB에서 VARCHAR(50)이므로 컨트롤러에서도 String으로 처리

TTL(1분) 만료 시 Redis가 자동 정리

프론트에서는 10초마다 갱신

다중 서버 환경에서도 Redis 하나만 공유하면 실시간 동기화 가능

🔮 향후 확장 아이디어

결제/예약 시 Redis TTL을 15분으로 변경해 “룸 락(lock)” 기능 구현 가능

호텔 상세 정보 DB(hotelInfo)와 Redis 데이터를 조합해 “조회수 + 정보”를 함께 응답하는 API로 확장 가능