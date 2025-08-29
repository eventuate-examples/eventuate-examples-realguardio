package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecuritySystemCommandHandlerTest {

    @Mock
    private SecuritySystemRepository repository;

    private SecuritySystemCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new SecuritySystemCommandHandler(repository);
    }

    @Test
    void shouldHandleCreateSecuritySystemCommand() {
        // Given
        CreateSecuritySystemCommand command = new CreateSecuritySystemCommand("Main Office");
        
        SecuritySystem savedSystem = new SecuritySystem();
        savedSystem.setId(123L);
        when(repository.save(any(SecuritySystem.class))).thenReturn(savedSystem);

        // When
        SecuritySystemCreated reply = commandHandler.handle(command);

        // Then
        assertThat(reply.securitySystemId()).isEqualTo(123L);
        verify(repository).save(argThat(system -> 
            system.getLocationName().equals("Main Office") &&
            system.getState() == SecuritySystemState.CREATION_PENDING
        ));
    }
}