package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;
import java.util.Optional;

public interface SecuritySystemService {
    List<SecuritySystemWithActions> findAll();

    Optional<SecuritySystem> findById(Long id);

    void noteLocationCreated(Long securitySystemId, Long locationId);

    void updateCreationFailed(Long securitySystemId, String rejectionReason);

    Long createSecuritySystemWithLocation(Long locationId, String locationName);

    SecuritySystem arm(Long id);

    SecuritySystem disarm(Long id);
}