package io.eventuate.examples.realguardio.securitysystemservice.db;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DBInitializer {

  public static final String LOCATION_OAKLAND_OFFICE = "Oakland office";
  public static final String LOCATION_BERKELEY_OFFICE = "Berkeley office";
  public static final String LOCATION_HAYWARD_OFFICE = "Hayward office";
  private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);

  private final SecuritySystemRepository repository;

  public DBInitializer(SecuritySystemRepository repository) {
    this.repository = repository;
  }

  public void initializeForLocation(long locationId) {
    initialize(Optional.of(locationId));
  }

  public void initialize(Optional<Long> locationId) {
      logger.info("Initializing database with sample security systems");

      // Set.of(SecuritySystemAction.DISARM)
      SecuritySystem system1 = new SecuritySystem(LOCATION_OAKLAND_OFFICE, SecuritySystemState.ARMED);
      locationId.ifPresent(system1::setLocationId);
      repository.save(system1);
      logger.info("Created security system: {}", system1.getLocationName());

      // Set.of(SecuritySystemAction.ARM)

      SecuritySystem system2 = new SecuritySystem(LOCATION_BERKELEY_OFFICE, SecuritySystemState.DISARMED);
      locationId.ifPresent(system2::setLocationId);
      repository.save(system2);
      logger.info("Created security system: {}", system2.getLocationName());

      // Set.of(SecuritySystemAction.ACKNOWLEDGE, SecuritySystemAction.DISARM)
      SecuritySystem system3 = new SecuritySystem(LOCATION_HAYWARD_OFFICE, SecuritySystemState.ALARMED);
      locationId.ifPresent(system3::setLocationId);
      repository.save(system3);
      logger.info("Created security system: {}", system3.getLocationName());

      logger.info("Database initialization complete. Created {} security systems", repository.count());
    }

  public void initialize() {
    // Check if data already exists
    if (repository.count() > 0) {
      logger.info("Database already contains data, skipping initialization");
      return;
    }

    initialize(Optional.empty());

  }
}

