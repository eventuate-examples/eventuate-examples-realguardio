package io.eventuate.examples.realguardio.customerservice.db;

import io.eventuate.examples.realguardio.customerservice.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerAction;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerRepository;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerState;
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
    CommandLineRunner initDatabase(CustomerRepository repository) {
        return args -> {
            // Check if data already exists
            if (repository.count() > 0) {
                logger.info("Database already contains data, skipping initialization");
                return;
            }
            
            logger.info("Initializing database with sample security systems");
            
            Customer system1 = new Customer(LOCATION_OAKLAND_OFFICE,
                    CustomerState.ARMED, 
                    Set.of(CustomerAction.DISARM));
            repository.save(system1);
            logger.info("Created security system: {}", system1.getLocationName());
            
            Customer system2 = new Customer(LOCATION_BERKELEY_OFFICE,
                    CustomerState.DISARMED, 
                    Set.of(CustomerAction.ARM));
            repository.save(system2);
            logger.info("Created security system: {}", system2.getLocationName());
            
            Customer system3 = new Customer(LOCATION_HAYWARD_OFFICE,
                    CustomerState.ALARMED, 
                    Set.of(CustomerAction.ACKNOWLEDGE, CustomerAction.DISARM));
            repository.save(system3);
            logger.info("Created security system: {}", system3.getLocationName());
            
            logger.info("Database initialization complete. Created {} security systems", repository.count());
        };
    }
}