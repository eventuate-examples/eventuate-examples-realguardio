package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.commands.consumer.CommandWithDestinationBuilder;
import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantOperation;
import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantProxy;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.*;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.*;

@SagaParticipantProxy(channel = CustomerServiceProxy.CHANNEL)
public class CustomerServiceProxy {

    public static final String CHANNEL = "customer-service";
    
    public static final Class<CustomerNotFound> customerNotFoundReply = CustomerNotFound.class;
    public static final Class<LocationAlreadyHasSecuritySystem> locationAlreadyHasSecuritySystemReply = LocationAlreadyHasSecuritySystem.class;

    @SagaParticipantOperation(
        commandClass = CreateLocationWithSecuritySystemCommand.class,
        replyClasses = LocationCreatedWithSecuritySystem.class
    )
    public CommandWithDestination createLocationWithSecuritySystem(Long customerId, String locationName, Long securitySystemId) {
        return CommandWithDestinationBuilder.send(
                new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
                .to(CHANNEL)
                .build();
    }

    @SagaParticipantOperation(
        commandClass = ValidateLocationCommand.class,
        replyClasses = LocationValidated.class
    )
    public CommandWithDestination validateLocation(Long locationId) {
        return CommandWithDestinationBuilder.send(new ValidateLocationCommand(locationId))
                .to(CHANNEL)
                .build();
    }
}