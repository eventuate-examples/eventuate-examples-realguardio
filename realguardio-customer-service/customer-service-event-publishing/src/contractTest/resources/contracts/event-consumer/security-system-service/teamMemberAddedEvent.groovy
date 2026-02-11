package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'teamMemberAddedEvent'
    input {
        triggeredBy('teamMemberAdded()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                teamId: 201,
                customerEmployeeId: 301
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamMemberAdded')
            header('event-aggregate-id', '123')
        }
    }
}
