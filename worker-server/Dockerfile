# 1단계: Gradle을 사용하여 애플리케이션 빌드
# 재현 가능한 빌드를 위해 특정 버전의 Gradle과 JDK 21을 사용합니다.
FROM gradle:8.5.0-jdk21 AS builder

# 컨테이너 내부의 작업 디렉토리를 설정합니다.
WORKDIR /app

# Docker 레이어 캐시를 활용하기 위해 필요한 Gradle 설정 파일만 먼저 복사합니다.
COPY worker-server/build.gradle worker-server/settings.gradle ./
COPY gradle ./gradle

# worker-server 모듈의 소스 코드를 복사합니다.
# 다른 모듈의 변경 사항이 이 레이어의 캐시를 무효화하지 않도록 합니다.
COPY worker-server ./worker-server

# worker-server 모듈만 빌드하여 실행 가능한 JAR 파일을 생성합니다.
# --no-daemon 옵션은 CI/CD 환경에 권장됩니다.
RUN ./gradlew :worker-server:bootJar --no-daemon

# 2단계: 최종 경량 런타임 이미지 생성
# 더 작은 용량과 향상된 보안을 위해 최소한의 JRE(Java Runtime Environment) 이미지를 사용합니다.
FROM amazoncorretto:21-alpine3.22

# 빌드 단계에서 생성된 JAR 파일을 최종 이미지로 복사합니다.
COPY --from=builder /app/worker-server/build/libs/*.jar app.jar

# 컨테이너가 시작될 때 애플리케이션을 실행하는 명령어입니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
