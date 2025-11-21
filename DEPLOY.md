# 백엔드 배포 가이드

## Docker 빌드 및 실행

### 1. 필요한 파일 확인
- `Dockerfile`: 백엔드 애플리케이션 빌드 및 컨테이너 실행 설정
- `docker-compose.yml`: MySQL, Redis, Backend 서비스 정의
- `v1.sql`: 데이터베이스 초기 스키마

### 2. 환경 변수 설정
GitHub Repository Settings > Secrets and variables > Actions에서 다음 환경 변수를 설정해야 합니다:

#### 기본 설정
- `PRIVATE_KEY`: EC2 접근을 위한 SSH 개인키
- `HOST`: EC2 서버 IP 주소 (백엔드 서버)

#### 데이터베이스 설정
- `DB_URL`: MySQL 연결 URL
  - 예: `jdbc:mysql://localhost:4444/checkin?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true`
- `DB_USERNAME`: MySQL 사용자명
- `DB_PASSWORD`: MySQL 비밀번호

#### 애플리케이션 환경변수
- `JWT_SECRET`: JWT 토큰 생성/검증용 시크릿 키
- `TOSS_SECRET_KEY`: 토스페이먼츠 시크릿 키
- `TOSS_CLIENT_KEY`: 토스페이먼츠 클라이언트 키
- `GMAIL_USERNAME`: Gmail 주소
- `GMAIL_APP_PASSWORD`: Gmail 앱 비밀번호
- `NEXT_PUBLIC_API_URL`: 프론트엔드 API URL
- `AWS_S3_ACCESS_KEY`: AWS S3 액세스 키
- `AWS_S3_SECRET_ACCESS_KEY`: AWS S3 시크릿 키
- `AWS_S3_BUCKET_NAME`: S3 버킷 이름
- `AWS_S3_REGION`: S3 리전

### 3. 자동 배포
`main` 브랜치에 backend/ 폴더 변경사항이 push되면 자동으로 배포됩니다.
- GitHub Actions에서 코드를 체크아웃
- .env 파일 생성
- tar 압축하여 EC2로 전송
- EC2에서 압축 해제 후 docker-compose로 배포

### 4. 수동 배포 (로컬 테스트용)
```bash
cd backend
docker-compose up --build
```

## 서비스 포트
- Backend: `8888`
- MySQL: `4444` (호스트에서 접근 시)
- Redis: `6379`

## 서비스 접속 정보

### MySQL
- Host: `localhost:4444` (외부 접속)
- Container에서: `mysql80:3306`
- Database: `checkin`
- User: `admin`
- Password: `${DB_PASSWORD}` (환경 변수에서 설정)

### Redis
- Host: `localhost:6379` (외부 접속)
- Container에서: `redis:6379`

### Backend API
- URL: `http://localhost:8888`
- Swagger UI: `http://localhost:8888/swagger-ui.html`
- API Docs: `http://localhost:8888/api-docs`

## 주의사항
1. 데이터베이스는 초기 SQL 파일(`v1.sql`)을 통해 초기화됩니다.
2. MySQL 데이터는 Docker volume(`mysql_data`)에 영구 저장됩니다.
3. **모든 민감한 정보는 GitHub Secrets로 관리**하며, 코드에 하드코딩하지 마세요.
4. 로컬 테스트용 비밀번호(1111 등)는 개발 환경에서만 사용하세요.

