package io.eventuate.examples.realguardio.securitysystemservice.db;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DBInitializerConfiguration {

    @Bean
    public DBInitializer dbInitializer(SecuritySystemRepository repository) {
        return new DBInitializer(repository);
    }

    @Bean
    CommandLineRunner initDatabase(DBInitializer db) {
        return args -> db.initialize();
    }

}