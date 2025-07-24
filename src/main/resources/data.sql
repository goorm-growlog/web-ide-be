-- 초기 사용자 데이터 삽입
INSERT INTO users (user_id, username) VALUES (1, 'test_user');

-- 초기 개발 환경 이미지 데이터 삽입
INSERT INTO images (image_id, image_name, version, docker_base_image, build_command, run_command, template_code)
VALUES
    (1, 'java', '17', 'openjdk:17-jdk-slim', 'javac Main.java', 'java Main', 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java 17 World!");\n    }\n}');
