# 워커 서버가 '사용자 코드 실행' 요청을 받았을 때,
# 이 Dockerfile로 미리 빌드된 이미지를 사용하여 임시 컨테이너를 생성합니다.
# ===================================================================

# 1. 베이스 이미지: Java 17 개발 도구(JDK)가 포함된 가벼운 이미지
FROM openjdk:17-slim

# 2. 필수 도구 설치
# noninteractive 설정을 통해 설치 중 사용자 입력 프롬프트가 뜨는 것을 방지합니다.
# git은 버전 관리, 소스 코드 클론 등에 필요할 수 있어 기본적으로 포함합니다.
# 설치 후에는 apt 캐시를 정리하여 최종 이미지의 크기를 줄입니다.
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y git && \
    rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉터리 설정: EFS 사용자 프로젝트 폴더가 마운트될 경로
WORKDIR /workspace

# 4. 실행 방식 정의: 컨테이너 실행 시 외부에서 명령어를 전달받아 실행하고 종료
ENTRYPOINT ["/bin/bash", "-c"]
