package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'customerEmployeeAssignedCustomerRoleEvent'
    input {
        triggeredBy('customerEmployeeAssignedCustomerRole()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                customerEmployeeId: 301,
                userName: 'owner@example.com',
                roleName: 'Owner'
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeAssignedCustomerRole')
            header('event-aggregate-id', '123')
        }
    }
}
