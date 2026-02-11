package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class LocationRepositoryImpl implements LocationRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Location findRequiredById(long locationId) {
    Location location = entityManager.find(Location.class, locationId);
    if (location == null) {
      throw new EntityNotFoundException("Location not found with id: " + locationId);
    }
    return location;
  }
}