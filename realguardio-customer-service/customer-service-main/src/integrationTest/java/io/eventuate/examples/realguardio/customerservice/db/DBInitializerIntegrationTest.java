package io.eventuate.examples.realguardio.customerservice.db;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.CustomerManagementConfiguration;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.*;
import io.eventuate.examples.realguardio.customerservice.customermanagement.eventpublishing.CustomerManagementEventPublishingConfiguration;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRoleRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes=DBInitializerIntegrationTest.Config.class)
@Testcontainers
@Transactional
public class DBInitializerIntegrationTest {

    @Configuration
    @Import({CustomerManagementConfiguration.class, CustomerManagementEventPublishingConfiguration.class})
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

    public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("customer-service-tests");

    public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
        .withNetworkAliases("kafka")
        .withReuse(true)
        ;

    public static EventuateDatabaseContainer<?> database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
        .withNetwork(eventuateKafkaCluster.network)
        .withNetworkAliases("database")
        .withReuse(true)
        ;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Startables.deepStart(database, kafka).join();

        kafka.registerProperties(registry::add);
        database.registerProperties(registry::add);

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