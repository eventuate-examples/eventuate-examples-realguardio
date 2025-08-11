package io.eventuate.examples.realguardio.customerservice.persistence;

import io.eventuate.examples.realguardio.customerservice.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerAction;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerRepository;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CustomerRepository repository;

    @Test
    void shouldSaveAndFindCustomer() {
        Customer customer = new Customer("Oakland office", 
            CustomerState.ARMED, 
            Set.of(CustomerAction.DISARM));

        Customer saved = repository.save(customer);
        assertThat(saved.getId()).isNotNull();

        Optional<Customer> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLocationName()).isEqualTo("Oakland office");
        assertThat(found.get().getState()).isEqualTo(CustomerState.ARMED);
        assertThat(found.get().getActions()).containsExactly(CustomerAction.DISARM);
    }

    @Test
    void shouldFindAllCustomers() {
        Customer system1 = new Customer("Oakland office",
            CustomerState.ARMED,
            Set.of(CustomerAction.DISARM));

        Customer system2 = new Customer("Berkeley office",
            CustomerState.DISARMED,
            Set.of(CustomerAction.ARM));

        repository.save(system1);
        repository.save(system2);

        Iterable<Customer> all = repository.findAll();
        assertThat(all).hasSize(2);
    }
}