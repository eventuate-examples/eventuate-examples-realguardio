package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.tram.commands.consumer.CommandDispatcher;
import io.eventuate.tram.commands.consumer.CommandDispatcherFactory;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = SecuritySystemRepository.class)
@Import({SagaParticipantConfiguration.class,
         TramMessageProducerJdbcConfiguration.class,
         EventuateTramKafkaMessageConsumerConfiguration.class})
public class SecuritySystemCommandHandlerConfiguration {

    @Bean
    public SecuritySystemCommandHandler securitySystemCommandHandler(SecuritySystemRepository repository) {
        return new SecuritySystemCommandHandler(repository);
    }

    @Bean
    public CommandDispatcher securitySystemCommandDispatcher(SecuritySystemCommandHandler commandHandler,
                                                             SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
        return sagaCommandDispatcherFactory.make("securitySystemCommandDispatcher",
                commandHandler.commandHandlers());
    }
}