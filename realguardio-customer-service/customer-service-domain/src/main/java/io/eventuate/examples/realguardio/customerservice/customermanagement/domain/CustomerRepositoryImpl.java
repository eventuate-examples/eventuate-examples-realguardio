package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepositoryImpl implements CustomerRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Customer findRequiredById(long customerId) {
    Customer customer = entityManager.find(Customer.class, customerId);
    if (customer == null) {
      throw new EntityNotFoundException("Customer not found with id: " + customerId);
    }
    return customer;
  }
}