-- 초기 사용자 데이터 삽입
INSERT INTO users (user_id, name, email, password, profile_image_url, created_at, updated_at)
VALUES (1, 'test_user', 'test@test.com', '$2a$12$wYwlA/Kof2KH2sJrbNaLXe4qpaux.LidOEHZnLrM8boW7IyZSiHra', 'https://kr.freepik.com/free-photo/beautiful-view-sunset-sea_3624503.htm#from_element=photos_discover&from_view=subhome', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP );

-- 초기 개발 환경 이미지 데이터 삽입
INSERT INTO images (image_id, image_name, version, docker_base_image, build_command, run_command, template_code)
VALUES
    (1, 'java', '17', 'openjdk:17-jdk-slim', 'javac Main.java', 'java Main', 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello, Java 17 World!");\n    }\n}');
