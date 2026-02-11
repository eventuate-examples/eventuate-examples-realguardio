package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.repository;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemFinder;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemProjection;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SecuritySystemFinderUsingRepository implements SecuritySystemFinder {

    @Autowired
    private SecuritySystemRepository securitySystemRepository;

    @Override
    public List<SecuritySystemProjection> findAllAccessible(String userName) {
        return securitySystemRepository.findAllAccessible(userName);
    }
}
