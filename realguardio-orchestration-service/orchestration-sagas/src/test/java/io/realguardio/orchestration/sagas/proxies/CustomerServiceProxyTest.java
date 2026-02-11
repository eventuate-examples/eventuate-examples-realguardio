package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceProxyTest {

    @Test
    void shouldCreateValidateLocationCommand() {
        CustomerServiceProxy proxy = new CustomerServiceProxy();

        CommandWithDestination cmd = proxy.validateLocation(100L);

        assertThat(cmd).isNotNull();
        assertThat(cmd.getDestinationChannel()).isEqualTo("customer-service");
        assertThat(cmd.getCommand()).isInstanceOf(ValidateLocationCommand.class);
        ValidateLocationCommand command = (ValidateLocationCommand) cmd.getCommand();
        assertThat(command.locationId()).isEqualTo(100L);
    }
}
