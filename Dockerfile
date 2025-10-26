FROM eclipse-temurin:17-jdk
WORKDIR /app

# 이미 빌드된 JAR 파일 복사
# GitHub Actions에서 빌드된 JAR 파일을 사용
COPY build/libs/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]

