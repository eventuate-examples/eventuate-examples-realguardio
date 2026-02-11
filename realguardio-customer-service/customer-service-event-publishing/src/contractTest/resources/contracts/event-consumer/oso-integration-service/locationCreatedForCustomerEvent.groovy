package contracts;

org.springframework.cloud.contract.spec.Contract.make {
    label 'locationCreatedForCustomerEvent'
    input {
        triggeredBy('locationCreatedForCustomer()')
    }

    outputMessage {
        sentTo('io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
        body([
                locationId: 101
        ])
        headers {
            header('event-aggregate-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer')
            header('event-type', 'io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationCreatedForCustomer')
            header('event-aggregate-id', '123')
        }
    }
}
