package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecuritySystemRepository extends JpaRepository<SecuritySystem, Long> {
}