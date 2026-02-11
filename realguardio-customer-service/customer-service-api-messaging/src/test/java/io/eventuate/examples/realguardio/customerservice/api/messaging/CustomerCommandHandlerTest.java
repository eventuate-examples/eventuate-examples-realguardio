package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.ValidateLocationCommand;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CustomerCommandHandlerTest.TestConfig.class)
class CustomerCommandHandlerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({CustomerCommandHandlerConfiguration.class, TramSagaInMemoryConfiguration.class})
    static class TestConfig {
        @Bean
        public TestMessageConsumerFactory testMessageConsumerFactory() {
            return new TestMessageConsumerFactory();
        }
    }

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private CommandProducer commandProducer;

    @Autowired
    private TestMessageConsumerFactory testMessageConsumerFactory;

    @Test
    void shouldHandleValidateLocationCommand() {
        // Given
        Long locationId = 100L;
        String locationName = "Main Office";
        Long customerId = 1L;

        ValidateLocationCommand command = new ValidateLocationCommand(locationId);

        Location location = new Location(locationName, customerId);
        setId(location, locationId);

        when(customerService.findLocationById(locationId)).thenReturn(location);

        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

        // When - Send the command
        var commandId = commandProducer.send("customer-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());

        // Then - Verify the reply is received
        replyConsumer.assertHasReplyTo(commandId);
        verify(customerService).findLocationById(locationId);
    }

    @Test
    void shouldHandleValidateLocationCommandWhenLocationNotFound() {
        // Given
        Long locationId = 999L;

        ValidateLocationCommand command = new ValidateLocationCommand(locationId);

        when(customerService.findLocationById(locationId)).thenReturn(null);

        // Create a test message consumer to receive the reply
        TestMessageConsumer replyConsumer = testMessageConsumerFactory.make();

        // When - Send the command
        var commandId = commandProducer.send("customer-service", command, replyConsumer.getReplyChannel(),
            Collections.emptyMap());

        // Then - Verify the failure reply is received
        replyConsumer.assertHasReplyTo(commandId);
        verify(customerService).findLocationById(locationId);
    }

    private static <T> void setId(T entity, Long id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}