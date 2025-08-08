package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SecuritySystemServiceImpl implements SecuritySystemService {
    
    private final SecuritySystemRepository securitySystemRepository;
    
    public SecuritySystemServiceImpl(SecuritySystemRepository securitySystemRepository) {
        if (securitySystemRepository == null) {
            throw new IllegalArgumentException("securitySystemRepository cannot be null");
        }
        this.securitySystemRepository = securitySystemRepository;
    }
    
    @Override
    public List<SecuritySystem> findAll() {
        return securitySystemRepository.findAll();
    }
}