package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecuritySystemServiceProxyTest {

    @Test
    void shouldCreateUpdateCreationFailedCommand() {
        SecuritySystemServiceProxy proxy = new SecuritySystemServiceProxy();

        CommandWithDestination cmd = proxy.updateCreationFailed(123L, "Customer not found");

        assertThat(cmd).isNotNull();
        assertThat(cmd.getDestinationChannel()).isEqualTo("security-system-service");
        assertThat(cmd.getCommand()).isInstanceOf(UpdateCreationFailedCommand.class);
        UpdateCreationFailedCommand command = (UpdateCreationFailedCommand) cmd.getCommand();
        assertThat(command.securitySystemId()).isEqualTo(123L);
        assertThat(command.rejectionReason()).isEqualTo("Customer not found");
    }
}