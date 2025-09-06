package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerEmployeeLocationEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerEmployeeLocationEventConsumer.class);
    
    private final LocationRolesReplicaService replicaService;
    
    @Autowired
    public CustomerEmployeeLocationEventConsumer(LocationRolesReplicaService replicaService) {
        this.replicaService = replicaService;
    }
    
    public DomainEventHandlers domainEventHandlers() {
        return DomainEventHandlersBuilder
            .forAggregateType("Customer")
            .onEvent(CustomerEmployeeAssignedLocationRole.class, this::handleCustomerEmployeeAssignedLocationRole)
            .build();
    }
    
    private void handleCustomerEmployeeAssignedLocationRole(DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        CustomerEmployeeAssignedLocationRole event = envelope.getEvent();
        logger.info("Handling CustomerEmployeeAssignedLocationRole event: userName={}, locationId={}, roleName={}", 
                   event.userName(), event.locationId(), event.roleName());
        
        replicaService.saveLocationRole(
            event.userName(),
            event.locationId(),
            event.roleName()
        );
    }
}