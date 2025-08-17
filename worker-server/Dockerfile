# 1. 빌드용 Gradle 환경 제공
# Gradle이 설치된 JDK 21 환경 기반으로 빌드용 이미지 설정
FROM gradle:8.5.0-jdk21 AS builder
RUN apt-get update && apt-get install -y docker.io

# 2. 애플리케이션 코드 복사
# 작업 디렉토리 설정(/app) 및 소스코드 복사(현재 디렉토리의 모든 파일(.)을 컨테이너의 /app 디렉토리로 복사)
WORKDIR /app
COPY .. .

# 3. 실행 가능한 JAR 파일 빌드 (테스트 생략)
# Gradle을 사용하여 애플리케이션을 빌드하고 실행 가능한 JAR 파일 생성
# --no-daemon 옵션은 Gradle 데몬을 사용하지 않도록 설정하여 빌드 속도를 높임(안정성 확보)
RUN gradle bootJar --no-daemon

# 4. 실행용 이미지 설정 (가볍고 빠름)
# 실행 이미지는 경량 Eclipse Temurin JRE 21 사용
FROM eclipse-temurin:21-jre

# 🔧 Docker CLI 추가 설치
RUN apt-get update && apt-get install -y docker.io

# 5. 애플리케이션 JAR 복사
# 빌드한 JAR 파일만 실행 이미지로 복사
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 6. 포트 설정 (Spring Boot 기본 포트) - 사용할 포트 정의
EXPOSE 8080

# 7. 실행 커맨드 - 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
