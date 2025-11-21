# 체크인 (CheckIn) - 호텔 예약 플랫폼 백엔드

## 📋 프로젝트 개요

체크인은 종합적인 호텔 예약 플랫폼의 백엔드 서버로, RESTful API를 제공하여 호텔 검색, 예약, 결제, 사용자 관리 등의 기능을 지원합니다.

## 🛠 기술 스택

### Backend Framework
- **Spring Boot 3.5.6** - Java 기반 웹 프레임워크
- **Java 17** - 프로그래밍 언어
- **Gradle** - 빌드 도구

### 데이터베이스
- **MySQL 8.0** - 관계형 데이터베이스
- **Flyway** - 데이터베이스 마이그레이션 관리
- **Spring Data JPA** - JPA 기반 데이터 접근
- **MyBatis** - 복잡한 쿼리 처리
- **QueryDSL** - 타입 안전한 쿼리 작성

### 인증 및 보안
- **Spring Security** - 보안 프레임워크
- **JWT (JSON Web Token)** - 토큰 기반 인증
- **OAuth2** - 소셜 로그인 (Naver, Kakao)
- **BCrypt** - 비밀번호 암호화

### API 문서화
- **Swagger/OpenAPI 3** - API 문서 자동 생성

### 추가 기능
- **Quartz Scheduler** - 작업 스케줄링
- **Spring Mail** - 이메일 발송
- **WebSocket** - 실시간 통신
- **Spring Boot Actuator** - 모니터링 및 관리
- **Redis** - 세션 및 캐시 관리

### 개발 도구
- **Lombok** - 보일러플레이트 코드 감소
- **Spring Boot DevTools** - 개발 편의성

## 🚀 시작하기

### 필수 요구사항
- Java 17 이상
- MySQL 8.0
- Gradle 7.0 이상
- Redis (선택사항)

### 설치 및 실행

1. **환경 변수 설정**
   `.env.example` 파일을 참고하여 `.env` 파일을 생성하고 필요한 환경 변수를 설정하세요:
   
   ```bash
   cp .env.example .env
   ```
   
   필수 환경 변수:
   - `DB_URL` - 데이터베이스 연결 URL
   - `DB_USERNAME` - 데이터베이스 사용자명
   - `DB_PASSWORD` - 데이터베이스 비밀번호
   - `JWT_SECRET` - JWT 서명 키 (최소 32자)
   
   > ⚠️ **주의**: `.env` 파일은 `.gitignore`에 포함되어 Git에 커밋되지 않습니다.

2. **데이터베이스 설정**
   - MySQL 서버 실행
   - `v1.sql` 파일로 초기 데이터베이스 스키마 생성 (선택사항)
   - Flyway가 자동으로 마이그레이션을 실행합니다

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```
   
   또는 IDE에서 `BackendApplication.java`를 실행하세요.

4. **서버 확인**
   - API 서버: http://localhost:8888
   - Swagger UI: http://localhost:8888/swagger-ui.html

## 📁 프로젝트 구조

```
src/main/java/com/sist/backend/
├── config/                  # 설정 클래스
│   ├── SecurityJavaConfig.java    # Spring Security 설정
│   ├── DbConfig.java        # MyBatis 설정
│   └── SwaggerConfig.java   # Swagger 설정
│
├── controller/              # REST API 컨트롤러
│   ├── hotel/              # 호텔 관련 API
│   ├── reservation/        # 예약 관련 API
│   ├── payment/            # 결제 관련 API
│   ├── customer/           # 고객 관련 API
│   └── admin/              # 관리자 관련 API
│
├── service/                 # 비즈니스 로직
│   ├── hotel/              # 호텔 서비스
│   ├── reservation/        # 예약 서비스
│   └── payment/            # 결제 서비스
│
├── repository/              # 데이터 접근 계층
│   ├── JPA Repository      # Spring Data JPA
│   └── MyBatis Mapper      # MyBatis
│
├── entity/                  # JPA 엔티티
│   ├── hotel/              # 호텔 관련 엔티티
│   ├── customer/           # 고객 관련 엔티티
│   └── reservation/        # 예약 관련 엔티티
│
├── dto/                     # 데이터 전송 객체
│   ├── request/            # 요청 DTO
│   └── response/           # 응답 DTO
│
├── jwt/                     # JWT 관련 유틸리티
│   ├── JwtTokenProvider.java
│   └── JwtFilter.java
│
├── security/                # 보안 관련 클래스
│   └── OAuth2/
│
└── util/                    # 유틸리티 클래스
```

## 🎯 주요 기능

### 인증 및 권한 관리
- ✅ **JWT 기반 토큰 인증**: Access Token 및 Refresh Token
- ✅ **소셜 로그인**: Naver, Kakao OAuth2 연동
- ✅ **역할 기반 접근 제어**: Customer, Admin, Master 권한 관리
- ✅ **비밀번호 암호화**: BCrypt 해싱

### 호텔 관리
- ✅ **호텔 검색**: 이름, 지역, 카테고리 기반 검색
- ✅ **호텔 상세 정보**: 이미지, 편의시설, 리뷰 조회
- ✅ **객실 관리**: 객실 정보 및 예약 가능 여부 조회
- ✅ **호텔 등록**: 관리자 호텔 등록 및 마스터 승인 시스템

### 예약 시스템
- ✅ **객실 예약**: 날짜별 예약 가능 여부 확인 및 예약
- ✅ **다이닝 예약**: 레스토랑 타임슬롯 예약
- ✅ **예약 잠금**: 동시 예약 방지 (Redis 기반)
- ✅ **예약 내역 관리**: 예약 조회, 취소, 수정

### 결제 시스템
- ✅ **토스페이먼츠 연동**: 다양한 결제 수단 지원
- ✅ **결제 검증**: 안전한 결제 프로세스
- ✅ **부분 결제**: 캐시/포인트 사용
- ✅ **쿠폰 시스템**: 할인 쿠폰 적용

### 사용자 관리
- ✅ **회원가입/로그인**: 일반 회원가입 및 소셜 로그인
- ✅ **프로필 관리**: 사용자 정보 수정
- ✅ **등급 시스템**: 구매 금액 기반 등급 관리
- ✅ **포인트 시스템**: 적립 및 사용

### 관리자 기능
- ✅ **호텔 승인**: 마스터 호텔 등록 승인
- ✅ **예약 관리**: 체크인/체크아웃 처리
- ✅ **고객 관리**: 고객 정보 및 예약 내역 조회
- ✅ **통계 및 리포트**: 매출, 예약 통계

### 기타 기능
- ✅ **중고거래**: 호텔 관련 중고 상품 거래
- ✅ **고객센터**: 문의 및 답변 시스템
- ✅ **이메일 발송**: 예약 확인, QR 코드 발송
- ✅ **실시간 통신**: WebSocket 기반 채팅

## 🗄 데이터베이스

### 주요 테이블
- `hotelInfo` - 호텔 기본 정보
- `hotelDetail` - 호텔 상세 정보
- `hotelImage` - 호텔 이미지
- `hotelLocation` - 호텔 위치 정보
- `room` - 객실 정보
- `roomReservation` - 객실 예약
- `roomPayment` - 결제 정보
- `customer` - 고객 정보
- `admin` - 관리자 정보
- `area` - 지역 정보
- `category` - 숙박시설 카테고리
- `dining` - 다이닝 정보
- `diningReservation` - 다이닝 예약
- `usedTrade` - 중고거래 정보

### 마이그레이션
Flyway를 사용하여 데이터베이스 스키마를 관리합니다:
- 마이그레이션 파일: `src/main/resources/db/migration/`
- 자동 실행: 애플리케이션 시작 시 자동 실행

## 📚 API 문서

애플리케이션 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: http://localhost:8888/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8888/api-docs

### 주요 API 엔드포인트

#### 호텔 관련
- `GET /api/hotels` - 호텔 목록 조회
- `GET /api/hotels/{contentId}` - 호텔 상세 정보
- `GET /api/hotels/{contentId}/rooms` - 객실 목록 조회
- `POST /api/hotel/search` - 호텔 검색

#### 예약 관련
- `POST /api/reservation` - 객실 예약
- `GET /api/reservation/{reservationIdx}` - 예약 상세 조회
- `DELETE /api/reservation/{reservationIdx}` - 예약 취소

#### 결제 관련
- `POST /api/payment/verify` - 결제 검증
- `POST /api/payment/cancel` - 결제 취소

#### 사용자 관련
- `POST /api/customer/signup` - 회원가입
- `POST /api/customer/login` - 로그인
- `GET /api/customer/me` - 현재 사용자 정보

## 🔧 환경 변수

`.env` 파일에 다음 환경 변수를 설정하세요:

```env
# 데이터베이스
DB_URL=jdbc:mysql://localhost:3306/checkin?useSSL=false&serverTimezone=Asia/Seoul
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_jwt_secret_key_minimum_32_characters_long

# Toss Payments
TOSS_SECRET_KEY=your_toss_secret_key
TOSS_CLIENT_KEY=your_toss_client_key

# Gmail SMTP
GMAIL_USERNAME=your_email@gmail.com
GMAIL_APP_PASSWORD=your_gmail_app_password

# AWS S3
AWS_S3_ACCESS_KEY=your_aws_access_key
AWS_S3_SECRET_ACCESS_KEY=your_aws_secret_access_key
AWS_S3_BUCKET_NAME=your_s3_bucket_name
AWS_S3_REGION=ap-northeast-2

# OAuth2
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3333,http://localhost:3000
```

자세한 내용은 `.env.example` 파일을 참고하세요.

## 🔒 보안 설정

### JWT 필터
- `JwtFilter`에서 토큰 검증 및 갱신 처리
- `permitAll()` 엔드포인트는 JWT 필터를 거치지 않음

### Spring Security
- `SecurityJavaConfig`에서 보안 설정 관리
- 역할 기반 접근 제어 (RBAC)
- CORS 설정

### 프로덕션 환경
- 프로덕션 환경에서는 모든 보안 설정을 활성화해야 합니다
- 환경 변수는 안전하게 관리해야 합니다

## 🧪 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest
```
