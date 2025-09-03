package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuritySystemServiceTest {

    @Mock
    private SecuritySystemRepository securitySystemRepository;

    private SecuritySystemService securitySystemService;

    @BeforeEach
    void setUp() {
        securitySystemService = new SecuritySystemServiceImpl(securitySystemRepository);
    }

    @Test
    void shouldReturnAllSecuritySystems() throws Exception {
        // Given
        SecuritySystem system1 = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED, 
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(system1, 1L);
        
        SecuritySystem system2 = new SecuritySystem("Office Back Door", SecuritySystemState.DISARMED, 
                new HashSet<>());
        setId(system2, 2L);
        
        List<SecuritySystem> expectedSystems = Arrays.asList(system1, system2);
        when(securitySystemRepository.findAll()).thenReturn(expectedSystems);
        
        // When
        List<SecuritySystem> actualSystems = securitySystemService.findAll();
        
        // Then
        assertThat(actualSystems).hasSize(2);
        assertThat(actualSystems).containsExactlyElementsOf(expectedSystems);
        assertThat(actualSystems.get(0).getLocationName()).isEqualTo("Office Front Door");
        assertThat(actualSystems.get(0).getState()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(actualSystems.get(1).getLocationName()).isEqualTo("Office Back Door");
        assertThat(actualSystems.get(1).getState()).isEqualTo(SecuritySystemState.DISARMED);
    }
    
    private void setId(SecuritySystem system, Long id) throws Exception {
        Field idField = SecuritySystem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

    @Test
    void shouldReturnEmptyListWhenNoSystemsExist() {
        // Given
        when(securitySystemRepository.findAll()).thenReturn(List.of());
        
        // When
        List<SecuritySystem> actualSystems = securitySystemService.findAll();
        
        // Then
        assertThat(actualSystems).isEmpty();
    }
    
    @Test
    void shouldDisarmSecuritySystem() throws Exception {
        // Given
        Long systemId = 1L;
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(456L);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SecuritySystem result = securitySystemService.disarm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.DISARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
    }
    
    @Test
    void shouldArmSecuritySystem() throws Exception {
        // Given
        Long systemId = 1L;
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(456L);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SecuritySystem result = securitySystemService.arm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.ARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
    }
}