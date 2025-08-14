package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;


public record CustomerAndCustomerEmployee(Customer customer, CustomerEmployee initialAdministrator) {

  public CustomerAndCustomerEmployee {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        if (initialAdministrator == null) {
            throw new IllegalArgumentException("Initial administrator cannot be null");
        }
        if (!initialAdministrator.getCustomerId().equals(customer.getId())) {
            throw new IllegalArgumentException("Initial administrator must belong to the customer");
        }
    }

}