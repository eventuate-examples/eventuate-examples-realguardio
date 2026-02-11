package io.realguardio.orchestration.sagas;

import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationValidated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationAlreadyHasSecuritySystem;
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

    private final CustomerServiceProxy customerServiceProxy;
    private final SecuritySystemServiceProxy securitySystemServiceProxy;
    private final PendingSecuritySystemResponses pendingResponses;
    private final SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition;

    public CreateSecuritySystemSaga(CustomerServiceProxy customerServiceProxy,
                                                   SecuritySystemServiceProxy securitySystemServiceProxy,
                                                   PendingSecuritySystemResponses pendingResponses) {
        this.customerServiceProxy = customerServiceProxy;
        this.securitySystemServiceProxy = securitySystemServiceProxy;
        this.pendingResponses = pendingResponses;
        this.sagaDefinition = buildSagaDefinition();
    }

    private SagaDefinition<CreateSecuritySystemSagaData> buildSagaDefinition() {
        return step()
                .invokeParticipant(this::makeValidateLocationCommand)
                .onReply(LocationValidated.class, this::handleLocationValidated)
                .onReply(LocationNotFound.class, this::handleLocationNotFound)
            .step()
                .invokeParticipant(this::makeCreateSecuritySystemCommand)
                .onReply(SecuritySystemCreated.class, this::handleSecuritySystemCreated)
                .onReply(LocationAlreadyHasSecuritySystem.class, this::handleLocationAlreadyHasSecuritySystem)
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

    // Step 1: Validate Location
    private CommandWithDestination makeValidateLocationCommand(CreateSecuritySystemSagaData data) {
        return customerServiceProxy.validateLocation(data.getLocationId());
    }

    private void handleLocationValidated(CreateSecuritySystemSagaData data, LocationValidated reply) {
        logger.info("LocationValidated received: locationId={}, locationName={}, customerId={}",
                reply.locationId(), reply.locationName(), reply.customerId());
        data.setLocationName(reply.locationName());
        data.setCustomerId(reply.customerId());
    }

    private void handleLocationNotFound(CreateSecuritySystemSagaData data, LocationNotFound reply) {
        logger.info("LocationNotFound received for locationId: {}", data.getLocationId());
        data.setRejectionReason("Location not found");
        pendingResponses.completeExceptionally(data.getSagaId(), new LocationNotFoundException(data.getLocationId()));
    }

    // Step 2: Create Security System with Location Id
    private CommandWithDestination makeCreateSecuritySystemCommand(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.createSecuritySystem(
                data.getLocationId(),
                data.getLocationName());
    }

    private void handleSecuritySystemCreated(CreateSecuritySystemSagaData data, SecuritySystemCreated reply) {
        logger.info("SecuritySystemCreated received with id: {}", reply.securitySystemId());
        data.setSecuritySystemId(reply.securitySystemId());
        pendingResponses.completeSecuritySystemCreation(data.getSagaId(), reply.securitySystemId());
    }

    private void handleLocationAlreadyHasSecuritySystem(CreateSecuritySystemSagaData data,
                                                         LocationAlreadyHasSecuritySystem reply) {
        logger.info("LocationAlreadyHasSecuritySystem received for locationId: {}", reply.locationId());
        data.setRejectionReason("Location already has a security system");
        pendingResponses.completeExceptionally(data.getSagaId(),
                new LocationAlreadyHasSecuritySystemException(reply.locationId()));
    }
}
