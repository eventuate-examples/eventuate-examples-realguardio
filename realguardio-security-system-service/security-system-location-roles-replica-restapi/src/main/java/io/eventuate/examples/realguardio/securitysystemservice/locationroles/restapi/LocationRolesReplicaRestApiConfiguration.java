package io.eventuate.examples.realguardio.securitysystemservice.locationroles.restapi;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = LocationRolesController.class)
public class LocationRolesReplicaRestApiConfiguration {
}
