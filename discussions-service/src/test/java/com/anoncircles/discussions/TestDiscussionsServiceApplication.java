package com.anoncircles.discussions;

import org.springframework.boot.SpringApplication;

public class TestDiscussionsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(DiscussionsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
