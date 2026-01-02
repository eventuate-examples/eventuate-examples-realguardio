package io.eventuate.examples.realguardio.customerservice.api.messaging;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.ValidateLocationCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationValidated;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.ValidateLocationResult;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
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
    public ValidateLocationResult handleValidateLocation(CommandMessage<ValidateLocationCommand> cm) {
        logger.info("Handling ValidateLocationCommand: " + cm);
        ValidateLocationCommand command = cm.getCommand();
        Location location = customerService.findLocationById(command.locationId());
        if (location == null) {
            logger.info("Location not found: " + command.locationId());
            return new LocationNotFound();
        }
        logger.info("Location validated: " + location.getId());
        return new LocationValidated(location.getId(), location.getName(), location.getCustomerId());
    }
}