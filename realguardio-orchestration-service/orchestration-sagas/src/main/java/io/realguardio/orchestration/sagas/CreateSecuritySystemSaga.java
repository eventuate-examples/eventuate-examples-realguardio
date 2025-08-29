package io.realguardio.orchestration.sagas;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import io.realguardio.customer.api.*;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import io.realguardio.securitysystem.api.*;
import org.springframework.stereotype.Component;

@Component
public class CreateSecuritySystemSaga implements SimpleSaga<CreateSecuritySystemSagaData> {

    private final SecuritySystemServiceProxy securitySystemServiceProxy;
    private final CustomerServiceProxy customerServiceProxy;
    private final SecuritySystemSagaService securitySystemSagaService;
    
    private SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition;

    public CreateSecuritySystemSaga(SecuritySystemServiceProxy securitySystemServiceProxy,
                                   CustomerServiceProxy customerServiceProxy,
                                   SecuritySystemSagaService securitySystemSagaService) {
        this.securitySystemServiceProxy = securitySystemServiceProxy;
        this.customerServiceProxy = customerServiceProxy;
        this.securitySystemSagaService = securitySystemSagaService;
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

    // Step 1: Create Security System
    public CommandWithDestination makeCreateSecuritySystemCommand(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.createSecuritySystem(data.getLocationName());
    }

    public void handleSecuritySystemCreated(CreateSecuritySystemSagaData data, SecuritySystemCreated reply) {
        data.setSecuritySystemId(reply.securitySystemId());
        securitySystemSagaService.completeSecuritySystemCreation(data.getSagaId(), reply.securitySystemId());
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