package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;

public interface SecuritySystemService {
    List<SecuritySystem> findAll();
}