package io.eventuate.examples.realguardio.customerservice.db;

import io.eventuate.examples.realguardio.customerservice.customermanagement.CustomerManagementConfiguration;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamLocationRoleRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRoleRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes=DBInitializerIntegrationTest.Config.class)
@Testcontainers
@Transactional
public class DBInitializerIntegrationTest {

    @Configuration
    @Import({CustomerManagementConfiguration.class})
    @EnableAutoConfiguration
    public static class Config {
        @Bean
        public DBInitializer dbInitializer() {
            return new DBInitializer();
        }
    }

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserNameSupplier userNameSupplier;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerEmployeeRepository customerEmployeeRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamLocationRoleRepository teamLocationRoleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRoleRepository memberRoleRepository;

    @Autowired
    private DBInitializer dbInitializer;

    @Test
    public void testDBInitializerCreatesAllEntities() {
        dbInitializer.createCustomer();
        assertThat(customerRepository.count()).isGreaterThan(0);
        assertThat(customerEmployeeRepository.count()).isGreaterThan(0);
        assertThat(locationRepository.count()).isGreaterThan(0);
        assertThat(teamRepository.count()).isGreaterThan(0);
        assertThat(teamLocationRoleRepository.count()).isGreaterThan(0);
        assertThat(organizationRepository.count()).isGreaterThan(0);
        assertThat(memberRepository.count()).isGreaterThan(0);
        assertThat(memberRoleRepository.count()).isGreaterThan(0);

    }

}