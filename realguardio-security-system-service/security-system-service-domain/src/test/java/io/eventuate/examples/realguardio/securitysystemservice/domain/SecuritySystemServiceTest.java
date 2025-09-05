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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecuritySystemServiceTest {

    @Mock
    private SecuritySystemRepository securitySystemRepository;
    
    @Mock
    private CustomerServiceClient customerServiceClient;
    
    @Mock
    private UserNameSupplier userNameSupplier;

    private SecuritySystemService securitySystemService;

    @BeforeEach
    void setUp() {
        securitySystemService = new SecuritySystemServiceImpl(securitySystemRepository, customerServiceClient, userNameSupplier);
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
    
    @Test
    void adminShouldBypassLocationCheckWhenDisarming() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up admin (not a customer employee)
        when(userNameSupplier.isCustomerEmployee()).thenReturn(false);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SecuritySystem result = securitySystemService.disarm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.DISARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        // Admin should not trigger location permission check
        verify(customerServiceClient, never()).getUserRolesAtLocation(anyString(), anyLong());
    }
    
    @Test
    void employeeWithCanDisarmPermissionShouldDisarm() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        String userId = "employee@example.com";
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
            .thenReturn(new HashSet<>(Arrays.asList("CAN_DISARM", "VIEW_ALERTS")));
        
        // When
        SecuritySystem result = securitySystemService.disarm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.DISARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        verify(customerServiceClient).getUserRolesAtLocation(userId, locationId);
    }
    
    @Test
    void employeeWithoutCanDisarmPermissionShouldGetForbiddenException() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        String userId = "employee@example.com";
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
            .thenReturn(new HashSet<>(Arrays.asList("VIEW_ALERTS", "CAN_ARM"))); // Has CAN_ARM but not CAN_DISARM
        
        // When & Then
        assertThatThrownBy(() -> securitySystemService.disarm(systemId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("User lacks CAN_DISARM permission for location 456");
        
        verify(securitySystemRepository).findById(systemId);
        verify(customerServiceClient).getUserRolesAtLocation(userId, locationId);
        verify(securitySystemRepository, never()).save(any());
    }
    
    @Test
    void adminShouldBypassLocationCheckWhenArming() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up admin (not a customer employee)
        when(userNameSupplier.isCustomerEmployee()).thenReturn(false);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SecuritySystem result = securitySystemService.arm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.ARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        // Admin should not trigger location permission check
        verify(customerServiceClient, never()).getUserRolesAtLocation(anyString(), anyLong());
    }
    
    @Test
    void employeeWithCanArmPermissionShouldArm() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        String userId = "employee@example.com";
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
            .thenReturn(new HashSet<>(Arrays.asList("CAN_ARM", "VIEW_ALERTS")));
        
        // When
        SecuritySystem result = securitySystemService.arm(systemId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.ARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        verify(customerServiceClient).getUserRolesAtLocation(userId, locationId);
    }
    
    @Test
    void employeeWithoutCanArmPermissionShouldGetForbiddenException() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        String userId = "employee@example.com";
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);
        
        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);
        
        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
            .thenReturn(new HashSet<>(Arrays.asList("VIEW_ALERTS", "CAN_DISARM"))); // Has CAN_DISARM but not CAN_ARM
        
        // When & Then
        assertThatThrownBy(() -> securitySystemService.arm(systemId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("User lacks CAN_ARM permission for location 456");
        
        verify(securitySystemRepository).findById(systemId);
        verify(customerServiceClient).getUserRolesAtLocation(userId, locationId);
        verify(securitySystemRepository, never()).save(any());
    }
    
}