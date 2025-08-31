package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerNotFoundException;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaCluster;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.eventuate.util.test.async.Eventually;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.lifecycle.Startables;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CustomerCommandHandlerIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
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
            EventuateKafkaTestCommandProducerConfiguration.class,
            CommandOutboxTestSupportConfiguration.class})
    static public class Config {
    }
    
    @MockitoBean
    private CustomerService customerService;
    
    @Autowired
    private CommandProducer commandProducer;

    @Autowired
    private CommandOutboxTestSupport commandOutboxTestSupport;

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
            commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);

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