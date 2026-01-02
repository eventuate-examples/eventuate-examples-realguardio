package io.realguardio.orchestration.sagas;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.CustomerNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationAlreadyHasSecuritySystem;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationCreatedWithSecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateSecuritySystemSaga implements SimpleSaga<CreateSecuritySystemSagaData> {

    private static final Logger logger = LoggerFactory.getLogger(CreateSecuritySystemSaga.class);

    private final SecuritySystemServiceProxy securitySystemServiceProxy;
    private final CustomerServiceProxy customerServiceProxy;
    private final PendingSecuritySystemResponses pendingResponses;
    
    private SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition;

    public CreateSecuritySystemSaga(SecuritySystemServiceProxy securitySystemServiceProxy,
                                   CustomerServiceProxy customerServiceProxy,
                                   PendingSecuritySystemResponses pendingResponses) {
        this.securitySystemServiceProxy = securitySystemServiceProxy;
        this.customerServiceProxy = customerServiceProxy;
        this.pendingResponses = pendingResponses;
        this.sagaDefinition = buildSagaDefinition();
    }

    private SagaDefinition<CreateSecuritySystemSagaData> buildSagaDefinition() {
        return step()
                .invokeParticipant(this::makeCreateSecuritySystemCommand)
                .onReply(SecuritySystemCreated.class, this::handleSecuritySystemCreated)
                .withCompensation(this::makeUpdateCreationFailedCommand)
            .step()
                .invokeParticipant(this::makeCreateLocationCommand)
                .onReply(LocationCreatedWithSecuritySystem.class, this::handleLocationCreated)
                .onReply(CustomerNotFound.class, this::handleCustomerNotFound)
                .onReply(LocationAlreadyHasSecuritySystem.class, this::handleLocationAlreadyHasSecuritySystem)
            .step()
                .invokeParticipant(this::makeNoteLocationCreatedCommand)
            .build();
    }

    @Override
    public SagaDefinition<CreateSecuritySystemSagaData> getSagaDefinition() {
        return sagaDefinition;
    }

    @Override
    public void onStarting(String sagaId, CreateSecuritySystemSagaData data) {
        logger.info("Starting CreateSecuritySystemSaga with id: {}", sagaId);
        data.setSagaId(sagaId);
    }

    // Step 1: Create Security System
    public CommandWithDestination makeCreateSecuritySystemCommand(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.createSecuritySystem(data.getLocationName());
    }

    public void handleSecuritySystemCreated(CreateSecuritySystemSagaData data, SecuritySystemCreated reply) {

        logger.info("SecuritySystemCreated received with id: {}", reply.securitySystemId());

        data.setSecuritySystemId(reply.securitySystemId());

        pendingResponses.completeSecuritySystemCreation(data.getSagaId(), reply.securitySystemId());
    }

    public CommandWithDestination makeUpdateCreationFailedCommand(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.updateCreationFailed(
                data.getSecuritySystemId(), 
                data.getRejectionReason());
    }

    // Step 2: Create Location with Security System
    public CommandWithDestination makeCreateLocationCommand(CreateSecuritySystemSagaData data) {
        return customerServiceProxy.createLocationWithSecuritySystem(
                data.getCustomerId(),
                data.getLocationName(),
                data.getSecuritySystemId());
    }

    public void handleLocationCreated(CreateSecuritySystemSagaData data, LocationCreatedWithSecuritySystem reply) {
        data.setLocationId(reply.locationId());
    }

    public void handleCustomerNotFound(CreateSecuritySystemSagaData data, CustomerNotFound reply) {
        data.setRejectionReason("Customer not found");
    }

    public void handleLocationAlreadyHasSecuritySystem(CreateSecuritySystemSagaData data, 
                                                      LocationAlreadyHasSecuritySystem reply) {
        data.setRejectionReason("Location already has security system");
    }

    // Step 3: Note Location Created
    public CommandWithDestination makeNoteLocationCreatedCommand(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.noteLocationCreated(
                data.getSecuritySystemId(),
                data.getLocationId());
    }
}