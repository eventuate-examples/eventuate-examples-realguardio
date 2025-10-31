package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.CreateLocationResult;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.CustomerNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationCreatedWithSecuritySystem;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerNotFoundException;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.commands.consumer.annotations.EventuateCommandHandler;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.slf4j.LoggerFactory.getLogger;

public class CustomerCommandHandler {

    private static Logger logger = getLogger(CustomerCommandHandler.class);

    private final CustomerService customerService;

    @Autowired
    public CustomerCommandHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @EventuateCommandHandler(subscriberId = "customerCommandDispatcher", channel = "customer-service")
    public CreateLocationResult handleCreateLocationWithSecuritySystem(CommandMessage<CreateLocationWithSecuritySystemCommand> cm) {
        logger.info("Handling CreateLocationWithSecuritySystemCommand: " + cm);
        CreateLocationWithSecuritySystemCommand command = cm.getCommand();
        try {
            Long locationId = customerService.createLocationWithSecuritySystem(
                    command.customerId(),
                    command.locationName(),
                    command.securitySystemId());
            logger.info("Created CreateLocationWithSecuritySystemCommand: " + cm);
            return new LocationCreatedWithSecuritySystem(locationId);
        } catch (CustomerNotFoundException e) {
            logger.error("Failed to CreateLocationWithSecuritySystemCommand: ", e);
            return new CustomerNotFound();
        }
    }
}