package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = SecuritySystemRepository.class)
@EntityScan(basePackages = "io.eventuate.examples.realguardio.securitysystemservice.domain")
public class JPAPersistenceConfiguration {
}