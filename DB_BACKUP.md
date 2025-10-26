# MySQL 데이터 백업 가이드

## 📋 개요

Git Actions를 통해 배포할 때 **기존 데이터는 자동으로 보존**됩니다.
하지만 추가 안전장치로 백업 방법을 제공합니다.

---

## ✅ 데이터 보존 보장

### 현재 설정

```yaml
# docker-compose.yml
volumes:
  - mysql_data:/var/lib/mysql

volumes:
  mysql_data:  # 이 volume이 데이터를 영구 저장
```

### 배포 시 동작

```bash
# 1. 컨테이너만 중지/제거 (volume은 유지)
docker-compose down

# 2. 새 컨테이너 시작 (기존 volume 사용)
docker-compose up -d --build
```

**결론**: `mysql_data` volume은 삭제되지 않으므로 데이터가 보존됩니다.

---

## 🛡️ 수동 백업 (추가 안전장치)

### 1. 실행 중인 컨테이너에서 백업

```bash
# MySQL 컨테이너 접속
docker exec -it mysql80 bash

# 백업 실행 (컨테이너 내부)
mysqldump -u admin -p1111 checkin > /tmp/backup_$(date +%Y%m%d_%H%M%S).sql

# 백업 파일 확인
ls -lh /tmp/backup_*.sql
```

### 2. 호스트에서 직접 백업

```bash
# EC2 서버에서 실행
cd /home/ubuntu/checkin_back

# MySQL 백업 파일 생성
docker exec mysql80 mysqldump -u admin -p1111 checkin > backup_$(date +%Y%m%d_%H%M%S).sql

# 백업 파일 압축
gzip backup_*.sql

# 백업 확인
ls -lh backup_*.sql.gz
```

### 3. S3로 백업 업로드 (권장)

```bash
# AWS CLI 설치 (없는 경우)
sudo apt install -y awscli

# S3에 백업 업로드
docker exec mysql80 mysqldump -u admin -p1111 checkin | \
  gzip | \
  aws s3 cp - s3://your-bucket/backups/checkin_$(date +%Y%m%d_%H%M%S).sql.gz
```

---

## 🔄 복원 방법

### 1. 컨테이너 내부에서 복원

```bash
# MySQL 컨테이너 접속
docker exec -it mysql80 bash

# DB 선택 및 복원
mysql -u admin -p1111
USE checkin;
SOURCE /tmp/backup_file.sql;
```

### 2. 호스트에서 복원

```bash
# 백업 파일을 컨테이너로 복원
cat backup_file.sql | docker exec -i mysql80 mysql -u admin -p1111 checkin
```

---

## 📅 자동 백업 설정 (Cron)

### 1. 백업 스크립트 생성

```bash
# EC2 서버에서
nano /home/ubuntu/checkin_back/backup_script.sh
```

```bash
#!/bin/bash
# backup_script.sh

BACKUP_DIR="/home/ubuntu/checkin_back/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

cd /home/ubuntu/checkin_back

# MySQL 백업
docker exec mysql80 mysqldump -u admin -p1111 checkin | \
  gzip > $BACKUP_DIR/checkin_$DATE.sql.gz

# 7일 이상 된 백업 삭제
find $BACKUP_DIR -name "checkin_*.sql.gz" -mtime +7 -delete

echo "백업 완료: checkin_$DATE.sql.gz"
```

### 2. 실행 권한 부여

```bash
chmod +x /home/ubuntu/checkin_back/backup_script.sh
```

### 3. Cron 설정 (매일 새벽 2시)

```bash
# crontab 편집
crontab -e

# 다음 줄 추가
0 2 * * * /home/ubuntu/checkin_back/backup_script.sh >> /home/ubuntu/checkin_back/backup.log 2>&1
```

---

## 🚨 주의사항

### 절대 하지 말아야 할 것

```bash
# ❌ 이렇게 하면 데이터가 삭제됩니다!
docker-compose down --volumes

# ❌ 이렇게 하면 volume이 삭제됩니다!
docker volume rm mysql_data
```

### 현재 워크플로우는 안전합니다

```bash
# ✅ 현재 워크플로우
docker-compose down       # volume 유지됨
docker-compose up -d      # 기존 volume 사용
```

---

## 🔍 Volume 상태 확인

### Volume 목록 확인

```bash
docker volume ls

# 예상 출력
DRIVER    VOLUME NAME
local     checkin_back_mysql_data
```

### Volume 세부 정보

```bash
docker volume inspect checkin_back_mysql_data

# 출력:
# [
#     {
#         "CreatedAt": "2024-01-01T00:00:00Z",
#         "Driver": "local",
#         "Mountpoint": "/var/lib/docker/volumes/checkin_back_mysql_data/_data",
#         "Name": "checkin_back_mysql_data",
#         "Options": {},
#         "Scope": "local"
#     }
# ]
```

---

## 📦 Volume 수동 백업 (가장 안전)

### 1. Volume 전체 백업

```bash
# Volume이 저장된 위치
VOLUME_PATH=$(docker volume inspect checkin_back_mysql_data --format '{{ .Mountpoint }}')

# 백업 생성
sudo tar -czf mysql_backup_$(date +%Y%m%d).tar.gz -C $VOLUME_PATH .
```

### 2. 백업 복원

```bash
# 기존 volume 삭제 (주의: 데이터 손실!)
docker-compose down --volumes

# 새 volume 생성
docker-compose up -d mysql80
sleep 5

# 백업 복원
VOLUME_PATH=$(docker volume inspect checkin_back_mysql_data --format '{{ .Mountpoint }}')
sudo tar -xzf mysql_backup_20240101.tar.gz -C $VOLUME_PATH

# 컨테이너 재시작
docker-compose restart mysql80
```

---

## 💡 요약

1. **현재 설정으로는 배포 시 데이터가 보존됩니다**
2. **Volume이 유지되므로 자동으로 백업과 비슷한 효과**
3. **추가 안전장치로 수동 백업 스크립트 사용 권장**
4. **중요한 데이터는 별도 S3에 백업 권장**

---

## 📞 문제 발생 시

### 데이터가 손실된 것처럼 보일 때

```bash
# 1. Volume 확인
docker volume ls

# 2. 컨테이너 재시작
docker-compose restart mysql80

# 3. MySQL 상태 확인
docker exec mysql80 mysql -u admin -p1111 -e "SHOW DATABASES;"
```

### 백업에서 복원

```bash
# 백업이 있다면 복원
cat backup_file.sql | docker exec -i mysql80 mysql -u admin -p1111 checkin
```

