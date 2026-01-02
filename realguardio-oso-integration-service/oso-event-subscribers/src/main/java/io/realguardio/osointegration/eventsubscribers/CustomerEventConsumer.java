package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeAssignedCustomerRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationCreatedForCustomer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.SecuritySystemAssignedToLocation;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.annotations.EventuateDomainEventHandler;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEventConsumer.class);

    private final RealGuardOsoFactManager osoFactManager;

    @Autowired
    public CustomerEventConsumer(RealGuardOsoFactManager osoFactManager) {
        this.osoFactManager = osoFactManager;
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer")
    public void handleCustomerEmployeeAssignedCustomerRole(DomainEventEnvelope<CustomerEmployeeAssignedCustomerRole> envelope) {
        CustomerEmployeeAssignedCustomerRole event = envelope.getEvent();
        String customerId = envelope.getAggregateId();

        logger.info("Handling CustomerEmployeeAssignedCustomerRole event: userName={}, customerId={}, roleName={}",
                   event.userName(), customerId, event.roleName());

        osoFactManager.createRoleInCustomer(
            event.userName(),
            customerId,
            event.roleName()
        );
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer")
    public void handleLocationCreatedForCustomer(DomainEventEnvelope<LocationCreatedForCustomer> envelope) {
        LocationCreatedForCustomer event = envelope.getEvent();
        String customerId = envelope.getAggregateId();

        logger.info("Handling LocationCreatedForCustomer event: locationId={}, customerId={}",
                   event.locationId(), customerId);

        osoFactManager.createLocationForCustomer(
            event.locationId().toString(),
            customerId
        );
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer")
    public void handleSecuritySystemAssignedToLocation(DomainEventEnvelope<SecuritySystemAssignedToLocation> envelope) {
        SecuritySystemAssignedToLocation event = envelope.getEvent();

        logger.info("Handling SecuritySystemAssignedToLocation event: securitySystemId={}, locationId={}",
                   event.securitySystemId(), event.locationId());

        osoFactManager.assignSecuritySystemToLocation(
            event.securitySystemId().toString(),
            event.locationId().toString()
        );
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer")
    public void handleCustomerEmployeeAssignedLocationRole(DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        CustomerEmployeeAssignedLocationRole event = envelope.getEvent();

        logger.info("Handling CustomerEmployeeAssignedLocationRole event: userName={}, locationId={}, roleName={}",
                   event.userName(), event.locationId(), event.roleName());

        osoFactManager.createRoleAtLocation(
            event.userName(),
            event.locationId().toString(),
            event.roleName()
        );
    }
}
