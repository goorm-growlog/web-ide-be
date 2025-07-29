package com.growlog.webide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class WebIdeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebIdeApplication.class, args);
	}

}
