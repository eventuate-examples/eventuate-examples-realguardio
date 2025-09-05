# CQRS Replica Location Roles - Implementation Specification

## Overview
Implement a CQRS (Command Query Responsibility Segregation) pattern where the Security Service maintains a read-only replica of customer employee location roles by subscribing to domain events published by the Customer Service.

## Architecture

### Event Flow
1. Customer Service assigns a location role to an employee
2. Customer Service publishes `CustomerEmployeeAssignedLocationRole` event via Eventuate Tram
3. Security Service consumes the event
4. Security Service updates its local replica database
5. Security Service provides query API for the replicated data

### Technology Stack
- **Event Bus**: Eventuate Tram with Kafka
- **Event Publishing**: DomainEventPublisher
- **Event Consuming**: DomainEventDispatcher
- **Database Access**: JDBC
- **Framework**: Spring Boot

## Implementation Details

### Customer Service (Publisher)

#### Event Class
**Package**: `io.eventuate.examples.realguardio.customerservice.domain`  
**Class**: `CustomerEmployeeAssignedLocationRole`

```java
package io.eventuate.examples.realguardio.customerservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record CustomerEmployeeAssignedLocationRole(
    String userName,
    Long locationId,
    String roleName
) implements DomainEvent {
}
```

**Important**: This event class must be duplicated in both services in the exact same package to ensure proper serialization/deserialization.

#### Event Publishing
**Location**: `CustomerService.assignLocationRole()`  
**Implementation**: Direct call to DomainEventPublisher

```java
public void assignLocationRole(String userName, Long locationId, String roleName) {
    // Business logic
    
    // Publish event
    domainEventPublisher.publish(
        Customer.class,
        customerId,
        Collections.singletonList(
            new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName)
        )
    );
}
```

### Security Service (Consumer)

#### Project Structure
Create new subproject: `location-roles-replica`

#### Event Class (Copy)
**Package**: `io.eventuate.examples.realguardio.customerservice.domain`  
**Class**: `CustomerEmployeeAssignedLocationRole`

The Security Service must have its own copy of this event class in the **exact same package** as the Customer Service:

```java
package io.eventuate.examples.realguardio.customerservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record CustomerEmployeeAssignedLocationRole(
    String userName,
    Long locationId,
    String roleName
) implements DomainEvent {
}
```

#### Database Schema
**Table**: `customer_employee_location_role`

```sql
CREATE TABLE customer_employee_location_role (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    location_id BIGINT NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Event Consumer
**Pattern**: Event handler delegates to service layer

```java
@Component
public class CustomerEmployeeLocationEventConsumer {
    
    @Autowired
    private LocationRolesReplicaService replicaService;
    
    public DomainEventHandlers domainEventHandlers() {
        return DomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.realguardio.customerservice.domain.Customer")
            .onEvent(CustomerEmployeeAssignedLocationRole.class, this::handleCustomerEmployeeAssignedLocationRole)
            .build();
    }
    
    private void handleCustomerEmployeeAssignedLocationRole(DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        CustomerEmployeeAssignedLocationRole event = envelope.getEvent();
        replicaService.saveLocationRole(
            event.userName(),
            event.locationId(),
            event.roleName()
        );
    }
}
```

#### Service Layer
```java
@Service
@Transactional
public class LocationRolesReplicaService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void saveLocationRole(String userName, Long locationId, String roleName) {
        String sql = "INSERT INTO customer_employee_location_role (user_name, location_id, role_name) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userName, locationId, roleName);
    }
    
    public List<LocationRole> findLocationRoles(String userName, Long locationId) {
        // Query implementation
    }
}
```

#### REST API
**Endpoint**: `GET /location-roles`  
**Query Parameters**: 
- `userName` (optional)
- `locationId` (optional)

```java
@RestController
@RequestMapping("/location-roles")
public class LocationRolesController {
    
    @Autowired
    private LocationRolesReplicaService replicaService;
    
    @GetMapping
    public List<LocationRole> getLocationRoles(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long locationId) {
        return replicaService.findLocationRoles(userName, locationId);
    }
}
```

## Configuration

### Customer Service
Add Eventuate Tram event publishing configuration:
```yaml
eventuate:
  database:
    schema: customer
```

### Security Service
Add Eventuate Tram event consuming configuration:
```yaml
eventuate:
  database:
    schema: security
spring:
  datasource:
    # Database configuration for replica storage
```

## Error Handling
- Event handlers throw exceptions on failure
- Eventuate Tram handles automatic retries with exponential backoff
- Failed messages eventually moved to dead letter queue
- No custom error handling code required

## Data Consistency
- **Type**: Eventually consistent
- **Duplicate Handling**: No special handling - rely on natural insert behavior
- **Ordering**: Events processed in order per aggregate

## Testing Strategy

### 1. Unit Test - Customer Service
**File**: `CustomerServiceTest`  
**Test**: `shouldCreateCustomerEmployeeAndAssignLocationRoles()`
- Mock DomainEventPublisher
- Verify event is published when assignLocationRole() is called
- Verify event contains correct data

### 2. Unit Test - Security Service
**File**: `SecurityServiceTest` or `LocationRolesReplicaServiceTest`
**Test**: `shouldConsumeEventAndUpdateDatabase()`
- Use Eventuate Tram in-memory message broker
- Publish `CustomerEmployeeAssignedLocationRole` event
- Verify database is updated by calling LocationRolesReplicaService
- Verify correct data is stored in replica table

### 3. Component Test - Customer Service
**File**: `CustomerServiceInProcessComponentTest`
- Verify event is written to outbox table
- Use actual database and Eventuate Tram

### 4. Component Test - Security Service
**File**: `SecuritySystemServiceComponentTest`
- Publish test event
- Verify database is updated with replica data
- Test query API returns correct data

### 5. End-to-End Test
**File**: `RealGuardioEndToEndTest`
- Call Customer Service API to assign location role
- Wait for event propagation
- Query Security Service API
- Verify replica contains expected data

## Implementation Steps

### Phase 1: Customer Service Event Publishing
1. Write unit test `CustomerServiceTest.shouldCreateCustomerEmployeeAndAssignLocationRoles()`
2. Create `CustomerEmployeeAssignedLocationRole` event class in `io.eventuate.examples.realguardio.customerservice.domain` package
3. Inject DomainEventPublisher into CustomerService
4. Implement event publishing in `assignLocationRole()`
5. Run test, ensure it passes

### Phase 2: Security Service Infrastructure
1. Create `location-roles-replica` subproject
2. Add Gradle dependencies for Eventuate Tram
3. **Copy `CustomerEmployeeAssignedLocationRole` event class to the same package** `io.eventuate.examples.realguardio.customerservice.domain`
4. Create database migration for `customer_employee_location_role` table
5. Configure Spring Boot application

### Phase 3: Event Consumer Implementation
1. Create `CustomerEmployeeLocationEventConsumer` class
2. Create `LocationRolesReplicaService` with JDBC implementation
3. Register event handlers with DomainEventDispatcher
4. Write component test for event consumption

### Phase 4: Query API
1. Create `LocationRolesController` REST controller
2. Implement query methods in service layer
3. Add integration tests for API

### Phase 5: Integration Testing
1. Update `CustomerServiceInProcessComponentTest` to verify outbox
2. Update `SecuritySystemServiceComponentTest` for event handling
3. Add end-to-end test in `RealGuardioEndToEndTest`

## Dependencies

### Customer Service
```gradle
dependencies {
    implementation 'io.eventuate.tram.core:eventuate-tram-spring-events'
    implementation 'io.eventuate.tram.core:eventuate-tram-spring-producer-jdbc'
}
```

### Security Service - location-roles-replica
```gradle
dependencies {
    implementation 'io.eventuate.tram.core:eventuate-tram-spring-events'
    implementation 'io.eventuate.tram.core:eventuate-tram-spring-consumer-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

## Success Criteria
1. ✅ Customer Service publishes events when location roles are assigned
2. ✅ Security Service successfully consumes and stores events
3. ✅ Query API returns correct replica data
4. ✅ All tests pass (unit, component, end-to-end)
5. ✅ No data loss during normal operations
6. ✅ System handles transient failures gracefully

## Notes
- Events use simple schema with no versioning initially
- Database uses denormalized structure for query optimization
- No special idempotency handling - rely on database behavior
- Follow TDD approach starting with unit test