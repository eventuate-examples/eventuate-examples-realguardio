package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({SagaParticipantConfiguration.class, EventuateTramFlywayMigrationConfiguration.class})
public class CustomerCommandHandlerConfiguration {

    @Bean
    public CustomerCommandHandler customerCommandHandler(CustomerService customerService) {
        return new CustomerCommandHandler(customerService);
    }
}