package io.eventuate.examples.realguardio.customerservice.persistence;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonName;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeLocationRoleRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamLocationRoleRepository;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRoleRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRepositoryIntegrationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
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
    private CustomerEmployeeLocationRoleRepository customerEmployeeLocationRoleRepository;

    @Test
    void shouldSaveCustomer() {
        Organization organization = new Organization("Acme Corporation");
        organization = organizationRepository.save(organization);
        logger.info("Created organization: {} with ID: {}",
            organization.getName(), organization.getId());

        // Create Customer
        Customer customer = new Customer("Acme Corporation", organization.getId());
        customer = customerRepository.save(customer);
        logger.info("Created customer: {} with ID: {}",
            customer.getName(), customer.getId());

        // Create initial admin Member
        Member adminMember = new Member(
            new PersonName("System", "Administrator"),
            new EmailAddress("admin@acme.com")
        );
        adminMember = memberRepository.save(adminMember);
        logger.info("Created admin member with ID: {}", adminMember.getId());

        // Create CustomerEmployee for admin
        CustomerEmployee adminEmployee = new CustomerEmployee(customer.getId(), adminMember.getId());
        adminEmployee = customerEmployeeRepository.save(adminEmployee);
        logger.info("Created admin employee with ID: {}", adminEmployee.getId());
    }

    @Test
    void shouldFindRolesAtLocation() {
        String userName = "foo-%s@example.com".formatted(System.currentTimeMillis());
        long locationId = System.currentTimeMillis();

        assertThat(customerEmployeeLocationRoleRepository.findRoleNamesByUserNameAndLocationId(userName, locationId)).isEmpty();
    }

}