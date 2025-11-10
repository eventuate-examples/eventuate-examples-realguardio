package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'securitySystemAssignedToLocationEvent'
    input {
        triggeredBy('securitySystemAssignedToLocation()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                locationId: 101,
                securitySystemId: 401
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.SecuritySystemAssignedToLocation')
            header('event-aggregate-id', '123')
        }
    }
}
