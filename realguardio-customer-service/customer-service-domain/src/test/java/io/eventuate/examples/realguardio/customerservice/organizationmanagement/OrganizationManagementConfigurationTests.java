package io.eventuate.examples.realguardio.customerservice.organizationmanagement;

import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class OrganizationManagementConfigurationTests {

	@MockitoBean
	private UserService userService;

	@Configuration
	@Import({OrganizationManagementConfiguration.class})
	@EnableAutoConfiguration
	public static class Config {
		// Configuration for the test context
	}

	@Test
	void contextLoads() {
	}

}
