package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerNotFoundException;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaCluster;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import io.eventuate.util.test.async.Eventually;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.lifecycle.Startables;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("postgres")
public class CustomerCommandHandlerIntegrationTest {

    private static final EventuateKafkaCluster eventuateKafkaCluster = new EventuateKafkaCluster();
    
    private static final EventuateDatabaseContainer database = DatabaseContainerFactory.makeVanillaDatabaseContainer();
    
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        eventuateKafkaCluster.kafka.dependsOn(eventuateKafkaCluster.zookeeper);
        Startables.deepStart(eventuateKafkaCluster.kafka, database).join();
        
        Stream.of(database, eventuateKafkaCluster.zookeeper, eventuateKafkaCluster.kafka).forEach(container ->
            container.registerProperties(registry::add));
    }
    
    @Configuration
    @EnableAutoConfiguration
    @Import({CustomerCommandHandlerConfiguration.class,
            EventuateTramFlywayMigrationConfiguration.class,
            TramMessageProducerJdbcConfiguration.class,
            EventuateTramKafkaMessageConsumerConfiguration.class})
    static public class Config {
    }
    
    @MockitoBean
    private CustomerService customerService;
    
    @Autowired
    private CommandProducer commandProducer;
    
    @Test
    public void shouldHandleCreateLocationWithSecuritySystemCommand() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        Long customerId = System.currentTimeMillis();
        String locationName = "Office Front Door";
        Long securitySystemId = System.currentTimeMillis() + 1000;
        Long expectedLocationId = System.currentTimeMillis() + 2000;
        
        when(customerService.createLocationWithSecuritySystem(customerId, locationName, securitySystemId))
            .thenReturn(expectedLocationId);
        
        // When
        sendCommand(customerId, locationName, securitySystemId, replyTo);
        
        // Then - verify the service method gets called
        Eventually.eventually(() -> {
            verify(customerService).createLocationWithSecuritySystem(customerId, locationName, securitySystemId);
        });
    }
    
    @Test
    public void shouldHandleCustomerNotFound() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        Long customerId = System.currentTimeMillis();
        String locationName = "Office Front Door";
        Long securitySystemId = System.currentTimeMillis() + 1000;
        
        when(customerService.createLocationWithSecuritySystem(customerId, locationName, securitySystemId))
            .thenThrow(new CustomerNotFoundException("Customer not found: " + customerId));
        
        // When
        sendCommand(customerId, locationName, securitySystemId, replyTo);
        
        // Then - verify the service method gets called
        Eventually.eventually(() -> {
            verify(customerService).createLocationWithSecuritySystem(customerId, locationName, securitySystemId);
        });
    }
    
    private void sendCommand(Long customerId, String locationName, Long securitySystemId, String replyTo) {
        commandProducer.send("customer-service", 
            new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId), 
            replyTo, 
            Collections.emptyMap());
    }
}