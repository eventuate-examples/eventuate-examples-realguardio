package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.ValidateLocationCommand;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaCluster;
import io.eventuate.tram.testing.producer.kafka.commands.DirectToKafkaCommandProducer;
import io.eventuate.tram.messaging.consumer.SubscriberMapping;
import io.eventuate.tram.testing.producer.kafka.commands.EnableDirectToKafkaCommandProducer;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.commands.common.CommandReplyOutcome;
import io.eventuate.tram.commands.common.ReplyMessageHeaders;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.lifecycle.Startables;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
            CommandOutboxTestSupportConfiguration.class})
    @EnableDirectToKafkaCommandProducer
    static public class Config {
        @Bean
        public SubscriberMapping subscriberMapping() {
            return subscriberId -> subscriberId + "-" + System.currentTimeMillis();
        }
    }

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private DirectToKafkaCommandProducer commandProducer;

    @Autowired
    private CommandOutboxTestSupport commandOutboxTestSupport;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void shouldHandleValidateLocationCommand() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        Long locationId = System.currentTimeMillis();
        String locationName = "Main Office";
        Long customerId = System.currentTimeMillis() + 1000;

        Location location = new Location(locationName, customerId);
        setId(location, locationId);

        when(customerService.findLocationById(locationId)).thenReturn(location);

        // When
        commandProducer.send("customer-service",
            new ValidateLocationCommand(locationId),
            replyTo,
            Collections.emptyMap());

        // Then
        Eventually.eventually(() -> {
            verify(customerService).findLocationById(locationId);
            commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
        });
    }

    @Test
    public void shouldHandleValidateLocationCommandWhenLocationNotFound() {
        // Given
        String replyTo = "my-reply-to-channel-" + System.currentTimeMillis();
        Long locationId = System.currentTimeMillis();

        when(customerService.findLocationById(locationId)).thenReturn(null);

        // When
        commandProducer.send("customer-service",
            new ValidateLocationCommand(locationId),
            replyTo,
            Collections.emptyMap());

        // Then - verify LocationNotFound reply is sent to the unique reply channel
        Eventually.eventually(() -> {
            verify(customerService).findLocationById(locationId);
            assertCommandReplyFailureMessageSent(replyTo);
        });
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

    private List<Message> findMessagesSentToChannel(String channel) {
        return jdbcTemplate.query("select headers,payload from message where destination = ?", (rs, rowNum) -> {
            String headers = rs.getString("headers");
            String payload = rs.getString("payload");
            return MessageBuilder.withPayload(payload).withExtraHeaders("", JSonMapper.fromJson(headers, Map.class)).build();
        }, channel);
    }

    private void assertCommandReplyFailureMessageSent(String channel) {
        List<Message> messages = findMessagesSentToChannel(channel);
        assertThat(messages)
                .hasSize(1)
                .allMatch(reply -> CommandReplyOutcome.FAILURE.name().equals(reply.getRequiredHeader(ReplyMessageHeaders.REPLY_OUTCOME)));
    }
}
