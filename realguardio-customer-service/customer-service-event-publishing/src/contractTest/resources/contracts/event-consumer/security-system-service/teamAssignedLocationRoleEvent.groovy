package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'teamAssignedLocationRoleEvent'
    input {
        triggeredBy('teamAssignedLocationRole()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                teamId: 201,
                locationId: 101,
                roleName: 'Admin'
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamAssignedLocationRole')
            header('event-aggregate-id', '123')
        }
    }
}
