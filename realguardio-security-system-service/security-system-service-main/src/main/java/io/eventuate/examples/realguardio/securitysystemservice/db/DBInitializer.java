package io.eventuate.examples.realguardio.securitysystemservice.db;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class DBInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);
    
    public static final String LOCATION_OAKLAND_OFFICE = "Oakland office";
    public static final String LOCATION_BERKELEY_OFFICE = "Berkeley office";
    public static final String LOCATION_HAYWARD_OFFICE = "Hayward office";
    
    @Bean
    CommandLineRunner initDatabase(SecuritySystemRepository repository) {
        return args -> {
            // Check if data already exists
            if (repository.count() > 0) {
                logger.info("Database already contains data, skipping initialization");
                return;
            }
            
            logger.info("Initializing database with sample security systems");
            
            SecuritySystem system1 = new SecuritySystem(LOCATION_OAKLAND_OFFICE,
                    SecuritySystemState.ARMED, 
                    Set.of(SecuritySystemAction.DISARM));
            repository.save(system1);
            logger.info("Created security system: {}", system1.getLocationName());
            
            SecuritySystem system2 = new SecuritySystem(LOCATION_BERKELEY_OFFICE,
                    SecuritySystemState.DISARMED, 
                    Set.of(SecuritySystemAction.ARM));
            repository.save(system2);
            logger.info("Created security system: {}", system2.getLocationName());
            
            SecuritySystem system3 = new SecuritySystem(LOCATION_HAYWARD_OFFICE,
                    SecuritySystemState.ALARMED, 
                    Set.of(SecuritySystemAction.ACKNOWLEDGE, SecuritySystemAction.DISARM));
            repository.save(system3);
            logger.info("Created security system: {}", system3.getLocationName());
            
            logger.info("Database initialization complete. Created {} security systems", repository.count());
        };
    }
}