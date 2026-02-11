package io.eventuate.examples.realguardio.customerservice.db;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbInitializerConfig {

  private static final Logger logger = LoggerFactory.getLogger(DbInitializerConfig.class);

  @Bean DBInitializer dbInitializer() {
    return new DBInitializer();
  }

  @Bean
  CommandLineRunner initDatabase(DBInitializer dbInitializer, CustomerRepository customerRepository) {
    return args -> {
      // Check if data already exists
      if (customerRepository.count() > 0) {
        logger.info("Database already contains data, skipping initialization");
        return;
      }

      logger.info("Initializing database with sample data");

      dbInitializer.createCustomer();

      logger.info("Database initialization completed successfully");
    };
  }

}
