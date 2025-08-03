# web-ide-be
> 구름톤 딥다이브 풀스택 13회차 백엔드 GrowLog🌱 <br>
> 개발 기간: 2025.07.11 - 2025.08.03

> FE Github <br>
> [web-ide-fe](https://github.com/GROWLOG-youtube-mockup/web-ide-fe)

# 팀원 소개

 profile  | ![img_1.png](readmeImage/profile/img_1.png)                                                                                   | ![img.png](readmeImage/profile/img.png)                                                                             | ![img_2.png](readmeImage/profile/img_2.png)                                                                       |![img_3.png](readmeImage/profile/img_3.png)
-----|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|---
 name| <p align="center">강현아 </p>                                                                                                    | <p align="center">김유진</p>                                                                                           | <p align="center">이혜원</p>                                                                                         |<p align="center">최지유</p>
Github| <p align="center">[![Github](https://img.shields.io/badge/hyuneeekang-black?logo=github)](https://github.com/hyuneeekang)</p> | <p align="center">[![Github](https://img.shields.io/badge/yuj118-black?logo=github)](https://github.com/yuj118)</p> | <p align="center">[![Github](https://img.shields.io/badge/hyew0-black?logo=github)](https://github.com/hyew0)</p> | <p align="center">[![Github](https://img.shields.io/badge/Jiyu-black?logo=github)](https://github.com/cherish0-0)</p> |
Project Role| <p align="center">프로젝트&파일 시스템/터미널 실행 구현,<br/>프로젝트 초기 세팅</p>                                                                   | <p align="center">파일 탐색기 구현,<br> AWS 세팅</p>                                                                         | <p align="center">사용자 인증/코드 편집기 구현,<br> CI/CD 세팅</p>                                                              | <p align="center">채팅 구현,<br> Docker 개발 환경 세팅</p>

# 프로젝트 소개

## 배포주소
https://growlog-web-ide.vercel.app/

## 기술 스택
| Category       | Stack                                                                                             |
|----------------|---------------------------------------------------------------------------------------------------|
| Language       | ![Java](https://img.shields.io/badge/Java-21-007396?logo=java)                                    |
| Framework      | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot) <br> ![JPA](https://img.shields.io/badge/Spring%20Data%20JPA-ORM-59666C?logo=spring) <br> ![WebSocket](https://img.shields.io/badge/Spring%20WebSocket-RealTime-6DB33F?logo=spring) |
| Real-time Comm | ![Socket.IO](https://img.shields.io/badge/Socket.IO-RealTime-010101?logo=socket.io)               |
| Database       | ![MySQL](https://img.shields.io/badge/MySQL-개발용-4479A1?logo=mysql) <br> ![AWS RDS](https://img.shields.io/badge/AWS%20RDS-운영용-527FFF?logo=amazonaws) |
| Auth           | ![Spring Security JWT](https://img.shields.io/badge/Security-JWT-000000?logo=springsecurity)     |
| Infra & Deploy | ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-Deploy-FF9900?logo=amazonaws) <br> ![Docker](https://img.shields.io/badge/Docker-Container-2496ED?logo=docker) <br> ![Docker Hub](https://img.shields.io/badge/Docker%20Hub-Image-2496ED?logo=docker) <br> ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI/CD-2088FF?logo=githubactions) |


## 아키텍쳐
![아키텍쳐](readmeImage/architec.png)

## 주요 기능

### 사용자 인증
- 로그인, 회원가입, 프로필 이미지 설정, 이메일 인증 등의 기능을 제공한다.
- 이메일 인증 절차를 통해 회원가입을 지원한다.
- 프로필 이미지, 비밀번호, 이름 등의 개인 정보를 사용자가 직접 수정할 수 있다.

### 메인 페이지
- 사용자가 속해 있는 프로젝트 목록을 볼 수 있다.
- 본인 소유의 프로젝트와 참여하는 프로젝트를 구분하여 보여준다.

### 컨테이너 기반의 격리된 개발 환경
- 프로젝트 시작 시, 사용자 전용 Docker 컨테이너를 실행하여 개발 환경을 제공한다.
- 각 프로젝트는 Docker 볼륨을 기반으로 고유한 파일 시스템을 가지며, 이를 통해 사용자 작업 내용을 안전하게 보존한다.
- 템플릿 기반으로 초기 개발 환경이 구성되며, 언어별 사전 정의된 이미지에서 파일이 복사되어 프로젝트 구조를 자동 생성한다.
- 컨테이너는 실시간 코드 편집, 컴파일, 실행 등을 위한 환경을 포함한다.

### 채팅
- 실시간 채팅 및 코드 위치 연결 기능을 제공한다.
- [텍스트] (ide://경로:줄번호) 방식을 통한 해당 위치 자동 포커싱이 가능하다.

### 동시 편집
- 동일한 파일에 대해 팀원들과 동시에 편집할 수 있는 실시간 협업 기능을 지원한다.

### 권한 기반 프로젝트 멤버 관리
- 프로젝트 구성원은 Owner, Write, Read 권한을 가질 수 있고 권한에 따라 기능 접근이 제한된다.

## 시연 영상

### 사용자 인증

<img src="![signup](https://github.com/user-attachments/assets/2e22da9f-b194-41a4-9beb-cadef6d3d7d5)">


### 프로젝트 생성
<img src="![Image](https://github.com/user-attachments/assets/2f6e6e5c-a43d-4cc2-84d7-d9248e268eac)">

### 코드 편집
<img src="![Image](https://github.com/user-attachments/assets/a2d1a2d4-92c3-4816-8c42-155bb14f5053)">

### 프로젝트 초대
<img src="![Image](https://github.com/user-attachments/assets/8bb9111e-1ca8-4815-aa0b-6de5ffc8c36f)" >

### 채팅
<img src="![Image](https://github.com/user-attachments/assets/97ba2730-4294-4374-8aba-20aefe42c96e)">
