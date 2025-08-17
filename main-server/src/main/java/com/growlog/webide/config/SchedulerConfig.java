package com.growlog.webide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

	@Bean
	public ThreadPoolTaskScheduler taskScheduler() {
		// TaskScheduler 구현
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

		scheduler.setPoolSize(5); // 동시에 최대 5개 예약 작업 처리
		scheduler.setThreadNamePrefix("session-scheduler-"); // 작업 스레드 이름 접체
		scheduler.initialize();

		return scheduler;
	}
}
