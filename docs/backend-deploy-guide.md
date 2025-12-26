## 백엔드 CI/CD 가이드

### 아키텍처 개요
- 프런트엔드와 백엔드를 분리 배포하며, 백엔드는 독립적인 EC2 인스턴스에 Gradle 빌드 아티팩트를 배포한다.
- GitHub Actions로 빌드/테스트 후, 성공 시 artifact를 EC2에 전송해 systemd 서비스(또는 백그라운드 프로세스)로 재시작한다.

### 사전 준비
- **AWS 인프라**
  - EC2 인스턴스 (Ubuntu 22.04 LTS)
  - RDS MySQL 데이터베이스 (또는 별도 DB 서버)
  - S3 버킷 (이미지 저장용)
  - IAM 사용자 (S3 접근 권한)
- **EC2**
  - OS: Ubuntu 22.04 LTS
  - **스토리지 용량**: 최소 30 GiB 권장 (프로덕션은 50 GiB 이상)
    - 루트 볼륨: 30-50 GiB (gp3 타입 권장)
    - 용도: OS(약 8-10 GiB) + Java 런타임(약 1 GiB) + 애플리케이션 파일(약 1-2 GiB) + 로그 파일(약 5-10 GiB) + 배포 백업(약 5-10 GiB) + 여유 공간(약 5-10 GiB)
  - Java 17 런타임 설치 (설치 명령어는 아래 "EC2 초기 세팅 절차" 참조)
  - 배포 디렉터리 예: `/opt/checkin-backend`
  - 애플리케이션 실행 계정 (예: `deploy`)
  - 필요 시 systemd 단위 파일(`checkin-backend.service`) 작성
- GitHub Secrets
  - `EC2_HOST`: EC2 퍼블릭 IP 또는 DNS
  - `EC2_PORT`(선택): SSH 포트 (기본 22)
  - `EC2_USER`: SSH 사용자 (예: `ubuntu`, `deploy`)
  - `EC2_KEY`: SSH 개인키 내용(멀티라인)
  - `EC2_APP_DIR`: 배포 디렉터리 (예: `/opt/checkin-backend`)
  - `ENV_FILE` 필요 시 추가 (예: DB 암호가 포함된 `.env`)

### EC2 초기 세팅 절차
1. **EC2 인스턴스 생성 시 스토리지 구성**
   - 루트 볼륨: **최소 30 GiB 권장** (프로덕션 환경은 50 GiB 이상)
   - 볼륨 타입: **gp3** (기본 IOPS 3000, 비용 효율적)
   - 암호화: 보안 요구사항에 따라 선택 (프로덕션 권장)
   - 용량 산정 기준:
     - OS 및 시스템 파일: 약 8-10 GiB
     - Java 17 런타임: 약 1 GiB
     - 애플리케이션 jar 및 의존성: 약 1-2 GiB
     - 로그 파일 (로그 로테이션 미적용 시): 약 5-10 GiB
     - 배포 백업 파일 (releases/): 약 5-10 GiB
     - 여유 공간 (시스템 업데이트, 임시 파일): 약 5-10 GiB
   - 참고: 이미지/파일은 S3에 저장되므로 EC2 스토리지에는 영향 없음
2. 배포 계정 생성(선택)
   ```bash
   sudo useradd -m deploy && sudo usermod -aG sudo deploy
   ```
3. 배포 디렉터리 생성 및 권한 부여  
   ```
   sudo mkdir -p /opt/checkin-backend/releases
   sudo chown -R deploy:deploy /opt/checkin-backend
   ```
4. 환경 변수 파일 작성
   - **실행 위치**: MobaXterm에서 EC2 인스턴스에 SSH 접속한 후 실행
   - **명령어 설명**:
     ```bash
     # 1. 파일 생성 및 내용 작성
     sudo tee /opt/checkin-backend/.env >/dev/null <<'EOF'
     ```
     - `sudo`: 관리자 권한으로 실행 (파일 생성에 필요)
     - `tee`: 표준 입력을 파일에 쓰는 명령어
     - `/opt/checkin-backend/.env`: 생성할 파일 경로
     - `> /dev/null`: tee의 표준 출력을 버림 (화면에 출력하지 않음)
     - `<<'EOF'`: "EOF"가 나올 때까지 입력을 받는 히어독큐먼트 (여러 줄 입력 가능)
     - `'EOF'`: 작은따옴표로 감싸서 변수 치환 방지
   
     ```bash
     # 환경 변수 내용 (실제 값으로 변경 필요)
     SPRING_DATASOURCE_URL=jdbc:mysql://your-rds-endpoint:3306/dbname
     SPRING_DATASOURCE_USERNAME=your-db-username
     SPRING_DATASOURCE_PASSWORD=your-db-password
     AWS_S3_ACCESS_KEY=your-s3-access-key-id
     AWS_S3_SECRET_ACCESS_KEY=your-s3-secret-access-key
     AWS_S3_BUCKET_NAME=check-in-backend-images
     AWS_S3_REGION=ap-northeast-2
     ```
     - 애플리케이션이 사용할 환경 변수들
     - `your-rds-endpoint`, `your-db-username` 등을 실제 값으로 변경해야 함
   
     ```bash
     EOF
     ```
     - 히어독큐먼트 종료 (입력 종료)
   
     ```bash
     # 2. 파일 소유권 변경
     sudo chown deploy:deploy /opt/checkin-backend/.env
     ```
     - `chown`: 파일/디렉터리의 소유자 변경
     - `deploy:deploy`: 소유자와 그룹을 모두 `deploy`로 설정
     - 애플리케이션이 `deploy` 계정으로 실행되므로 소유권 필요
   
     ```bash
     # 3. 파일 권한 설정 (보안)
     sudo chmod 600 /opt/checkin-backend/.env
     ```
     - `chmod`: 파일 권한 변경
     - `600`: 소유자만 읽기/쓰기 가능 (rw-------)
     - 다른 사용자는 읽을 수 없도록 보안 설정 (비밀번호 등 민감 정보 포함)
   
   - **전체 명령어** (한 번에 실행):
     ```bash
     sudo tee /opt/checkin-backend/.env >/dev/null <<'EOF'
     # 데이터베이스 설정
     SPRING_DATASOURCE_URL=jdbc:mysql://your-rds-endpoint:3306/dbname
     SPRING_DATASOURCE_USERNAME=your-db-username
     SPRING_DATASOURCE_PASSWORD=your-db-password
     
     # AWS S3 설정
     AWS_S3_ACCESS_KEY=your-s3-access-key-id
     AWS_S3_SECRET_ACCESS_KEY=your-s3-secret-access-key
     AWS_S3_BUCKET_NAME=check-in-backend-images
     AWS_S3_REGION=ap-northeast-2
     
     # 기타 설정 (Redis, JWT, TossPayments 등)
     # Redis 설정
     # JWT_SECRET=...
     # TOSS_SECRET_KEY=...
     # TOSS_CLIENT_KEY=...
     EOF
     sudo chown deploy:deploy /opt/checkin-backend/.env
     sudo chmod 600 /opt/checkin-backend/.env
     ```
   
   - **주의사항**:
     - `your-rds-endpoint`, `your-db-username` 등을 실제 값으로 변경해야 함
     - 비밀번호나 액세스 키는 안전하게 관리
     - 파일 생성 후 내용 확인: `cat /opt/checkin-backend/.env`
5. Java 17 설치 및 확인
   ```bash
   sudo apt update
   sudo apt install -y openjdk-17-jdk
   ```
   - 설치 확인:
     ```bash
     java -version
     # 출력 예시: openjdk version "17.0.x"
     ```
   - `JAVA_HOME` 환경 변수 설정 (필요 시):
     ```bash
     echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
     source ~/.bashrc
     ```
6. **외부 서비스 설정 확인**
   - **MySQL 데이터베이스**: EC2에 직접 설치할 필요 없음
     - 일반적으로 RDS(Amazon Relational Database Service) 또는 별도의 DB 서버 사용
     - EC2는 애플리케이션 서버 역할만 수행
     - `.env` 파일의 `SPRING_DATASOURCE_URL`에 DB 연결 정보만 설정하면 됨
     - 예: `jdbc:mysql://your-rds-endpoint:3306/dbname`
   - **MyBatis**: 별도 설치 불필요
     - `build.gradle`에 의존성으로 포함되어 있음 (`mybatis-spring-boot-starter`)
     - 빌드된 jar 파일에 자동으로 포함됨
     - EC2에서 추가 작업 없음
   - **Redis**: 사용 시 별도 서버 또는 ElastiCache 사용
     - EC2에 직접 설치하지 않고 별도 인스턴스/서비스 사용 권장
     - **AWS S3**: EC2에 폴더 생성 불필요 (AWS 클라우드 서비스)
     - **S3 버킷 생성 필요** (AWS 콘솔에서 미리 생성)
       1. AWS 콘솔 → S3 → 버킷 만들기
       2. 버킷 이름 설정 (예: `check-in-backend-images`)
       3. 리전 선택 (예: `ap-northeast-2` - 서울)
       4. **퍼블릭 액세스 설정** (중요):
          - **현재 코드 구조**: 퍼블릭 읽기 권한 필요 (`CannedAccessControlList.PublicRead` 사용)
          - **"모든 퍼블릭 액세스 차단" 해제 필요** (이미지 공개 필요)
          - **설정 방법**:
            1. S3 버킷 → 권한 탭 → "퍼블릭 액세스 차단 설정" 편집
            2. "모든 퍼블릭 액세스 차단" 체크박스 **해제**
            3. 경고 확인 후 "현재 설정으로 인해 이 버킷과 그 안에 포함된 객체가 퍼블릭 상태가 될 수 있음을 알고 있습니다." 체크
            4. 변경사항 저장
          - **해제 시 동작**:
            - ✅ 애플리케이션이 S3 객체에 `PublicRead` ACL 설정 가능
            - ✅ 프론트엔드에서 S3 URL 직접 사용 가능: `https://bucket-name.s3.region.amazonaws.com/path/image.jpg`
            - ✅ 브라우저에서 URL 입력 시 이미지 표시됨
            - ✅ `<img src="S3_URL">` 태그로 이미지 로드 가능
            - ✅ **개발 의도대로 정상 동작**
          - **차단 시 문제점**: 
            - ❌ `PublicRead` ACL 설정 시도 시 오류 발생
            - ❌ 브라우저에서 직접 URL 접근 불가 (403 Forbidden)
            - ❌ 프론트엔드에서 `<img src="S3_URL">` 사용 불가
            - ❌ 현재 코드 구조에서는 동작하지 않음
          - **보안 고려사항**:
            - 퍼블릭 액세스 허용 시 URL을 아는 누구나 접근 가능
            - 민감한 이미지는 퍼블릭 액세스 비권장
            - **대안** (보안 강화 시):
              - CloudFront + OAI(Origin Access Identity) 사용 (권장)
              - 서명된 URL(Signed URL) 사용 (코드 수정 필요)
              - 애플리케이션을 통한 프록시 (코드 수정 필요)
       5. **기본 암호화 설정** (선택 사항, 보안 강화):
          - **목적**: 검증이 아닌 **저장 시 암호화** (데이터 보안)
          - **암호화 유형 선택**:
            - **SSE-S3 (권장, 기본값)**: Amazon S3 관리형 키 사용
              - 무료, 자동 암호화
              - 대부분의 경우 충분
            - **SSE-KMS**: AWS KMS 키 사용
              - 추가 비용 발생
              - 더 세밀한 키 관리 및 감사 가능
            - **DSSE-KMS**: 이중 계층 암호화
              - 최고 수준의 보안 필요 시
          - **버킷 키**: SSE-KMS 사용 시 활성화 권장 (비용 절감)
          - **참고**: 
            - 암호화는 저장 시 자동 적용 (업로드 시 검증 아님)
            - 암호화된 객체도 퍼블릭 URL로 접근 가능 (권한이 있으면)
            - 이미지 무결성 검증은 별도 기능 (ETag, 체크섬 등)
       6. 버킷 정책 설정 (필요 시)
     - **S3 버킷과 애플리케이션 연결 설정** (중요):
       - **주의**: S3는 AWS 클라우드 서비스이므로 EC2에 직접 "연결"하는 개념이 아님
       - 애플리케이션이 AWS SDK를 통해 S3 버킷에 접근하도록 설정
       - **단계별 설정**:
         1. **IAM 사용자 생성 및 권한 부여** (S3 접근용)
            - AWS 콘솔 → IAM → 사용자 → 사용자 추가
            - 사용자 이름: `s3-access-user` (예시)
            - **액세스 유형 선택**:
              - ✅ **"프로그래밍 방식 액세스" 선택** (필수)
                - 액세스 키 ID와 비밀 액세스 키 발급
                - 애플리케이션이 AWS SDK로 S3 접근 시 사용
              - ❌ **"AWS Management Console에 대한 사용자 액세스 권한 제공" 선택 안 함** (권장)
                - **효과**: IAM 사용자가 AWS 콘솔에 로그인 가능
                - **필요성**: 콘솔 로그인은 불필요 (프로그래밍 방식만 사용)
                - **보안**: 불필요한 콘솔 접근 권한 제거 권장
                - **결론**: 체크하지 않고 프로그래밍 방식 액세스만 활성화
            - 권한 정책: `AmazonS3FullAccess` 또는 커스텀 정책
              - 커스텀 정책 예시 (특정 버킷만 접근):
                ```json
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "s3:PutObject",
                        "s3:GetObject",
                        "s3:DeleteObject",
                        "s3:ListBucket"
                      ],
                      "Resource": [
                        "arn:aws:s3:::check-in-backend-images",
                        "arn:aws:s3:::check-in-backend-images/*"
                      ]
                    }
                  ]
                }
                ```
            - **중요**: 액세스 키 ID와 비밀 액세스 키를 안전하게 저장 (한 번만 표시됨)
         2. **환경 변수 설정** (EC2의 `.env` 파일에 추가)
            - MobaXterm에서 EC2 접속 후 `.env` 파일 편집:
              ```bash
              sudo nano /opt/checkin-backend/.env
              # 또는
              sudo vi /opt/checkin-backend/.env
              ```
            - 다음 내용 추가 (실제 값으로 변경):
              ```bash
              # AWS S3 설정
              AWS_S3_ACCESS_KEY=AKIA... (IAM 사용자의 액세스 키 ID)
              AWS_S3_SECRET_ACCESS_KEY=... (IAM 사용자의 비밀 액세스 키)
              AWS_S3_BUCKET_NAME=check-in-backend-images
              AWS_S3_REGION=ap-northeast-2
              ```
            - 파일 저장 후 권한 확인:
              ```bash
              sudo chown deploy:deploy /opt/checkin-backend/.env
              sudo chmod 600 /opt/checkin-backend/.env
              ```
         3. **연결 확인** (애플리케이션 실행 후)
            - 애플리케이션 로그에서 S3 설정 로드 확인:
              ```bash
              sudo journalctl -u checkin-backend -f | grep S3
              ```
            - 예상 출력: `AWS S3 Configuration Loading`, `Bucket Name: check-in-backend-images`
     - **요약**: 
       - EC2에 직접 연결하는 것이 아님
       - IAM 사용자 생성 → 액세스 키 발급 → `.env` 파일에 설정 → 애플리케이션이 AWS SDK로 접근
     - **참고**: 
       - S3는 폴더 구조가 없지만, 키(경로)로 폴더처럼 사용 가능 (예: `hotelmain/hotel/image.jpg`)
       - 애플리케이션 코드에서 동적으로 폴더 경로 생성
       - EC2에 별도 폴더 생성 불필요
       - **현재 구현 방식**: 퍼블릭 URL 직접 사용
         - 업로드 시: `CannedAccessControlList.PublicRead` 설정
         - 반환: `amazonS3.getUrl(bucketName, s3Key).toString()` (퍼블릭 URL)
         - 프론트엔드: `<img src="https://bucket.s3.region.amazonaws.com/path/image.jpg">` 형태로 사용
       - **보안 고려사항**:
         - 퍼블릭 액세스 허용 시 누구나 URL을 알면 접근 가능
         - 민감한 이미지는 퍼블릭 액세스 비권장
         - CloudFront + OAI(Origin Access Identity) 사용 권장 (추후 개선)
7. (선택) Nginx 또는 ALB 설정으로 80/443 포트를 백엔드 포트로 프록시

### GitHub Actions 워크플로
- 위치: `.github/workflows/backend-ci-cd.yml`
- 트리거
  - `pull_request` → `build` job (테스트/빌드만)
  - `push` to `main` → `build` + `deploy`
- `build` job
  1. `actions/checkout@v4`
  2. `actions/setup-java@v4` (temurin, 17)
  3. `gradle/actions/setup-gradle@v4`
  4. `./gradlew clean test`
  5. `./gradlew bootJar`
  6. `actions/upload-artifact@v4` 로 jar 업로드
- `deploy` job
  1. `actions/download-artifact@v4`
  2. `appleboy/scp-action` 으로 jar 전송
  3. `appleboy/ssh-action` 으로 EC2 접속 후 배포 스크립트 실행
     - 최근 jar 백업
     - systemd 서비스가 있으면 재시작, 없으면 `nohup java -jar ...` 실행
     - 로그(`app.log`)로 표준 출력/에러 리다이렉트

### 시스템 서비스 예시 (`/etc/systemd/system/checkin-backend.service`)
```
[Unit]
Description=Check-In Backend
After=network.target

[Service]
User=deploy
WorkingDirectory=/opt/checkin-backend
ExecStart=/usr/bin/java -jar /opt/checkin-backend/check-in-backend.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
EnvironmentFile=/opt/checkin-backend/.env

[Install]
WantedBy=multi-user.target
```
- 적용: `sudo systemctl daemon-reload && sudo systemctl enable --now checkin-backend`

### 수동 배포 절차 (비상 시)
1. `./gradlew bootJar` 로 로컬 빌드
2. `scp build/libs/*.jar user@EC2:/opt/checkin-backend`
3. `ssh` 접속 → `sudo systemctl restart checkin-backend`
4. `sudo journalctl -u checkin-backend -f` 로 로그 확인

### 롤백 전략
- `releases/` 폴더에 버전별 jar 보관
- 장애 발생 시:
  1. `LATEST=$(ls -t releases | sed -n '2p')`
  2. 현재 서비스 중지
  3. 이전 jar 복사: `cp releases/$LATEST check-in-backend.jar`
  4. 서비스 재시작

### 모니터링 및 점검
- CloudWatch 에이전트로 CPU/메모리 지표 수집
- `app.log`를 CloudWatch Logs 또는 ELK로 전송
- 헬스체크 엔드포인트(`/actuator/health`)를 로드밸런서/모니터링에 등록

### 배포 준비 체크리스트 (순서대로 진행)

#### ✅ 완료된 작업
- [x] EC2 인스턴스 생성 (Ubuntu 22.04 LTS)
- [x] Java 17 설치 (MobaXterm에서 `sudo apt install -y openjdk-17-jdk`)
- [x] S3 버킷 생성 (`check-in-backend-images`)

#### 🔄 다음 단계 (순서대로 진행)

**1. EC2에서 배포 디렉터리 생성** (MobaXterm에서 실행)
```bash
# 배포 계정 생성 (선택)
sudo useradd -m deploy && sudo usermod -aG sudo deploy

# 배포 디렉터리 생성 및 권한 부여
sudo mkdir -p /opt/checkin-backend/releases
sudo chown -R deploy:deploy /opt/checkin-backend
```

**2. IAM 사용자 생성 및 S3 액세스 키 발급** (AWS 콘솔)
- AWS 콘솔 → IAM → 사용자 → 사용자 추가
- 사용자 이름: `s3-access-user` (예시)
- 프로그래밍 방식 액세스 선택
- 권한: `AmazonS3FullAccess` 또는 커스텀 정책
- **중요**: 액세스 키 ID와 비밀 액세스 키를 안전하게 저장 (한 번만 표시됨)

**3. 환경 변수 파일 작성** (MobaXterm에서 실행)
- `.env` 파일 생성 (위 "4. 환경 변수 파일 작성" 섹션 참조)
- 실제 DB 연결 정보, S3 액세스 키 등 입력
- 파일 권한 설정: `chmod 600`

**4. systemd 서비스 파일 생성** (MobaXterm에서 실행)
```bash
sudo tee /etc/systemd/system/checkin-backend.service >/dev/null <<'EOF'
[Unit]
Description=Check-In Backend
After=network.target

[Service]
User=deploy
WorkingDirectory=/opt/checkin-backend
ExecStart=/usr/bin/java -jar /opt/checkin-backend/check-in-backend.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
EnvironmentFile=/opt/checkin-backend/.env

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
```

**5. GitHub Secrets 설정** (GitHub 저장소)
- 저장소 → Settings → Secrets and variables → Actions
- 다음 Secrets 추가:
  - `EC2_HOST`: EC2 퍼블릭 IP 또는 DNS
  - `EC2_USER`: `ubuntu` 또는 `deploy`
  - `EC2_KEY`: SSH 개인키 내용 (전체 내용, `-----BEGIN RSA PRIVATE KEY-----` 포함)
  - `EC2_APP_DIR`: `/opt/checkin-backend`

**6. 첫 배포 테스트**
- `main` 브랜치에 push 또는 GitHub Actions 수동 실행
- 배포 로그 확인: `sudo journalctl -u checkin-backend -f`

### 문제 발생 시 체크리스트
- GitHub 액션 로그에서 `build` 실패 여부 확인
- EC2 연결 문제 → 방화벽/보안그룹/SSH 키 재확인
- 런타임 에러 → `journalctl -u checkin-backend` 또는 `tail -f app.log`
- DB 연결 실패 → `.env`/환경변수 값 검증
- 디스크 용량 부족 → `df -h`로 확인, 로그 로테이션 설정 또는 오래된 배포 파일 정리


