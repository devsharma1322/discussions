package com.anoncircles.genai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sanity check that the Spring context wires up. Uses the {@code test} profile
 * so it picks up the dummy {@code GEMINI_API_KEY} from {@code application-test.yml}
 * — {@link com.anoncircles.genai.service.GeminiGenaiProvider} would otherwise
 * refuse to start.
 */
@SpringBootTest
@ActiveProfiles("test")
class GenaiServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
