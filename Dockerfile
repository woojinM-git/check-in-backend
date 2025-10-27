FROM eclipse-temurin:17-jdk
WORKDIR /app

# 이미 빌드된 JAR 파일 복사
# GitHub Actions에서 빌드된 JAR 파일을 사용
COPY build/libs/*.jar app.jar

# .env 파일 복사 (백엔드 코드에서 Dotenv.load()가 필요함)
COPY .env .env

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]

