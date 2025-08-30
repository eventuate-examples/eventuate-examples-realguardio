package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaCluster;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.messaging.consumer.SubscriberMapping;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.eventuate.util.test.async.Eventually;
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
import org.testcontainers.lifecycle.Startables;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SecuritySystemCommandHandlerIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SecuritySystemCommandHandlerIntegrationTest {

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
    @Import({SecuritySystemCommandHandlerConfiguration.class,
            EventuateTramFlywayMigrationConfiguration.class,
            EventuateKafkaTestCommandProducerConfiguration.class,
            CommandOutboxTestSupportConfiguration.class})
    static public class Config {
        @Bean
        public SubscriberMapping subscriberMapping() {
            return subscriberId -> subscriberId + "-" + System.currentTimeMillis();
        }
    }
    
    @MockitoBean
    private SecuritySystemService securitySystemService;
    
    @Autowired
    private CommandProducer commandProducer;

    @Autowired
    private CommandOutboxTestSupport commandOutboxTestSupport;

    @Test
    public void shouldHandleCreateSecuritySystemCommand() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        String locationName = "Office Front Door";
        Long expectedSecuritySystemId = System.currentTimeMillis();
        
        when(securitySystemService.createSecuritySystem(locationName))
            .thenReturn(expectedSecuritySystemId);
        
        // When
        commandProducer.send("security-system-service", 
            new CreateSecuritySystemCommand(locationName), 
            replyTo, 
            Collections.emptyMap());
        
        // Then
        Eventually.eventually(() -> {
            verify(securitySystemService).createSecuritySystem(locationName);
            commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
        });
    }
    
    @Test
    public void shouldHandleNoteLocationCreatedCommand() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        Long securitySystemId = System.currentTimeMillis();
        Long locationId = System.currentTimeMillis() + 1000;
        
        // When
        commandProducer.send("security-system-service", 
            new NoteLocationCreatedCommand(securitySystemId, locationId), 
            replyTo, 
            Collections.emptyMap());
        
        // Then
        Eventually.eventually(() -> {
            verify(securitySystemService).noteLocationCreated(securitySystemId, locationId);
            commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
        });
    }
}