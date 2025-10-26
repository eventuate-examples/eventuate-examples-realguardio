package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;

public interface SecuritySystemRepositoryWithOso {

    List<SecuritySystemProjection> findAllAccessible(String userName);

}
