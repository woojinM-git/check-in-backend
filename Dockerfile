FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle 파일들 복사 (캐시 최적화)
COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle settings.gradle ./

# 소스 코드 복사
COPY src ./src

# 권한 설정
RUN chmod +x gradlew

# 빌드 실행
RUN ./gradlew clean build -x test --no-daemon

# 빌드된 JAR 파일 찾기
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌더 스테이지에서 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]

