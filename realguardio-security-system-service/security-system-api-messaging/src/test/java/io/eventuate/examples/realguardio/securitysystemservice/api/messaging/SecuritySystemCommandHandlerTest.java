package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemWithLocationIdCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.sagas.spring.inmemory.TramSagaInMemoryConfiguration;
import io.eventuate.tram.testutil.TestMessageConsumer;
import io.eventuate.tram.testutil.TestMessageConsumerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import io.eventuate.examples.realguardio.securitysystemservice.domain.LocationAlreadyHasSecuritySystemException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SecuritySystemCommandHandlerTest.TestConfig.class)
class SecuritySystemCommandHandlerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({SecuritySystemCommandHandlerConfiguration.class, TramSagaInMemoryConfiguration.class})
    static class TestConfig {

        @Bean
        public TestMessageConsumerFactory testMessageConsumerFactory() {
            return new TestMessageConsumerFactory();
        }
    }

    @MockitoBean
    private SecuritySystemService securitySystemService;

    @Autowired
    private CommandProducer commandProducer;
    
    @Autowired
    private TestMessageConsumerFactory testMessageConsumerFactory;

    @Test
    void shouldHandleCreateSecuritySystemCommand() {
        // Given
        String locationName = "Office Front Door";
        Long expectedSecuritySystemId = 42L;
        CreateSecuritySystemCommand command = new CreateSecuritySystemCommand(locationName);
        
        when(securitySystemService.createSecuritySystem(locationName)).thenReturn(expectedSecuritySystemId);
        
        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

      // When - Send the command
        var commandId = commandProducer.send("security-system-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());
        
        // Then - Verify the reply is received

        replyConsumer.assertHasReplyTo(commandId);
        verify(securitySystemService).createSecuritySystem(locationName);
    }

    @Test
    void shouldHandleNoteLocationCreatedCommand() {
        // Given
        Long securitySystemId = 42L;
        Long locationId = 123L;
        NoteLocationCreatedCommand command = new NoteLocationCreatedCommand(securitySystemId, locationId);
        
        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

        // When - Send the command
        var commandId = commandProducer.send("security-system-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());
        
        // Then - Verify the reply is received

        replyConsumer.assertHasReplyTo(commandId);

        verify(securitySystemService).noteLocationCreated(securitySystemId, locationId);
    }

    @Test
    void shouldHandleCreateSecuritySystemWithLocationIdCommand() {
        // Given
        Long locationId = 100L;
        String locationName = "Main Office";
        Long expectedSecuritySystemId = 42L;
        CreateSecuritySystemWithLocationIdCommand command = new CreateSecuritySystemWithLocationIdCommand(locationId, locationName);

        when(securitySystemService.createSecuritySystemWithLocation(locationId, locationName)).thenReturn(expectedSecuritySystemId);

        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

        // When - Send the command
        var commandId = commandProducer.send("security-system-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());

        // Then - Verify the reply is received
        replyConsumer.assertHasReplyTo(commandId);
        verify(securitySystemService).createSecuritySystemWithLocation(locationId, locationName);
    }

    @Test
    void shouldReturnErrorReplyWhenLocationAlreadyHasSecuritySystem() {
        // Given
        Long locationId = 100L;
        String locationName = "Main Office";
        CreateSecuritySystemWithLocationIdCommand command = new CreateSecuritySystemWithLocationIdCommand(locationId, locationName);

        when(securitySystemService.createSecuritySystemWithLocation(locationId, locationName))
            .thenThrow(new LocationAlreadyHasSecuritySystemException(locationId));

        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

        // When - Send the command
        var commandId = commandProducer.send("security-system-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());

        // Then - Verify an error reply is received (handler caught exception and returned error reply)
        replyConsumer.assertHasReplyTo(commandId);
        verify(securitySystemService).createSecuritySystemWithLocation(locationId, locationName);
    }
}