package com.anoncircles.discussions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class DiscussionsServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
