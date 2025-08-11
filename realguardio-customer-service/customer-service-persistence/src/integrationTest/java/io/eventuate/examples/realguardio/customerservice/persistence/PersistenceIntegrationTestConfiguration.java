package io.eventuate.examples.realguardio.customerservice.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "io.eventuate.examples.realguardio.customerservice.domain")
@EntityScan(basePackages = "io.eventuate.examples.realguardio.customerservice.domain")
public class PersistenceIntegrationTestConfiguration {
}