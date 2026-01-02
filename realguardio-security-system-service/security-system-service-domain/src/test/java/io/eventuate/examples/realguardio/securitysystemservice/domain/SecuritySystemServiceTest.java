package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecuritySystemServiceTest {

    @Mock
    private SecuritySystemRepository securitySystemRepository;
    
    @Mock
    private CustomerServiceClient customerServiceClient;
    
    @Mock
    private UserNameSupplier userNameSupplier;

    @Mock
    private SecuritySystemActionAuthorizer securitySystemActionAuthorizer;

    @Mock
    private SecuritySystemEventPublisher securitySystemEventPublisher;

    private SecuritySystemService securitySystemService;

    @Mock
    private SecuritySystemFinder securitySystemFinder;

    @BeforeEach
    void setUp() {
        securitySystemService = new SecuritySystemServiceImpl(securitySystemRepository, customerServiceClient, userNameSupplier, securitySystemActionAuthorizer, securitySystemFinder, securitySystemEventPublisher);
    }

    @Test
    void shouldReturnAllSecuritySystems() {
        // Given
        String userId = "user123";

        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);

        // new HashSet<>(Arrays.asList(SecuritySystemAction.ARM))
        SecuritySystemProjection system1 = new SecuritySystemProjectionImpl(1L, "Office Front Door", SecuritySystemState.ARMED, Set.of());

        // new HashSet<>()

        SecuritySystemProjection system2 = new SecuritySystemProjectionImpl(2L, "Office Back Door", SecuritySystemState.DISARMED, Set.of());

        List<SecuritySystemProjection> expectedSystems = List.of(system1, system2);
        when(securitySystemFinder.findAllAccessible(userId)).thenReturn(expectedSystems);
        
        // When
        List<SecuritySystemWithActions> actualSystems = securitySystemService.findAll();
        
        // Then
        assertThat(actualSystems).hasSize(2);
        // assertThat(actualSystems).containsExactlyElementsOf(expectedSystems);
        assertThat(actualSystems.get(0).locationName()).isEqualTo("Office Front Door");
        assertThat(actualSystems.get(0).state()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(actualSystems.get(1).locationName()).isEqualTo("Office Back Door");
        assertThat(actualSystems.get(1).state()).isEqualTo(SecuritySystemState.DISARMED);
    }
    
    private void setId(SecuritySystem system, Long id) throws Exception {
        Field idField = SecuritySystem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

    @Test
    void shouldReturnEmptyListWhenNoSystemsExist() {
        // Given
        String userId = "user123";

        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);
        when(userNameSupplier.getCurrentUserName()).thenReturn(userId);

        when(securitySystemFinder.findAllAccessible(userId)).thenReturn(List.of());
        
        // When
        List<SecuritySystemWithActions> actualSystems = securitySystemService.findAll();
        
        // Then
        assertThat(actualSystems).isEmpty();
    }
    
    @Test
    void shouldDisarmSecuritySystem() throws Exception {
        // Given
        Long systemId = 1L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED);
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
        // new HashSet<>(Arrays.asList(SecuritySystemAction.ARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED);
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
        // new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED);
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
        // Admin should not trigger authorization check
        verify(securitySystemActionAuthorizer, never()).verifyCanDo(anyLong(), eq(RolesAndPermissions.DISARM));
    }
    
    @Test
    void employeeWithCanDisarmPermissionShouldDisarm() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED);
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);

        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);

        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SecuritySystem result = securitySystemService.disarm(systemId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.DISARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        verify(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.DISARM);
    }
    
    @Test
    void employeeWithoutCanDisarmPermissionShouldGetForbiddenException() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED);
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);

        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);

        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        doThrow(new ForbiddenException("User lacks SECURITY_SYSTEM_DISARMER permission for location 456"))
            .when(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.DISARM);

        // When & Then
        assertThatThrownBy(() -> securitySystemService.disarm(systemId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("User lacks SECURITY_SYSTEM_DISARMER permission for location 456");

        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.DISARM);
        verify(securitySystemRepository, never()).save(any());
    }
    
    @Test
    void adminShouldBypassLocationCheckWhenArming() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.ARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED);
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
        // Admin should not trigger authorization check
        verify(securitySystemActionAuthorizer, never()).verifyCanDo(anyLong(), eq(RolesAndPermissions.ARM));
    }
    
    @Test
    void employeeWithCanArmPermissionShouldArm() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.ARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED);
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);

        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);

        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        when(securitySystemRepository.save(any(SecuritySystem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SecuritySystem result = securitySystemService.arm(systemId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(SecuritySystemState.ARMED);
        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemRepository).save(securitySystem);
        verify(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.ARM);
    }
    
    @Test
    void employeeWithoutCanArmPermissionShouldGetForbiddenException() throws Exception {
        // Given
        Long systemId = 1L;
        Long locationId = 456L;
        // new HashSet<>(Arrays.asList(SecuritySystemAction.ARM))
        SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED);
        setId(securitySystem, systemId);
        securitySystem.setLocationId(locationId);

        // Set up employee
        when(userNameSupplier.isCustomerEmployee()).thenReturn(true);

        when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
        doThrow(new ForbiddenException("User lacks SECURITY_SYSTEM_ARMER permission for location 456"))
            .when(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.ARM);

        // When & Then
        assertThatThrownBy(() -> securitySystemService.arm(systemId))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("User lacks SECURITY_SYSTEM_ARMER permission for location 456");

        verify(securitySystemRepository).findById(systemId);
        verify(securitySystemActionAuthorizer).verifyCanDo(systemId, RolesAndPermissions.ARM);
        verify(securitySystemRepository, never()).save(any());
    }

    @Test
    void shouldCreateSecuritySystemWithLocationAndPublishEvent() throws Exception {
        // Given
        Long locationId = 100L;
        String locationName = "Main Office";
        Long expectedSecuritySystemId = 1L;

        SecuritySystem savedSecuritySystem = new SecuritySystem(locationName, SecuritySystemState.DISARMED);
        setId(savedSecuritySystem, expectedSecuritySystemId);
        savedSecuritySystem.setLocationId(locationId);

        when(securitySystemRepository.save(any(SecuritySystem.class))).thenReturn(savedSecuritySystem);

        // When
        Long securitySystemId = securitySystemService.createSecuritySystemWithLocation(locationId, locationName);

        // Then
        assertThat(securitySystemId).isEqualTo(expectedSecuritySystemId);

        // Verify the security system was saved with correct state and locationId
        verify(securitySystemRepository).save(argThat(ss ->
            ss.getLocationName().equals(locationName) &&
            ss.getState() == SecuritySystemState.DISARMED &&
            ss.getLocationId().equals(locationId)
        ));

        // Verify the event was published
        verify(securitySystemEventPublisher).publish(
            argThat((SecuritySystem ss) -> ss.getId().equals(expectedSecuritySystemId)),
            argThat((SecuritySystemAssignedToLocation event) ->
                event.securitySystemId().equals(expectedSecuritySystemId) &&
                event.locationId().equals(locationId)
            )
        );
    }

}