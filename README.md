# web-ide-be

> 구름톤 딥다이브 풀스택 13회차 백엔드 GrowLog🌱 <br>
> 개발 기간:
>  - 1차 : 2025.07.11 - 2025.08.03
>  - 2차 : 2025.08.05 - 2025.09.17

> FE Github <br>
> [web-ide-fe](https://github.com/GROWLOG-youtube-mockup/web-ide-fe)

# 팀원 소개

 profile      | ![img_1.png](readmeImage/profile/img_1.png)                                                                                   | ![img.png](readmeImage/profile/img.png)                                                                             | ![img_2.png](readmeImage/profile/img_2.png)                                                                       | ![img_3.png](readmeImage/profile/img_3.png)
--------------|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------
 name         | <p align="center">강현아 </p>                                                                                                    | <p align="center">김유진</p>                                                                                           | <p align="center">이혜원</p>                                                                                         | <p align="center">최지유</p>
 Github       | <p align="center">[![Github](https://img.shields.io/badge/hyuneeekang-black?logo=github)](https://github.com/hyuneeekang)</p> | <p align="center">[![Github](https://img.shields.io/badge/yuj118-black?logo=github)](https://github.com/yuj118)</p> | <p align="center">[![Github](https://img.shields.io/badge/hyew0-black?logo=github)](https://github.com/hyew0)</p> | <p align="center">[![Github](https://img.shields.io/badge/Jiyu-black?logo=github)](https://github.com/cherish0-0)</p> |
 Project Role | <p align="center">1차 : 프로젝트&파일 시스템/터미널 실행 구현,<br/>2차 : 채팅 Redis 연동, 소셜 로그인,<br/>프로젝트 초기 세팅</p>                                                                   | <p align="center">1차 : 파일 탐색기 구현,<br/>2차 : 코드 및 터미널 실행 구현,<br> AWS 세팅</p>                                                                         | <p align="center">1차 : 사용자 인증/코드 편집기 구현,<br/>2차 : 파일 트리 및 파일 관련 구현,<br> CI/CD 세팅</p>                                                              | <p align="center">1차 : 채팅 구현,<br/>2차 : 프로젝트 관련 구현,<br> Docker 개발 환경 세팅</p>

# 프로젝트 소개

## 배포주소

https://growlog-web-ide.vercel.app/

## 기술 스택
| Category       | Stack                                                                                             |
|----------------|---------------------------------------------------------------------------------------------------|
| Language       | ![Java 21](https://img.shields.io/badge/java%2021-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)                                   |
| Framework      | ![Spring Boot](https://img.shields.io/badge/spring%20boot-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) &nbsp; |
| Package Manager      | ![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white) &nbsp;|
| Database & ORM | ![MySQL](https://img.shields.io/badge/mysql-4479A1.svg?style=for-the-badge&logo=mysql&logoColor=white) &nbsp; ![Spring Data JPA](https://img.shields.io/badge/spring%20data%20JPA-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) &nbsp; ![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white)             |
| API Documetation       | ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white) &nbsp; |
| Auth/Security | ![Spring Security](https://img.shields.io/badge/spring%20security-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) <br> ![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)|
| RTC           |![WebSocket](https://img.shields.io/badge/WebSocket-%23262626.svg?style=for-the-badge&logo=liveblocks&logoColor=white)      |
| Static Analysis           |![CheckStyle](https://img.shields.io/badge/checkstyle-%23FFFFFF.svg?style=for-the-badge&logo=liveblocks) <br> 	![SonarLint](https://img.shields.io/badge/SonarLint-CB2029?style=for-the-badge&logo=SONARLINT&logoColor=white) <br>![IntelliJ Inspection](https://img.shields.io/badge/IntelliJ%20Inspection-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)     |
| File Management           |![AWS S3](https://img.shields.io/badge/AWS%20S3-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white) <br> 	![AWS EFS](https://img.shields.io/badge/AWS%20EFS-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white)     |
| Infrastructure & CI/CD           |![AWS](https://img.shields.io/badge/AWS-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white) <br> 	![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white) <br>    ![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)     |
| Monitoring           |![Spring Actuator](https://img.shields.io/badge/spring%20Actuator-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white) <br> 	![Node Exporter](https://img.shields.io/badge/node%20Exporter-6DA55F?style=for-the-badge&logo=node.js&logoColor=white) <br>    ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=Prometheus&logoColor=white)  <br>   ![Grafana](https://img.shields.io/badge/grafana-%23F46800.svg?style=for-the-badge&logo=grafana&logoColor=white)     |


## 아키텍쳐

<img width="1701" height="852" alt="webide2 0" src="https://github.com/user-attachments/assets/8ea81588-acdb-4671-b73c-74673fd671c6" />


## 주요 기능

### 사용자 인증

- 로그인, 회원가입, 프로필 이미지 설정, 이메일 인증 등의 기능을 제공한다.
- 이메일 인증 절차를 통해 회원가입을 지원한다.
- 프로필 이미지, 비밀번호, 이름 등의 개인 정보를 사용자가 직접 수정할 수 있다.

### 메인 페이지

- 사용자가 속해 있는 프로젝트 목록을 볼 수 있다.
- 본인 소유의 프로젝트와 참여하는 프로젝트를 구분하여 보여준다.

### 컨테이너 기반의 격리된 개발 환경

- 프로젝트 생성 시 EFS에 저장되며, Active/Inactive 상태를 가진다.
- 템플릿 기반으로 초기 개발 환경이 구성되며, 언어별 사전 정의된 이미지에서 파일이 복사되어 프로젝트 구조를 자동 생성한다.
- 프로젝트 별로 코드 실행 요청을 보내는 시점에서 컨테이너를 생성해서 코드 실행 로그를 웹소켓을 통해 실시간으로 보여준다.
- 컨테이너는 실시간 코드 편집, 컴파일, 실행 등을 위한 환경을 포함한다.

### 프로젝트에 대해 개인 대화형 터미널 할당

- 터미널 실행 요청을 보내는 시점에서 컨테이너를 생성해 응답 받도록 한다.
- 터미널 사용이 일정 시간 없을 경우 자동으로 컨테이너를 삭제하도록 한다.

### 채팅

- 실시간 채팅 및 코드 위치 연결 기능을 제공한다.
- [텍스트] (ide://경로:줄번호) 방식을 통한 해당 위치 자동 포커싱이 가능하다.

### 동시 편집

- 동일한 파일에 대해 팀원들과 동시에 편집할 수 있는 실시간 협업 기능을 지원한다.

### 음성 채팅

- 프로젝트 구성원끼리의 실시간 음성 채팅을 지원한다.
- 각 멤버별로 볼륨 설정이 가능하고, 발언자에게 별도의 표시를 두어서 식별 가능하게 한다.

### 권한 기반 프로젝트 멤버 관리

- 프로젝트 구성원은 Owner, Write, Read 권한을 가질 수 있고 권한에 따라 기능 접근이 제한된다.

## 시연 영상

### 사용자 인증

https://github.com/user-attachments/assets/996307f7-cdbd-420f-bdd3-61f06316f4c7


### Github 소셜 로그인


https://github.com/user-attachments/assets/1a7b89d7-0f7f-4df0-ab59-1585ccea39ae


### 프로젝트 생성

https://github.com/user-attachments/assets/d6a8b4b3-9933-4988-9bed-159d6e52c2b2


### 코드 편집

https://github.com/user-attachments/assets/05c83ca7-54cc-4962-b72f-fc7b44fb61cc


### 프로젝트 초대

![invite_cropped](https://github.com/user-attachments/assets/f20a1c4f-d2bd-4a14-8005-35b1c98b7901)

### 채팅

![chat_cropped](https://github.com/user-attachments/assets/006fa4db-9fc4-4bb3-9cf2-d9830842342c)


### 음성채팅

https://github.com/user-attachments/assets/0e958e90-9636-4dfb-a28f-fd89f550ab70

