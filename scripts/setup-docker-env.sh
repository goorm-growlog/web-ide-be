#!/bin/bash
set -e

echo "📦 Docker 볼륨 및 컨테이너 초기화 중..."

VOLUME_NAME="volume-a"
CONTAINER_NAME="container-a"
WORKSPACE_PATH="/app"
SRC_PATH="$WORKSPACE_PATH/src"
FILE_NAME="Main.java"

# 볼륨 생성
docker volume inspect $VOLUME_NAME >/dev/null 2>&1 || docker volume create $VOLUME_NAME

# 기존 컨테이너 제거
docker rm -f $CONTAINER_NAME 2>/dev/null || true

# 컨테이너 실행
docker run -d \
  --name $CONTAINER_NAME \
  -v $VOLUME_NAME:$WORKSPACE_PATH \
  openjdk:21-jdk-bullseye \
  bash -c "tail -f /dev/null"

# src 디렉토리 생성
docker exec $CONTAINER_NAME mkdir -p $SRC_PATH

# 기본 Java 파일 넣기
docker exec $CONTAINER_NAME bash -c "echo 'public class Main { public static void main(String[] args) { System.out.println(\"Hello, Java World!\"); } }' > $SRC_PATH/$FILE_NAME"

echo "✅ 자바 기본 템플릿 프로젝트 초기화 완료"
