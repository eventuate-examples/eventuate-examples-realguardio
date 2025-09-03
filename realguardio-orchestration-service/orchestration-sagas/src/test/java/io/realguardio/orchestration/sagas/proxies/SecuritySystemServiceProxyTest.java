package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.*;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecuritySystemServiceProxyTest {

    @Test
    void shouldCreateSecuritySystemCommand() {
        SecuritySystemServiceProxy proxy = new SecuritySystemServiceProxy();
        
        CommandWithDestination cmd = proxy.createSecuritySystem("Main Office");
        
        assertThat(cmd).isNotNull();
        assertThat(cmd.getDestinationChannel()).isEqualTo("security-system-service");
        assertThat(cmd.getCommand()).isInstanceOf(CreateSecuritySystemCommand.class);
        CreateSecuritySystemCommand command = (CreateSecuritySystemCommand) cmd.getCommand();
        assertThat(command.locationName()).isEqualTo("Main Office");
    }
    
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
    
    @Test
    void shouldCreateNoteLocationCreatedCommand() {
        SecuritySystemServiceProxy proxy = new SecuritySystemServiceProxy();
        
        CommandWithDestination cmd = proxy.noteLocationCreated(456L, 789L);
        
        assertThat(cmd).isNotNull();
        assertThat(cmd.getDestinationChannel()).isEqualTo("security-system-service");
        assertThat(cmd.getCommand()).isInstanceOf(NoteLocationCreatedCommand.class);
        NoteLocationCreatedCommand command = (NoteLocationCreatedCommand) cmd.getCommand();
        assertThat(command.securitySystemId()).isEqualTo(456L);
        assertThat(command.locationId()).isEqualTo(789L);
    }
}