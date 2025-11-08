FROM eclipse-temurin:17-jdk
WORKDIR /app

# 타임존 설정 (한국 시간)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 이미 빌드된 JAR 파일 복사
# GitHub Actions에서 빌드된 JAR 파일을 사용
COPY build/libs/*.jar app.jar

# .env 파일 복사 (백엔드 코드에서 Dotenv.load()가 필요함)
COPY .env .env

EXPOSE 8888

# JVM 타임존 설정을 포함한 실행 명령
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]

