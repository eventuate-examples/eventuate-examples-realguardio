package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.CustomerNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationCreatedWithSecuritySystem;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerNotFoundException;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class CustomerCommandHandler {

    private final CustomerService customerService;

    @Autowired
    public CustomerCommandHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CommandHandlers commandHandlers() {
        return SagaCommandHandlersBuilder
                .fromChannel("customer-service")
                .onMessage(CreateLocationWithSecuritySystemCommand.class, this::handleCreateLocationWithSecuritySystem)
                .build();
    }

    public Message handleCreateLocationWithSecuritySystem(CommandMessage<CreateLocationWithSecuritySystemCommand> cm) {
        CreateLocationWithSecuritySystemCommand command = cm.getCommand();
        try {
            Long locationId = customerService.createLocationWithSecuritySystem(
                    command.customerId(), 
                    command.locationName(), 
                    command.securitySystemId());
            return withSuccess(new LocationCreatedWithSecuritySystem(locationId));
        } catch (CustomerNotFoundException e) {
            return withFailure(new CustomerNotFound());
        }
    }
}