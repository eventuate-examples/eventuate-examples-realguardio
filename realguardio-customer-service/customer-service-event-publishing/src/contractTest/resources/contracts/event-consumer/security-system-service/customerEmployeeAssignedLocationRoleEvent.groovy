package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'customerEmployeeAssignedLocationRoleEvent'
    input {
        triggeredBy('customerEmployeeAssignedLocationRole()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                userName: 'john.doe',
                locationId: 101,
                roleName: 'Manager'
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole')
            header('event-aggregate-id', '123')
        }
    }
}
