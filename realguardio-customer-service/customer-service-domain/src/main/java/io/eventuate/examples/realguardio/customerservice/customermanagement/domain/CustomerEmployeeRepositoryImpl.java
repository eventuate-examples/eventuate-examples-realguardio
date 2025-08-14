package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerEmployeeRepositoryImpl implements CustomerEmployeeRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public CustomerEmployee findRequiredById(long customerEmployeeId) {
    CustomerEmployee customerEmployee = entityManager.find(CustomerEmployee.class, customerEmployeeId);
    if (customerEmployee == null) {
      throw new EntityNotFoundException("Customer employee not found with id: " + customerEmployeeId);
    }
    return customerEmployee;
  }
}