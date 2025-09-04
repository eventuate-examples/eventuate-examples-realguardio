package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationRoleServiceImplTest {
    
    @Mock
    private CustomerEmployeeLocationRoleRepository locationRoleRepository;
    
    @Mock
    private TeamLocationRoleRepository teamLocationRoleRepository;
    
    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserNameSupplier userNameSupplier;

    private LocationRoleServiceImpl locationRoleService;


    @BeforeEach
    void setUp() {
        locationRoleService = new LocationRoleServiceImpl(
            locationRoleRepository,
            teamLocationRoleRepository,
            locationRepository, userNameSupplier
        );
    }
    
    @Test
    void shouldHandleNonNumericUserId() {
        // Given
        String userId = "non-numeric-user";
        Long locationId = 456L;
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then - should handle gracefully and return empty set
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldReturnDirectRolesForEmployeeAtLocation() {
        // Given
        String userId = "123";
        Long employeeId = 123L;
        Long locationId = 456L;
        Long customerId = 789L;
        String userName = "user132@example.com";

        List<String> directRoles = List.of("CAN_ARM", "CAN_DISARM");
        when(locationRoleRepository.findRoleNamesByUserNameAndLocationId(userName, locationId))
            .thenReturn(directRoles);

        when(userNameSupplier.getCurrentUserEmail()).thenReturn(userName);
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM");
    }
    
    @Test
    void shouldReturnEmptySetWhenNoRoles() {
        // Given
        String userId = "999";
        Long employeeId = 999L;
        Long locationId = 111L;
        Long customerId = 789L;
        String userName = "user132@example.com";

        when(userNameSupplier.getCurrentUserEmail()).thenReturn(userName);

        when(locationRoleRepository.findRoleNamesByUserNameAndLocationId(
            userName, locationId))
            .thenReturn(List.of());
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void shouldAggregateDirectAndTeamBasedRoles() {
        // Given
        String userName = "user@example.com";
        Long locationId = 456L;
        
        List<String> directRoles = List.of("CAN_ARM", "VIEW_ALERTS");
        List<String> teamRoles = List.of("CAN_DISARM", "VIEW_ALERTS");
        
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(userName);
        when(locationRoleRepository.findRoleNamesByUserNameAndLocationId(userName, locationId))
            .thenReturn(directRoles);
        when(teamLocationRoleRepository.findTeamRolesByUserNameAndLocationId(userName, locationId))
            .thenReturn(teamRoles);
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM", "VIEW_ALERTS");
    }
    
    @Test
    void shouldHandleUserWithOnlyTeamRoles() {
        // Given
        String userName = "user@example.com";
        Long locationId = 456L;
        
        List<String> teamRoles = List.of("CAN_ARM", "CAN_DISARM");
        
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(userName);
        when(locationRoleRepository.findRoleNamesByUserNameAndLocationId(userName, locationId))
            .thenReturn(List.of());
        when(teamLocationRoleRepository.findTeamRolesByUserNameAndLocationId(userName, locationId))
            .thenReturn(teamRoles);
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM");
    }
    
    @Test
    void shouldHandleUserWithBothDirectAndTeamRoles() {
        // Given
        String userName = "user@example.com";
        Long locationId = 456L;
        
        List<String> directRoles = List.of("CAN_ARM");
        List<String> teamRoles = List.of("CAN_DISARM", "MANAGE_USERS");
        
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(userName);
        when(locationRoleRepository.findRoleNamesByUserNameAndLocationId(userName, locationId))
            .thenReturn(directRoles);
        when(teamLocationRoleRepository.findTeamRolesByUserNameAndLocationId(userName, locationId))
            .thenReturn(teamRoles);
        
        // When
        Set<String> result = locationRoleService.getUserRolesAtLocation(locationId);
        
        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM", "MANAGE_USERS");
    }
}