package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamMemberAdded;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.annotations.EventuateDomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomerEmployeeLocationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEmployeeLocationEventConsumer.class);

    private final LocationRolesReplicaService replicaService;

    @Autowired
    public CustomerEmployeeLocationEventConsumer(LocationRolesReplicaService replicaService) {
        this.replicaService = replicaService;
    }

    @EventuateDomainEventHandler(subscriberId = "locationRolesReplicaDispatcher", channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer")
    public void handleCustomerEmployeeAssignedLocationRole(DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        CustomerEmployeeAssignedLocationRole event = envelope.getEvent();
        logger.info("Handling CustomerEmployeeAssignedLocationRole event: userName={}, locationId={}, roleName={}",
                   event.userName(), event.locationId(), event.roleName());

        replicaService.saveLocationRole(
            event.userName(),
            event.locationId(),
            event.roleName()
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "locationRolesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleTeamMemberAdded(DomainEventEnvelope<TeamMemberAdded> envelope) {
        TeamMemberAdded event = envelope.getEvent();
        logger.info("Handling TeamMemberAdded: teamId={}, employeeId={}",
                   event.teamId(), event.customerEmployeeId());

        replicaService.saveTeamMember(
            event.teamId().toString(),
            event.customerEmployeeId().toString()
        );
    }
}