package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JPAPersistenceConfiguration.class)
public class PersistenceIntegrationTestConfiguration {
}