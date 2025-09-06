package io.eventuate.examples.realguardio.securitysystemservice.domain;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRolesReplicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceClientReplicaImplTest {

    @Mock
    private LocationRolesReplicaService locationRolesReplicaService;

    private CustomerServiceClientReplicaImpl customerServiceClient;

    @BeforeEach
    void setUp() {
        customerServiceClient = new CustomerServiceClientReplicaImpl(locationRolesReplicaService);
    }

    @Test
    void shouldReturnRolesFromReplicaService() {
        // Given
        String userId = "user123";
        Long locationId = 456L;
        
        List<LocationRole> locationRoles = Arrays.asList(
            new LocationRole(1L, userId, locationId, "CAN_ARM"),
            new LocationRole(2L, userId, locationId, "CAN_DISARM")
        );
        
        when(locationRolesReplicaService.findLocationRoles(userId, locationId))
            .thenReturn(locationRoles);

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM");
    }

    @Test
    void shouldReturnEmptySetWhenNoRolesFound() {
        // Given
        String userId = "user123";
        Long locationId = 999L;
        
        when(locationRolesReplicaService.findLocationRoles(userId, locationId))
            .thenReturn(Collections.emptyList());

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnUniqueRolesWhenDuplicatesExist() {
        // Given
        String userId = "user123";
        Long locationId = 456L;
        
        List<LocationRole> locationRoles = Arrays.asList(
            new LocationRole(1L, userId, locationId, "CAN_VIEW"),
            new LocationRole(2L, userId, locationId, "CAN_VIEW"),
            new LocationRole(3L, userId, locationId, "CAN_EDIT")
        );
        
        when(locationRolesReplicaService.findLocationRoles(userId, locationId))
            .thenReturn(locationRoles);

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_VIEW", "CAN_EDIT");
    }
}