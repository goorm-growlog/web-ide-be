-- src/main/resources/schema.sql

-- 외래 키 제약조건을 잠시 비활성화합니다.
SET
    FOREIGN_KEY_CHECKS = 0;

-- 기존 테이블들을 모두 삭제합니다.
DROP TABLE IF EXISTS `active_instances`;
DROP TABLE IF EXISTS `active_sessions`;
DROP TABLE IF EXISTS `project_members`;
DROP TABLE IF EXISTS `projects`;
DROP TABLE IF EXISTS `images`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `chats`;
DROP TABLE IF EXISTS `file_meta`;

-- 제약조건을 다시 활성화합니다.
SET
    FOREIGN_KEY_CHECKS = 1;

-- =================================================================
-- 1. 'users' 테이블 생성
-- =================================================================
CREATE TABLE `users`
(
    `user_id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 ID (PK)',
    `name`              VARCHAR(50)  NOT NULL COMMENT '사용자 이름',
    `email`             VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
    `password`          VARCHAR(255) NOT NULL COMMENT '비밀번호',
    `profile_image_url` VARCHAR(255) COMMENT '프로필 이미지',
    `created_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    `updated_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    `deleted_at`        DATETIME(6)  NULL COMMENT '삭제 일시',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 2. 'images' 테이블 생성
-- =================================================================
CREATE TABLE `images`
(
    `image_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '이미지 ID (PK)',
    `image_name`        VARCHAR(50)  NOT NULL COMMENT '언어 이름 (예: Java, Python)',
    `version`           VARCHAR(30)  NOT NULL COMMENT '언어 버전 (예: 17, 3.11)',
    `docker_base_image` VARCHAR(100) NOT NULL COMMENT '실행 환경 Docker 이미지명',
    `build_command`     TEXT         NULL COMMENT '빌드 명령어 템플릿',
    `run_command`       TEXT         NULL COMMENT '실행 명령어 템플릿',
    `template_code`     TEXT         NULL COMMENT '초기 생성될 기본 템플릿 코드',
    `created_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    `updated_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (`image_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 3. 'projects' 테이블 생성 (컬럼 이름 수정)
-- =================================================================
CREATE TABLE `projects`
(
    `project_id`     BIGINT                     NOT NULL AUTO_INCREMENT COMMENT '프로젝트 ID (PK)',
    `create_user_id` BIGINT                     NOT NULL COMMENT '개설자 ID (FK)',
    `image_id`       BIGINT                     NOT NULL COMMENT '이미지 ID (FK)',
    `project_name`   VARCHAR(255)               NOT NULL COMMENT '프로젝트명',
    `description`    TEXT                       NULL COMMENT '프로젝트 설명',
    `status`         ENUM ('ACTIVE','INACTIVE') NOT NULL DEFAULT 'INACTIVE' COMMENT '프로젝트 상태 (ENUM)',
    `created_at`     DATETIME(6)                NOT NULL,
    `updated_at`     DATETIME(6)                NOT NULL,
    PRIMARY KEY (`project_id`),
    CONSTRAINT `fk_projects_to_users` FOREIGN KEY (`create_user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_projects_to_images` FOREIGN KEY (`image_id`) REFERENCES `images` (`image_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 4. ★★★ 'project_members' 테이블 생성 (신규 추가) ★★★
-- =================================================================
CREATE TABLE `project_members`
(
    `project_member_id` BIGINT                          NOT NULL AUTO_INCREMENT COMMENT '프로젝트 멤버 ID (PK)',
    `project_id`        BIGINT                          NOT NULL COMMENT '프로젝트 ID (FK)',
    `user_id`           BIGINT                          NOT NULL COMMENT '사용자 ID (FK)',
    `role`              ENUM ('OWNER', 'READ', 'WRITE') NOT NULL COMMENT '역할 (소유자, 멤버)',
    `created_at`        DATETIME(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    `updated_at`        DATETIME(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    PRIMARY KEY (`project_member_id`),
    -- 한 명의 유저가 하나의 프로젝트에 중복으로 추가되는 것을 방지
    UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
    CONSTRAINT `fk_project_members_to_projects` FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_project_members_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 5. 'active_sessions' 테이블 생성
-- =================================================================
CREATE TABLE `active_sessions`
(
    `session_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '세션 ID (PK)',
    `project_id`   BIGINT       NOT NULL COMMENT '프로젝트 ID (FK)',
    `user_id`      BIGINT       NOT NULL COMMENT '사용자 ID (FK)',
    `server_id`    VARCHAR(255) NOT NULL COMMENT 'EC2 인스턴스 ID',
    `connected_at` DATETIME(6)  NOT NULL,
    PRIMARY KEY (`session_id`),
    CONSTRAINT `fk_active_sessions_to_projects` FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_active_sessions_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 6. 'active_instances' 테이블 생성
-- =================================================================
CREATE TABLE `active_instances`
(
    `instance_id`      BIGINT                     NOT NULL AUTO_INCREMENT COMMENT '인스턴스 ID (PK)',
    `project_id`       BIGINT                     NOT NULL COMMENT '프로젝트 ID (FK)',
    `user_id`          BIGINT                     NOT NULL COMMENT '사용자 ID (FK)',
    `container_id`     VARCHAR(255)               NOT NULL COMMENT '실행 중인 Docker 컨테이너 ID',
    `status`           ENUM ('ACTIVE', 'PENDING') NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE: 활성, PENDING: 삭제 대기',
    `connected_at`     DATETIME(6)                NOT NULL,
    `last_activity_at` DATETIME(6)                NOT NULL COMMENT '마지막 활동 시간',
    PRIMARY KEY (`instance_id`),
    UNIQUE KEY `uk_active_instances_container_id` (`container_id`),
    CONSTRAINT `fk_active_instances_to_projects` FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_active_instances_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 7. 'chats' 테이블 생성
-- =================================================================
CREATE TABLE `chats`
(
    `chat_id`    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '채팅 ID (PK)',
    `project_id` BIGINT      NOT NULL COMMENT '프로젝트 ID (FK)',
    `user_id`    BIGINT      NOT NULL COMMENT '사용자 ID (FK)',
    `content`    TEXT        NOT NULL COMMENT '채팅 내용',
    `sent_at`    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '채팅 전송 시간',
    PRIMARY KEY (`chat_id`),
    CONSTRAINT `fk_chats_to_projects` FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chats_to_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =================================================================
-- 8. 'file_meta' 테이블 생성 (신규 추가)
-- =================================================================
CREATE TABLE `file_meta`
(
    `id`         BIGINT                  NOT NULL AUTO_INCREMENT COMMENT '파일/폴더 메타데이터 ID (PK)',
    `project_id` BIGINT                  NOT NULL COMMENT '프로젝트 ID (FK)',
    `name`       VARCHAR(255)            NOT NULL COMMENT '파일/폴더 이름',
    `path`       VARCHAR(512)            NOT NULL COMMENT '전체 경로 (예: /src/Main.java)',
    `type`       ENUM ('file', 'folder') NOT NULL COMMENT '파일 또는 폴더',
    `deleted`    BOOLEAN DEFAULT FALSE COMMENT '삭제 여부',
    `deleted_at` DATETIME(6)             NULL COMMENT '삭제 일시',

    PRIMARY KEY (`id`),

    -- 인덱싱: 프로젝트 내 경로 빠른 조회
    UNIQUE KEY `uk_project_path_deleted` (`project_id`, `path`, `deleted_at`),

    -- 외래키 연결
    CONSTRAINT `fk_file_meta_to_projects`
        FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
