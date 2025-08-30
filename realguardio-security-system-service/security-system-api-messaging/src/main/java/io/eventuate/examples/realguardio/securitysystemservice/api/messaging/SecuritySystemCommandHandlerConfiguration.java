package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.examples.realguardio.securitysystemservice.persistence.JPAPersistenceConfiguration;
import io.eventuate.tram.commands.consumer.CommandDispatcher;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({SagaParticipantConfiguration.class})
public class SecuritySystemCommandHandlerConfiguration {

    @Bean
    public SecuritySystemCommandHandler securitySystemCommandHandler(SecuritySystemService securitySystemService) {
        return new SecuritySystemCommandHandler(securitySystemService);
    }

    @Bean
    public CommandDispatcher securitySystemCommandDispatcher(SecuritySystemCommandHandler commandHandler,
                                                             SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
        return sagaCommandDispatcherFactory.make("securitySystemCommandDispatcher",
                commandHandler.commandHandlers());
    }
}