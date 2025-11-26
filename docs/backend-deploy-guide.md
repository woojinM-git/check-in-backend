## 백엔드 CI/CD 가이드

### 아키텍처 개요
- 프런트엔드와 백엔드를 분리 배포하며, 백엔드는 독립적인 EC2 인스턴스에 Gradle 빌드 아티팩트를 배포한다.
- GitHub Actions로 빌드/테스트 후, 성공 시 artifact를 EC2에 전송해 systemd 서비스(또는 백그라운드 프로세스)로 재시작한다.

### 사전 준비
- EC2
  - OS: Amazon Linux 2 또는 Ubuntu 22.04 LTS
  - Java 17 런타임 설치 (`sudo yum install java-17-amazon-corretto` 등)
  - 배포 디렉터리 예: `/opt/checkin-backend`
  - 애플리케이션 실행 계정 (예: `deploy`)
  - 필요 시 systemd 단위 파일(`checkin-backend.service`) 작성
- GitHub Secrets
  - `EC2_HOST`: EC2 퍼블릭 IP 또는 DNS
  - `EC2_PORT`(선택): SSH 포트 (기본 22)
  - `EC2_USER`: SSH 사용자 (예: `ec2-user`, `ubuntu`, `deploy`)
  - `EC2_KEY`: SSH 개인키 내용(멀티라인)
  - `EC2_APP_DIR`: 배포 디렉터리 (예: `/opt/checkin-backend`)
  - `ENV_FILE` 필요 시 추가 (예: DB 암호가 포함된 `.env`)

### EC2 초기 세팅 절차
1. 배포 계정 생성(선택)  
   `sudo useradd -m deploy && sudo usermod -aG wheel deploy`
2. 배포 디렉터리 생성 및 권한 부여  
   ```
   sudo mkdir -p /opt/checkin-backend/releases
   sudo chown -R deploy:deploy /opt/checkin-backend
   ```
3. 환경 변수 파일 작성  
   ```
   sudo tee /opt/checkin-backend/.env >/dev/null <<'EOF'
   SPRING_DATASOURCE_URL=jdbc:mysql://...
   SPRING_DATASOURCE_USERNAME=...
   SPRING_DATASOURCE_PASSWORD=...
   AWS_ACCESS_KEY_ID=...
   AWS_SECRET_ACCESS_KEY=...
   # 기타 Redis, JWT, S3 설정
   EOF
   sudo chown deploy:deploy /opt/checkin-backend/.env
   sudo chmod 600 /opt/checkin-backend/.env
   ```
4. Java 17 설치 및 확인  
   `java -version` 결과를 확인해 CI/CD와 동일한 버전인지 검증
5. (선택) Nginx 또는 ALB 설정으로 80/443 포트를 백엔드 포트로 프록시

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

### 문제 발생 시 체크리스트
- GitHub 액션 로그에서 `build` 실패 여부 확인
- EC2 연결 문제 → 방화벽/보안그룹/SSH 키 재확인
- 런타임 에러 → `journalctl -u checkin-backend` 또는 `tail -f app.log`
- DB 연결 실패 → `.env`/환경변수 값 검증


