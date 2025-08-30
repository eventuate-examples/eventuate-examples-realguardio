package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationCreatedWithSecuritySystem;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.CustomerNotFound;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerNotFoundException;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaCluster;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.commands.consumer.CommandReplyProducer;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private CustomerCommandHandler customerCommandHandler;
    
    @Test
    public void shouldHandleCreateLocationWithSecuritySystemCommand() {
        // Given
        Long customerId = 123L;
        String locationName = "Office Front Door";
        Long securitySystemId = 456L;
        Long expectedLocationId = 789L;
        
        when(customerService.createLocationWithSecuritySystem(customerId, locationName, securitySystemId))
            .thenReturn(expectedLocationId);
        
        CreateLocationWithSecuritySystemCommand command = new CreateLocationWithSecuritySystemCommand(
            customerId, locationName, securitySystemId);
        
        CommandMessage<CreateLocationWithSecuritySystemCommand> commandMessage = 
            new CommandMessage<>("messageId", command, Collections.emptyMap(), null);
        
        // When
        Message reply = customerCommandHandler.handleCreateLocationWithSecuritySystem(commandMessage);
        
        // Then
        verify(customerService).createLocationWithSecuritySystem(customerId, locationName, securitySystemId);
        assertThat(reply).isNotNull();
        assertThat(reply.getHeader("reply_outcome-type")).isPresent()
            .hasValue("SUCCESS");
    }
    
    @Test
    public void shouldHandleCustomerNotFound() {
        // Given
        Long customerId = 123L;
        String locationName = "Office Front Door";
        Long securitySystemId = 456L;
        
        when(customerService.createLocationWithSecuritySystem(customerId, locationName, securitySystemId))
            .thenThrow(new CustomerNotFoundException("Customer not found: " + customerId));
        
        CreateLocationWithSecuritySystemCommand command = new CreateLocationWithSecuritySystemCommand(
            customerId, locationName, securitySystemId);
        
        CommandMessage<CreateLocationWithSecuritySystemCommand> commandMessage = 
            new CommandMessage<>("messageId", command, Collections.emptyMap(), null);
        
        // When
        Message reply = customerCommandHandler.handleCreateLocationWithSecuritySystem(commandMessage);
        
        // Then
        verify(customerService).createLocationWithSecuritySystem(customerId, locationName, securitySystemId);
        assertThat(reply).isNotNull();
        assertThat(reply.getHeader("reply_outcome-type")).isPresent()
            .hasValue("FAILURE");
    }
}