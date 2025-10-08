# 체크인 (CheckIn) - 호텔 예약 플랫폼 백엔드

## 📋 프로젝트 개요

체크인은 종합적인 호텔 예약 플랫폼으로, 사용자가 다양한 숙박시설을 검색하고 예약할 수 있는 서비스입니다.

## 🛠 기술 스택

### Backend Framework
- **Spring Boot 3.5.6** (Java 17)
- **Spring Security** - 인증 및 권한 관리
- **Spring Data JPA** - 데이터 접근 계층
- **MyBatis** - 복잡한 쿼리 처리

### Database
- **MySQL 8.0** - 메인 데이터베이스
- **Flyway** - 데이터베이스 마이그레이션 관리

### Authentication & Security
- **JWT (JSON Web Token)** - 토큰 기반 인증
- **OAuth2** - 소셜 로그인
- **BCrypt** - 비밀번호 암호화

### API Documentation
- **Swagger/OpenAPI 3** - API 문서화

### Additional Features
- **Quartz Scheduler** - 작업 스케줄링
- **QueryDSL** - 타입 안전한 쿼리 작성
- **Spring Mail** - 이메일 발송
- **WebSocket** - 실시간 통신
- **Spring Boot Actuator** - 모니터링 및 관리

### Development Tools
- **Lombok** - 보일러플레이트 코드 감소
- **Spring Boot DevTools** - 개발 편의성

## 🚀 시작하기

### 필수 요구사항
- Java 17 이상
- MySQL 8.0
- Gradle 7.0 이상

### 설치 및 실행

1. **프로젝트 디렉토리로 이동**
   ```bash
    git clone https://github.com/your-username/checkIn.git
   cd checkIn/backend
   ```


2. **애플리케이션 설정**
   `src/main/resources/application.yml` 파일에서 데이터베이스 연결 정보를 확인하세요.

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```

   또는 IDE에서 `BackendApplication.java`를 실행하세요.

## 📁 프로젝트 구조

```
src/main/java/com/sist/backend/
├── config/                 # 설정 클래스
│   ├── DbConfig.java      # MyBatis 설정
│   ├── SecurityJavaConfig.java  # Spring Security 설정
│   └── SwaggerConfig.java # Swagger 설정
├── controller/            # REST API 컨트롤러
├── dto/                   # 데이터 전송 객체
├── entity/                # JPA 엔티티
├── jwt/                   # JWT 관련 유틸리티
├── repository/            # 데이터 접근 계층
├── security/              # 보안 관련 클래스
├── service/               # 비즈니스 로직
└── util/                  # 유틸리티 클래스
```

## 🔧 주요 기능

### 인증 및 권한 관리
- JWT 기반 토큰 인증
- 소셜 로그인

### 호텔 관리
- 호텔 정보 조회 및 검색
- 객실 정보 관리
- 예약 시스템

### 사용자 관리
- 회원가입 및 로그인
- 프로필 관리
- 예약 내역 조회

### 결제 시스템
- 토스페이 기반 결제 방법 지원
- 쿠폰 및 할인 시스템

## 📚 API 문서

애플리케이션 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:

- **Swagger UI**: http://localhost:8888/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8888/api-docs

## 🗄 데이터베이스

### 주요 테이블
- `hotelInfo` - 호텔 기본 정보
- `room` - 객실 정보
- `customer` - 고객 정보
- `roomReservation` - 객실 예약
- `roomPayment` - 결제 정보
- `area` - 지역 정보
- `category` - 숙박시설 카테고리


## 🔒 보안 설정

현재 개발 단계에서는 모든 요청이 허용되도록 설정되어 있습니다. 프로덕션 환경에서는 JWT 기반 인증을 활성화해야 합니다.

`SecurityJavaConfig.java`에서 보안 설정을 관리할 수 있습니다.
