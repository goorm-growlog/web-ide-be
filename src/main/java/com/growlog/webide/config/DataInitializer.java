package com.growlog.webide.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.growlog.webide.domain.images.entity.Image;
import com.growlog.webide.domain.images.repository.ImageRepository;
import com.growlog.webide.domain.projects.entity.MemberRole;
import com.growlog.webide.domain.projects.entity.Project;
import com.growlog.webide.domain.projects.entity.ProjectMembers;
import com.growlog.webide.domain.projects.repository.ProjectMemberRepository;
import com.growlog.webide.domain.projects.repository.ProjectRepository;
import com.growlog.webide.domain.users.entity.Users;
import com.growlog.webide.domain.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final ImageRepository imageRepository;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;

	@Override
	public void run(String... args) throws Exception {
		// 1. 테스트용 사용자 생성
		Users testUser = Users.builder()
			.email("test@test.com")
			.password("password")
			.name("MyTestUser")
			.build();
		userRepository.save(testUser);

		// 2. 테스트용 Image 생성
		Image testImage = Image.builder()
			.imageName("java-17-template")
			.version("1.0")
			.dockerBaseImage("openjdk:17-jdk-slim")
			.build();

		// 3. (가장 중요한 변경점!) Image를 Project보다 먼저 저장합니다.
		imageRepository.save(testImage);

		// 4. 이제 DB에 저장된 Image 객체를 사용하여 Project를 생성합니다.
		Project testProject = Project.builder()
			.projectName("Test Project")
			.storageVolumeName("test-project-volume")
			.owner(testUser)
			.image(testImage) // 저장된 Image 객체를 전달
			.build();
		projectRepository.save(testProject); // 이제 정상적으로 저장됩니다.

		// 5. 테스트용 사용자를 프로젝트 멤버로 추가
		ProjectMembers member = ProjectMembers.builder()
			.project(testProject)
			.user(testUser)
			.role(MemberRole.OWNER)
			.build();
		projectMemberRepository.save(member);
	}
}
