-- 테스트 용도라면 맨 위에 초기화 추가
-- 현재는 생성 api가 추가되지 않은 상태라서 pk인 id들이 포함되어야 지정가능하여 추가한 상태.
-- 이후 생성 api 추가시 pk 없는 코드로 변경하여 더미테이터 넣어줘야 함.

DELETE
FROM active_instances;
DELETE
FROM project_members;
DELETE
FROM projects;
DELETE
FROM images;
DELETE
FROM users;


-- Users
INSERT INTO users (user_id, email, password, name, created_at, updated_at)
VALUES (1, 'test1@example.com', '$2a$10$pngHXjZ3FV/fB1P6GTBTgOysuGXyXs4/E8G2dUo5JuLFmMmnZt9kq', '사용자1', NOW(),
        NOW()), --비밀번호 :test
       (2, 'test2@example.com', '$2a$10$pngHXjZ3FV/fB1P6GTBTgOysuGXyXs4/E8G2dUo5JuLFmMmnZt9kq', '사용자2', NOW(), NOW());
--비밀번호 :test

-- Images
INSERT INTO images (image_id, image_name, version, docker_base_image, build_command, run_command, template_code,
                    created_at, updated_at)
VALUES (1, 'Java', '17', 'openjdk:21', 'javac Main.java', 'java Main',
        'public class Main { public static void main(String[] args) { System.out.println("Hello!"); } }',
        NOW(), NOW());

-- Projects
INSERT INTO projects (project_id, create_user_id, project_name, storage_volume_name, image_id, description, status,
                      created_at, updated_at)
VALUES (1, 1, '프로젝트 A', 'container-a', 1, '프로젝트 설명 A', 'ACTIVE', NOW(), NOW()),
       (2, 1, '프로젝트 B', 'container-b', 1, '프로젝트 설명 B', 'INACTIVE', NOW(), NOW());

-- ProjectMembers
INSERT INTO project_members (project_id, user_id, role)
VALUES (1, 1, 'OWNER'),
       (2, 2, 'OWNER'),
       (2, 1, 'READ');

INSERT INTO active_instances (instance_id, project_id, user_id, container_id, web_socket_port, connected_at)
VALUES
--     (1, 1, 1, 'mock-container-id-001', 10000, NOW());
(1, 1, 1, 'container-a', 10000, NOW());
/*도커 명령어는 컨테이너 이름과 id 두 값을 모두 인식할 수 있어서 container_id에 컨테이너 이름을 넣어도 무사히 값을 찾아감*/


/*
-- 이후에 생성 api 도입 시 코드
-- 초기화
DELETE FROM project_members;
DELETE FROM projects;
DELETE FROM images;
DELETE FROM users;

-- Users (user_id 생략)
INSERT INTO users (email, password, name, created_at, updated_at)
VALUES
  ('test1@example.com', '$2a$10$GfUMmUq5NLom3aAqwV2M7u2g9SlpOJH09cwU5G7T0obz4pqxqumF2', '사용자1', NOW(), NOW()),
  ('test2@example.com', '$2a$10$GfUMmUq5NLom3aAqwV2M7u2g9SlpOJH09cwU5G7T0obz4pqxqumF2', '사용자2', NOW(), NOW());

-- Images (id 생략)
INSERT INTO images (
  image_name, version, docker_base_image, build_command, run_command, template_code, created_at, updated_at
)
VALUES (
  'Java', '17', 'openjdk:21', 'javac Main.java', 'java Main',
  'public class Main { public static void main(String[] args) { System.out.println("Hello!"); } }',
  NOW(), NOW()
);

-- Projects (id 생략 + 외래키는 SELECT로)
INSERT INTO projects (
  create_user_id, project_name, storage_volume_name, image_id, description, status, created_at, updated_at
)
VALUES
  (
    (SELECT user_id FROM users WHERE email = 'test1@example.com'),
    '프로젝트 A', 'volume-a',
    (SELECT id FROM images WHERE image_name = 'Java'),
    '프로젝트 설명 A', 'ACTIVE', NOW(), NOW()
  ),
  (
    (SELECT user_id FROM users WHERE email = 'test1@example.com'),

*/
