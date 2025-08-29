# Security System Arm/Disarm Feature Specification

## Overview

This specification details the implementation of a REST endpoint to arm and disarm security systems with role-based access control. The feature requires coordination between the Security Service and Customer Service to enforce location-based permissions.

## Architecture Overview

### Services Involved
- **Security Service**: Manages security system state and operations
- **Customer Service**: Provides role-based authorization for locations
- **IAM Service**: Manages authentication and top-level roles (REALGUARDIO_ADMIN, REALGUARDIO_CUSTOMER_EMPLOYEE)

### Communication Pattern
- Synchronous REST calls between services
- JWT token forwarding for user identity propagation
- RestTemplate or WebClient for service-to-service communication

## API Specifications

### Security Service Endpoint

#### PUT /securitysystems/{id}
Arms or disarms a security system.

**Request:**
```http
PUT /securitysystems/123
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "action": "ARM" | "DISARM"
}
```

**Path Parameters:**
- `id` (Long): Security system database ID

**Request Body:**
```typescript
interface SecuritySystemActionRequest {
  action: "ARM" | "DISARM"
}
```

**Response - Success (200 OK):**
```json
{
  "id": 123,
  "locationId": 456,
  "locationName": "Warehouse-1",
  "state": "ARMED",
  "actions": ["ARM", "DISARM", "ACKNOWLEDGE"],
  "version": 2
}
```

**Response - Errors:**
- `400 Bad Request`: Invalid action or missing locationId
- `403 Forbidden`: User lacks required permission
- `404 Not Found`: Security system not found

### Customer Service Endpoint

#### GET /locations/{locationId}/roles
Returns the current user's roles at the specified location.

**Request:**
```http
GET /locations/456/roles
Authorization: Bearer <jwt-token>
```

**Path Parameters:**
- `locationId` (Long): Location ID

**Response (200 OK):**
```json
{
  "roles": ["CAN_ARM", "CAN_DISARM", "VIEW_ALERTS"]
}
```

**Response - Errors:**
- `404 Not Found`: Location not found or user has no roles at this location
- `401 Unauthorized`: Invalid or missing JWT token

## Data Model Changes

### SecuritySystem Entity Updates

Add the following field to the existing SecuritySystem entity:

```java
@Column(name = "location_id")
private Long locationId;

public Long getLocationId() {
    return locationId;
}

public void setLocationId(Long locationId) {
    this.locationId = locationId;
}
```

### Database Migration

```sql
ALTER TABLE security_systems 
ADD COLUMN location_id BIGINT;

-- Add foreign key constraint if locations table exists
ALTER TABLE security_systems
ADD CONSTRAINT fk_security_system_location
FOREIGN KEY (location_id) REFERENCES locations(id);
```

## Authorization Logic

### Authorization Flow Diagram

```
User Request → Security Service
    ↓
Extract JWT & Validate Top-Level Role
    ↓
REALGUARDIO_ADMIN? → Yes → Authorize Operation
    ↓ No
REALGUARDIO_CUSTOMER_EMPLOYEE? → No → Return 403
    ↓ Yes
Check locationId != null → No → Return 400
    ↓ Yes
Call Customer Service /locations/{id}/roles
    ↓
Check for CAN_ARM or CAN_DISARM → No → Return 403
    ↓ Yes
Authorize Operation
```

### Role Requirements

| User Role | Action | Required Location Role |
|-----------|--------|------------------------|
| REALGUARDIO_ADMIN | ARM | None (bypass check) |
| REALGUARDIO_ADMIN | DISARM | None (bypass check) |
| REALGUARDIO_CUSTOMER_EMPLOYEE | ARM | CAN_ARM |
| REALGUARDIO_CUSTOMER_EMPLOYEE | DISARM | CAN_DISARM |

### Customer Service Role Aggregation

The Customer Service must return the union of roles from:
1. **Direct roles**: CustomerEmployeeLocationRole entries
2. **Team-based roles**: Roles inherited from team memberships

SQL Query Example:
```sql
-- Direct roles
SELECT role_name 
FROM customer_employee_location_roles
WHERE customer_employee_id = :employeeId 
  AND location_id = :locationId

UNION

-- Team-based roles
SELECT tlr.role_name
FROM team_location_roles tlr
JOIN team_members tm ON tm.team_id = tlr.team_id
WHERE tm.customer_employee_id = :employeeId
  AND tlr.location_id = :locationId
```

## Implementation Details

### Security Service Controller

```java
@RestController
@RequestMapping("/securitysystems")
public class SecuritySystemController {
    
    private final SecuritySystemService securitySystemService;
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('REALGUARDIO_ADMIN') or hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public ResponseEntity<SecuritySystemDto> updateSecuritySystem(
            @PathVariable Long id,
            @RequestBody SecuritySystemActionRequest request) {
        
        SecuritySystemDto updated;
        
        switch (request.getAction()) {
            case ARM:
                updated = securitySystemService.arm(id);
                break;
            case DISARM:
                updated = securitySystemService.disarm(id);
                break;
            default:
                throw new BadRequestException("Unsupported action: " + request.getAction());
        }
        
        return ResponseEntity.ok(updated);
    }
}
```

### Security Service Implementation

```java
@Service
@Transactional
public class SecuritySystemService {
    
    private final SecuritySystemRepository repository;
    private final CustomerServiceClient customerServiceClient;
    
    @PreAuthorize("hasRole('REALGUARDIO_ADMIN') or hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public SecuritySystemDto arm(Long id) {
        SecuritySystem securitySystem = loadAndValidateSystem(id);
        
        // Check location-based authorization for customer employees
        if (isCustomerEmployee()) {
            validateLocationPermission(securitySystem.getLocationId(), "CAN_ARM");
        }
        
        // Arm the system
        securitySystem.arm();
        
        // Log the operation
        logOperation(getCurrentUser(), securitySystem, SecuritySystemAction.ARM);
        
        // Entity is automatically saved by JPA within transaction
        return SecuritySystemMapper.toDto(securitySystem);
    }
    
    @PreAuthorize("hasRole('REALGUARDIO_ADMIN') or hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
    public SecuritySystemDto disarm(Long id) {
        SecuritySystem securitySystem = loadAndValidateSystem(id);
        
        // Check location-based authorization for customer employees
        if (isCustomerEmployee()) {
            validateLocationPermission(securitySystem.getLocationId(), "CAN_DISARM");
        }
        
        // Disarm the system
        securitySystem.disarm();
        
        // Log the operation
        logOperation(getCurrentUser(), securitySystem, SecuritySystemAction.DISARM);
        
        // Entity is automatically saved by JPA within transaction
        return SecuritySystemMapper.toDto(securitySystem);
    }
    
    private SecuritySystem loadAndValidateSystem(Long id) {
        SecuritySystem securitySystem = repository.findById(id)
            .orElseThrow(() -> new SecuritySystemNotFoundException(id));
        
        // Check if locationId is configured
        if (securitySystem.getLocationId() == null) {
            throw new BadRequestException("Security system not properly configured: missing location");
        }
        
        return securitySystem;
    }
    
    private void validateLocationPermission(Long locationId, String requiredRole) {
        String jwt = extractJwtFromCurrentAuthentication();
        Set<String> roles = customerServiceClient.getUserRolesAtLocation(jwt, locationId);
        
        if (!roles.contains(requiredRole)) {
            throw new ForbiddenException(
                String.format("User lacks %s permission for location %d", 
                            requiredRole, locationId)
            );
        }
    }
    
    private boolean isCustomerEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_REALGUARDIO_CUSTOMER_EMPLOYEE"));
    }
    
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
    
    private String extractJwtFromCurrentAuthentication() {
        // Extract JWT token from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Implementation depends on JWT configuration
        return "Bearer " + auth.getCredentials();
    }
}
```

### Customer Service Controller

```java
@RestController
@RequestMapping("/locations")
public class LocationRoleController {
    
    private final LocationRoleService locationRoleService;
    
    @GetMapping("/{locationId}/roles")
    @PreAuthorize("hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE') or hasRole('REALGUARDIO_ADMIN')")
    public ResponseEntity<RolesResponse> getUserRolesAtLocation(
            @PathVariable Long locationId,
            Authentication authentication) {
        
        Set<String> roles = locationRoleService.getUserRolesAtLocation(
            authentication.getName(), // User ID from JWT
            locationId
        );
        
        if (roles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new RolesResponse(roles));
    }
}
```

### Customer Service Implementation

```java
@Service
public class LocationRoleService {
    
    private final CustomerEmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    
    public Set<String> getUserRolesAtLocation(String userId, Long locationId) {
        Set<String> allRoles = new HashSet<>();
        
        // Get direct roles
        CustomerEmployee employee = employeeRepository.findByMemberId(Long.parseLong(userId))
            .orElseThrow(() -> new NotFoundException("Employee not found"));
        
        Set<String> directRoles = employeeRepository
            .findDirectRolesAtLocation(employee.getId(), locationId);
        allRoles.addAll(directRoles);
        
        // Get team-based roles
        Set<String> teamRoles = teamRepository
            .findTeamRolesAtLocation(employee.getId(), locationId);
        allRoles.addAll(teamRoles);
        
        return allRoles;
    }
}
```

### Customer Service Client (Security Service Side)

```java
@Component
public class CustomerServiceClient {
    
    private final RestTemplate restTemplate;
    private final String customerServiceUrl;
    
    public Set<String> getUserRolesAtLocation(String jwt, Long locationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwt);
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<RolesResponse> response = restTemplate.exchange(
                customerServiceUrl + "/locations/{locationId}/roles",
                HttpMethod.GET,
                entity,
                RolesResponse.class,
                locationId
            );
            
            return response.getBody().getRoles();
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptySet();
        } catch (Exception e) {
            throw new ServiceUnavailableException("Authorization service temporarily unavailable");
        }
    }
}
```

### State Transition Logic

```java
public class SecuritySystem {
    
    public void arm() {
        if (state == SecuritySystemState.ALARMED) {
            throw new IllegalStateException("Cannot arm system in ALARMED state");
        }
        setState(SecuritySystemState.ARMED);
    }
    
    public void disarm() {
        setState(SecuritySystemState.DISARMED);
    }
    
    public void acknowledge() {
        if (state != SecuritySystemState.ALARMED) {
            throw new IllegalStateException("Can only acknowledge when in ALARMED state");
        }
        setState(SecuritySystemState.DISARMED);
    }
    
    private void setState(SecuritySystemState newState) {
        this.state = newState;
        this.lastModified = Instant.now();
    }
}
```

## Error Handling

### Error Response Format

```json
{
  "error": "FORBIDDEN",
  "message": "User lacks CAN_ARM permission for location 456",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/securitysystems/123"
}
```

### Error Scenarios

| Scenario | HTTP Status | Error Message |
|----------|------------|---------------|
| Invalid action in request | 400 | "Invalid action: {action}. Supported actions: ARM, DISARM" |
| SecuritySystem missing locationId | 400 | "Security system not properly configured: missing location" |
| SecuritySystem not found | 404 | "Security system not found: {id}" |
| User lacks permission | 403 | "User lacks {required_role} permission for location {locationId}" |
| Customer Service unavailable | 503 | "Authorization service temporarily unavailable" |
| Invalid JWT token | 401 | "Invalid or expired authentication token" |

## Logging Requirements

### Audit Log Format

Use SLF4J with the following format:

```java
logger.info("Security system action: user={}, action={}, systemId={}, " +
           "locationId={}, result={}, timestamp={}", 
           userId, action, systemId, locationId, result, timestamp);
```

### Log Levels

- **INFO**: Successful arm/disarm operations
- **WARN**: Authorization failures, missing locationId
- **ERROR**: Service communication failures, unexpected exceptions

### Example Log Entries

```
INFO  - Security system action: user=john.doe@example.com, action=ARM, systemId=123, locationId=456, result=SUCCESS, timestamp=2024-01-15T10:30:00Z
WARN  - Security system action: user=jane.smith@example.com, action=DISARM, systemId=789, locationId=null, result=FAILED_MISSING_LOCATION, timestamp=2024-01-15T10:31:00Z
ERROR - Failed to contact Customer Service for authorization: Connection timeout
```

## Testing Strategy

### Unit Tests

#### Security Service Tests

1. **Controller Tests**
   - Test valid ARM request with admin role
   - Test valid DISARM request with customer employee role
   - Test missing locationId returns 400
   - Test invalid action returns 400
   - Test missing authorization returns 403

2. **Service Tests**
   - Test state transitions (DISARMED → ARMED, ARMED → DISARMED)
   - Test idempotent operations (ARM when already ARMED)
   - Test authorization service integration

3. **Authorization Service Tests**
   - Test admin role bypasses location check
   - Test customer employee with valid role
   - Test customer employee without required role
   - Test handling of Customer Service failures

#### Customer Service Tests

1. **Controller Tests**
   - Test role retrieval for valid location
   - Test role aggregation (direct + team roles)
   - Test missing location returns 404
   - Test invalid JWT returns 401

2. **Repository Tests**
   - Test direct role query
   - Test team-based role query
   - Test union of roles

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecuritySystemIntegrationTest {
    
    @Test
    public void testArmSystemAsAdmin() {
        // Given: Admin JWT and security system with location
        // When: PUT /securitysystems/1 with ARM action
        // Then: System is armed, 200 OK returned
    }
    
    @Test
    public void testDisarmSystemAsEmployeeWithPermission() {
        // Given: Employee JWT with CAN_DISARM at location
        // When: PUT /securitysystems/1 with DISARM action
        // Then: System is disarmed, 200 OK returned
    }
    
    @Test
    public void testArmSystemAsEmployeeWithoutPermission() {
        // Given: Employee JWT without CAN_ARM at location
        // When: PUT /securitysystems/1 with ARM action
        // Then: 403 Forbidden returned
    }
}
```

### End-to-End Tests

1. **Happy Path Scenarios**
   - Admin arms and disarms system
   - Employee with permissions arms and disarms system
   - Multiple employees operate different systems

2. **Error Scenarios**
   - Employee attempts operation without permission
   - Operation on system without locationId
   - Customer Service unavailable during authorization

3. **Concurrency Tests**
   - Multiple simultaneous ARM/DISARM requests
   - Verify optimistic locking prevents conflicts

## Configuration Requirements

### Security Service Configuration

```yaml
customer-service:
  url: ${CUSTOMER_SERVICE_URL:http://localhost:8082}
  connection-timeout: 5000
  read-timeout: 5000
  
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}
    
logging:
  level:
    io.eventuate.examples.realguardio.securitysystem: INFO
```

### Customer Service Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}
    
logging:
  level:
    io.eventuate.examples.realguardio.customer: INFO
```

### Security Configuration Classes

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> authorities = jwt.getClaimAsStringList("authorities");
            return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return converter;
    }
}

## Deployment Considerations

### Service Dependencies
- Security Service depends on Customer Service for authorization
- Both services depend on IAM Service for JWT validation
- Database migrations must be run before deployment

### Health Checks
- Security Service should include Customer Service connectivity in health check
- Circuit breaker pattern recommended for Customer Service calls

### Performance Considerations
- Cache Customer Service responses (TTL: 5 minutes)
- Consider bulk operations for multiple security systems
- Monitor response times for authorization checks

## Security Considerations

### JWT Token Handling
- Never log JWT tokens
- Validate token signature and expiration
- Forward original token to downstream services

### Rate Limiting
- Implement rate limiting per user: 10 operations/minute
- Log excessive requests for security monitoring

### Data Validation
- Validate all input parameters
- Sanitize error messages to prevent information leakage
- Use parameterized queries to prevent SQL injection

## Migration Plan

### Phase 1: Deploy Database Changes
1. Add locationId column (nullable initially)
2. Deploy updated entities

### Phase 2: Deploy Service Updates
1. Deploy Customer Service with new endpoint
2. Deploy Security Service with new endpoint
3. Keep existing functionality unchanged

### Phase 3: Data Migration
1. Populate locationId for existing security systems
2. Make locationId non-nullable after migration

### Rollback Strategy
- Feature flag to disable new endpoint
- Database changes are backward compatible
- Services can be rolled back independently

## Acceptance Criteria

1. ✅ Admin can arm/disarm any security system
2. ✅ Employee with CAN_ARM can arm systems at their location
3. ✅ Employee with CAN_DISARM can disarm systems at their location
4. ✅ Employee without permission receives 403 error
5. ✅ System without locationId returns 400 error
6. ✅ All operations are logged for audit
7. ✅ Customer Service returns union of direct and team roles
8. ✅ JWT token is properly forwarded between services
9. ✅ Service handles Customer Service unavailability gracefully
10. ✅ Concurrent operations use optimistic locking

## References

- JWT Authentication: [Internal IAM Documentation]
- REST API Standards: [Company API Guidelines]
- Logging Standards: [Company Logging Guidelines]
- Security Best Practices: [OWASP REST Security Cheat Sheet]