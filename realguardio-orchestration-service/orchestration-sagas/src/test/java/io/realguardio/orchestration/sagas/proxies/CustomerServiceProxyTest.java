package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.realguardio.customer.api.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceProxyTest {

    @Test
    void shouldCreateLocationWithSecuritySystemCommand() {
        CustomerServiceProxy proxy = new CustomerServiceProxy();
        
        CommandWithDestination cmd = proxy.createLocationWithSecuritySystem(100L, "Warehouse", 200L);
        
        assertThat(cmd).isNotNull();
        assertThat(cmd.getDestinationChannel()).isEqualTo("customer-service");
        assertThat(cmd.getCommand()).isInstanceOf(CreateLocationWithSecuritySystemCommand.class);
        CreateLocationWithSecuritySystemCommand command = (CreateLocationWithSecuritySystemCommand) cmd.getCommand();
        assertThat(command.customerId()).isEqualTo(100L);
        assertThat(command.locationName()).isEqualTo("Warehouse");
        assertThat(command.securitySystemId()).isEqualTo(200L);
    }
}