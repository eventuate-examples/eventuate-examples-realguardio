package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecuritySystemCommandHandlerConfigurationTest {

    @Mock
    private SecuritySystemRepository repository;

    @Test
    void shouldCreateCommandHandlers() {
        SecuritySystemCommandHandler commandHandler = new SecuritySystemCommandHandler(repository);
        CommandHandlers handlers = commandHandler.commandHandlers();
        
        assertThat(handlers).isNotNull();
        assertThat(handlers.getChannels()).contains("security-system-service");
    }
}