package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "io.eventuate.examples.realguardio.securitysystemservice.domain")
@EntityScan(basePackages = "io.eventuate.examples.realguardio.securitysystemservice.domain")
public class PersistenceIntegrationTestConfiguration {
}