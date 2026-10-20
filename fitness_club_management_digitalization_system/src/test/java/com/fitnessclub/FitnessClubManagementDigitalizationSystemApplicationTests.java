package com.fitnessclub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude="
		+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration"
})
class FitnessClubManagementDigitalizationSystemApplicationTests {

	@Test
	void contextLoads() {
	}
}

