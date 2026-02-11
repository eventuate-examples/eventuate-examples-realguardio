package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.oso;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemFinder;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemProjection;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepositoryWithOso;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SecuritySystemFinderUsingRepositoryWithOso implements SecuritySystemFinder {

    @Autowired
    private SecuritySystemRepositoryWithOso securitySystemRepository;

    @Override
    public List<SecuritySystemProjection> findAllAccessible(String userName) {
        return securitySystemRepository.findAllAccessible(userName);
    }
}
