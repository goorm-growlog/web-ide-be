-- 테스트 용도라면 맨 위에 초기화 추가
-- 현재는 생성 api가 추가되지 않은 상태라서 pk인 id들이 포함되어야 지정가능하여 추가한 상태.
-- 이후 생성 api 추가시 pk 없는 코드로 변경하여 더미테이터 넣어줘야 함.

DELETE
FROM active_instances;
DELETE
FROM active_sessions;
DELETE
FROM project_members;
DELETE
FROM projects;
DELETE
FROM images;
DELETE
FROM users;


-- Users
INSERT INTO users (name, email, password, profile_image_url, created_at, updated_at)
VALUES ('test_user', 'test@test.com', '$2a$12$wYwlA/Kof2KH2sJrbNaLXe4qpaux.LidOEHZnLrM8boW7IyZSiHra',
        'https://kr.freepik.com/free-photo/beautiful-view-sunset-sea_3624503.htm#from_element=photos_discover&from_view=subhome',
        NOW(), NOW()),
       ('사용자1', 'test1@example.com', '$2a$10$pngHXjZ3FV/fB1P6GTBTgOysuGXyXs4/E8G2dUo5JuLFmMmnZt9kq',
        'https://example.com/default-profile.png',
        NOW(), NOW()), -- 비밀번호 :test
       ('사용자2', 'test2@example.com', '$2a$10$pngHXjZ3FV/fB1P6GTBTgOysuGXyXs4/E8G2dUo5JuLFmMmnZt9kq',
        'https://example.com/default-profile.png',
        NOW(), NOW());
-- 비밀번호 :test

-- Images
INSERT INTO images (image_id, image_name, version, docker_base_image, build_command, run_command, template_code,
                    created_at, updated_at)
VALUES (1, 'java', '17', 'openjdk:17-jdk-slim', 'javac -d bin {filePath}',
        'java -cp bin {className}', -- 단일 Java 파일용
        'public class Main { public static void main(String[] args) { System.out.println("Hello, Java!"); } }',
        NOW(), NOW()),
       (2, 'terminal', '1.0', 'ubuntu:22.04', null, '/bin/bash', null, -- 공식 이미지 사용
        NOW(), NOW());

-- Projects
INSERT INTO projects (create_user_id, image_id, project_name, description, status, created_at,
                      updated_at)
VALUES ((SELECT user_id FROM users WHERE email = 'test1@example.com'), 1, '프로젝트 A', '설명 A', 'INACTIVE',
        NOW(), NOW()),
       ((SELECT user_id FROM users WHERE email = 'test2@example.com'), 1, '프로젝트 B', '설명 B', 'INACTIVE',
        NOW(), NOW());

-- ProjectMembers
INSERT INTO project_members (project_id, user_id, role)
VALUES ((SELECT project_id FROM projects WHERE project_name = '프로젝트 A'),
        (SELECT user_id FROM users WHERE email = 'test1@example.com'), 'OWNER'),

       ((SELECT project_id FROM projects WHERE project_name = '프로젝트 B'),
        (SELECT user_id FROM users WHERE email = 'test2@example.com'), 'OWNER'),

       ((SELECT project_id FROM projects WHERE project_name = '프로젝트 A'),
        (SELECT user_id FROM users WHERE email = 'test@test.com'), 'READ');
