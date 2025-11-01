# Oso Fact Tables Population Requirements

## Overview

This document describes the events that the **Security System Service** needs to consume and the **Customer Service** must publish to populate the Oso authorization fact tables using the CQRS pattern.

The Oso fact tables are defined in `realguardio-security-system-service/location-roles-replica/src/main/resources/flyway/postgresql/V10002__oso__fact_tables.sql` and support the authorization model defined in `main.polar`.

## Target Tables

The following tables need to be populated via event consumption:

| Table | Purpose | Primary Key |
|-------|---------|-------------|
| `customer_employee_customer_roles` | Role assignments for CustomerEmployees at Customer level | (customer_employee_id, role_name, customer_id) |
| `team_location_roles` | Role assignments for Teams at Location level | (team_id, role_name, location_id) |
| `team_members` | Team membership relationships | (team_id, customer_employee_id) |
| `locations` | Locations and their relationship to Customers | (id) |

## Events by Table

The following table shows which events update each database table. **Note**: All events use aggregate type `"Customer"` with the Customer ID as the aggregate ID.

| Database Table | Events (Operation) | Event Status | Business Logic Status |
|----------------|-------------------|--------------|----------------------|
| `customer_employee_customer_roles` | `CustomerEmployeeAssignedCustomerRole` (INSERT) | ✅ Exists & Published | ✅ `CustomerService.assignRoleInternal()` |
| `team_location_roles` | `TeamAssignedLocationRole` (INSERT) | ⚠️ Need to create | ✅ `CustomerService.assignTeamRole()` exists |
| | `TeamLocationRoleRemoved` (DELETE) | ⚠️ Need to create | ❌ No service method exists |
| `team_members` | `TeamMemberAdded` (INSERT) | ⚠️ Need to create | ✅ `CustomerService.addTeamMember()` exists |
| | `TeamMemberRemoved` (DELETE) | ⚠️ Need to create | ❌ No service method exists |
| `locations` | `LocationCreatedForCustomer` (INSERT) | ✅ Exists & Published | ✅ `CustomerService.createLocation()` |

## Event Architecture

### Technology Stack
- **Event Bus**: Kafka
- **Framework**: Eventuate Tram
- **Pattern**: CQRS with Event Sourcing
- **Consistency**: Eventually Consistent
- **Event Publishing**: Type-safe `CustomerEventPublisher` (extends `DomainEventPublisherForAggregate`)
- **Event Handling**: Annotation-based with `@EventuateDomainEventHandler`

### Event Flow
```
Customer Service (Command Side)
    |
    | CustomerEventPublisher (type-safe)
    |
    v
  Kafka
    |
    | @EventuateDomainEventHandler methods
    |
    v
Security System Service - location-roles-replica (Query Side)
    |
    | Event Consumer methods
    |
    v
Oso Fact Tables (Read Model)
```

## Events Required

### 1. Events That Already Exist

These events are already published by Customer Service and can be consumed immediately:

#### 1.1 CustomerEmployeeAssignedCustomerRole

**Source**: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerEmployeeAssignedCustomerRole.java`

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record CustomerEmployeeAssignedCustomerRole(
    Long customerEmployeeId,
    String roleName
) implements CustomerEvent {
}
```

**Published by**: `CustomerService.assignRoleInternal()` using `customerEventPublisher.publish(customer, event)`
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Target Table**: `customer_employee_customer_roles`

**Mapping**:
- `customerEmployeeId` → `customer_employee_id` (convert Long to String)
- `roleName` → `role_name`
- Aggregate ID → `customer_id`

---

#### 1.2 LocationCreatedForCustomer

**Source**: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/LocationCreatedForCustomer.java`

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record LocationCreatedForCustomer(Long locationId) implements CustomerEvent {
}
```

**Published by**: `CustomerService.createLocation()` using `customerEventPublisher.publish(customer, event)`
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Target Table**: `locations`

**Mapping**:
- `locationId` → `id`
- Aggregate ID → `customer_id`

---

### 2. Events That Need to Be Created

These events do not currently exist and must be created in the Customer Service to populate the V10002 Oso fact tables.

**Implementation Status Summary**:
- 2 events have existing business logic that just needs event publishing added
- 2 events require new business logic methods to be implemented

#### 2.1 TeamMemberAdded

**Purpose**: Notify when a customer employee is added to a team
**Required**: **YES** - Critical for populating `team_members` table
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Business Logic**: ✅ **Already exists** in `CustomerService.addTeamMember()`
**Implementation**: Add event publishing to existing method

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamMemberAdded(
    Long teamId,
    Long customerEmployeeId
) implements CustomerEvent {
}
```

**Published by**: `CustomerService.addTeamMember()` using `customerEventPublisher.publish(customer, event)`
**Target Table**: `team_members`

**Mapping**:
- `teamId` → `team_id` (convert Long to String)
- `customerEmployeeId` → `customer_employee_id` (convert Long to String)

---

#### 2.2 TeamMemberRemoved

**Purpose**: Notify when a customer employee is removed from a team
**Required**: **YES** - Critical for maintaining accurate `team_members` table
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Business Logic**: ❌ **Does not exist** - must implement `CustomerService.removeTeamMember()`
**Implementation**: Create new service method with event publishing

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamMemberRemoved(
    Long teamId,
    Long customerEmployeeId
) implements CustomerEvent {
}
```

**Published by**: Future `CustomerService.removeTeamMember()` using `customerEventPublisher.publish(customer, event)`
**Target Table**: `team_members` (DELETE operation)

**Mapping**: Same as TeamMemberAdded, but triggers DELETE instead of INSERT

---

#### 2.3 TeamAssignedLocationRole

**Purpose**: Notify when a team is assigned a role at a specific location
**Required**: **YES** - Critical for populating `team_location_roles` table
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Business Logic**: ✅ **Already exists** in `CustomerService.assignTeamRole()`
**Implementation**: Add event publishing to existing method

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamAssignedLocationRole(
    Long teamId,
    Long locationId,
    String roleName
) implements CustomerEvent {
}
```

**Published by**: `CustomerService.assignTeamRole()` using `customerEventPublisher.publish(customer, event)`
**Target Table**: `team_location_roles`

**Mapping**:
- `teamId` → `team_id` (convert Long to String)
- `locationId` → `location_id`
- `roleName` → `role_name`

---

#### 2.4 TeamLocationRoleRemoved

**Purpose**: Notify when a team's role assignment at a location is removed
**Required**: **YES** - Critical for maintaining accurate `team_location_roles` table
**Aggregate Type**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer` (fully qualified)
**Aggregate ID**: Customer ID (Long)
**Business Logic**: ❌ **Does not exist** - must implement `CustomerService.removeTeamRole()`
**Implementation**: Create new service method with event publishing

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamLocationRoleRemoved(
    Long teamId,
    Long locationId,
    String roleName
) implements CustomerEvent {
}
```

**Published by**: Future `CustomerService.removeTeamRole()` using `customerEventPublisher.publish(customer, event)`
**Target Table**: `team_location_roles` (DELETE operation)

**Mapping**: Same as TeamAssignedLocationRole, but triggers DELETE instead of INSERT

---

## Event Consumer Implementation

### Module Location
`realguardio-security-system-service/location-roles-replica/`

### Required Components

#### 1. Event Consumer Class

Create or update: `OsoFactTablesEventConsumer.java`

**Note**: Events must be copied to the Security System Service in package `io.eventuate.examples.realguardio.customerservice.domain` (shorter package) for proper deserialization.

```java
package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.customerservice.domain.*;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.OsoFactTablesReplicaService;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.annotations.EventuateDomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OsoFactTablesEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OsoFactTablesEventConsumer.class);

    private final OsoFactTablesReplicaService replicaService;

    @Autowired
    public OsoFactTablesEventConsumer(OsoFactTablesReplicaService replicaService) {
        this.replicaService = replicaService;
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleCustomerEmployeeAssignedCustomerRole(
            DomainEventEnvelope<CustomerEmployeeAssignedCustomerRole> envelope) {
        CustomerEmployeeAssignedCustomerRole event = envelope.getEvent();
        String customerId = envelope.getAggregateId();

        logger.info("Handling CustomerEmployeeAssignedCustomerRole: employeeId={}, role={}, customerId={}",
                   event.customerEmployeeId(), event.roleName(), customerId);

        replicaService.saveCustomerEmployeeCustomerRole(
            event.customerEmployeeId().toString(),
            event.roleName(),
            customerId
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleLocationCreatedForCustomer(
            DomainEventEnvelope<LocationCreatedForCustomer> envelope) {
        LocationCreatedForCustomer event = envelope.getEvent();
        String customerId = envelope.getAggregateId();

        logger.info("Handling LocationCreatedForCustomer: locationId={}, customerId={}",
                   event.locationId(), customerId);

        replicaService.saveLocation(
            event.locationId(),
            customerId
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleTeamMemberAdded(
            DomainEventEnvelope<TeamMemberAdded> envelope) {
        TeamMemberAdded event = envelope.getEvent();

        logger.info("Handling TeamMemberAdded: teamId={}, employeeId={}",
                   event.teamId(), event.customerEmployeeId());

        replicaService.saveTeamMember(
            event.teamId().toString(),
            event.customerEmployeeId().toString()
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleTeamMemberRemoved(
            DomainEventEnvelope<TeamMemberRemoved> envelope) {
        TeamMemberRemoved event = envelope.getEvent();

        logger.info("Handling TeamMemberRemoved: teamId={}, employeeId={}",
                   event.teamId(), event.customerEmployeeId());

        replicaService.removeTeamMember(
            event.teamId().toString(),
            event.customerEmployeeId().toString()
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleTeamAssignedLocationRole(
            DomainEventEnvelope<TeamAssignedLocationRole> envelope) {
        TeamAssignedLocationRole event = envelope.getEvent();

        logger.info("Handling TeamAssignedLocationRole: teamId={}, locationId={}, role={}",
                   event.teamId(), event.locationId(), event.roleName());

        replicaService.saveTeamLocationRole(
            event.teamId().toString(),
            event.roleName(),
            event.locationId()
        );
    }

    @EventuateDomainEventHandler(
        subscriberId = "osoFactTablesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleTeamLocationRoleRemoved(
            DomainEventEnvelope<TeamLocationRoleRemoved> envelope) {
        TeamLocationRoleRemoved event = envelope.getEvent();

        logger.info("Handling TeamLocationRoleRemoved: teamId={}, locationId={}, role={}",
                   event.teamId(), event.locationId(), event.roleName());

        replicaService.removeTeamLocationRole(
            event.teamId().toString(),
            event.roleName(),
            event.locationId()
        );
    }
}
```

#### 2. Service Layer

Create: `OsoFactTablesReplicaService.java`

```java
package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OsoFactTablesReplicaService {

    private static final Logger logger = LoggerFactory.getLogger(OsoFactTablesReplicaService.class);
    private final JdbcTemplate jdbcTemplate;

    public OsoFactTablesReplicaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveCustomerEmployeeCustomerRole(String customerEmployeeId, String roleName, String customerId) {
        String sql = "INSERT INTO customer_employee_customer_roles (customer_employee_id, role_name, customer_id) " +
                     "VALUES (?, ?, ?) " +
                     "ON CONFLICT (customer_employee_id, role_name, customer_id) DO NOTHING";
        jdbcTemplate.update(sql, customerEmployeeId, roleName, customerId);
        logger.info("Saved customer employee customer role: employeeId={}, role={}, customerId={}",
                   customerEmployeeId, roleName, customerId);
    }

    public void saveLocation(Long locationId, String customerId) {
        String sql = "INSERT INTO locations (id, customer_id) " +
                     "VALUES (?, ?) " +
                     "ON CONFLICT (id) DO NOTHING";
        jdbcTemplate.update(sql, locationId, customerId);
        logger.info("Saved location: id={}, customerId={}", locationId, customerId);
    }

    public void saveTeamMember(String teamId, String customerEmployeeId) {
        String sql = "INSERT INTO team_members (team_id, customer_employee_id) " +
                     "VALUES (?, ?) " +
                     "ON CONFLICT (team_id, customer_employee_id) DO NOTHING";
        jdbcTemplate.update(sql, teamId, customerEmployeeId);
        logger.info("Saved team member: teamId={}, employeeId={}", teamId, customerEmployeeId);
    }

    public void removeTeamMember(String teamId, String customerEmployeeId) {
        String sql = "DELETE FROM team_members WHERE team_id = ? AND customer_employee_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, teamId, customerEmployeeId);
        logger.info("Removed team member: teamId={}, employeeId={}, rowsDeleted={}",
                   teamId, customerEmployeeId, rowsDeleted);
    }

    public void saveTeamLocationRole(String teamId, String roleName, Long locationId) {
        String sql = "INSERT INTO team_location_roles (team_id, role_name, location_id) " +
                     "VALUES (?, ?, ?) " +
                     "ON CONFLICT (team_id, role_name, location_id) DO NOTHING";
        jdbcTemplate.update(sql, teamId, roleName, locationId);
        logger.info("Saved team location role: teamId={}, role={}, locationId={}",
                   teamId, roleName, locationId);
    }

    public void removeTeamLocationRole(String teamId, String roleName, Long locationId) {
        String sql = "DELETE FROM team_location_roles WHERE team_id = ? AND role_name = ? AND location_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, teamId, roleName, locationId);
        logger.info("Removed team location role: teamId={}, role={}, locationId={}, rowsDeleted={}",
                   teamId, roleName, locationId, rowsDeleted);
    }
}
```

#### 3. Configuration

Update: `LocationRolesReplicaMessagingConfiguration.java`

```java
package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaMessagingCommonConfiguration;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.OsoFactTablesReplicaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LocationRolesReplicaMessagingCommonConfiguration.class)
public class LocationRolesReplicaMessagingConfiguration {

    @Bean
    public OsoFactTablesEventConsumer osoFactTablesEventConsumer(
            OsoFactTablesReplicaService replicaService) {
        return new OsoFactTablesEventConsumer(replicaService);
    }
}
```

**Note**: With annotation-based event handling using `@EventuateDomainEventHandler`, there's no need to explicitly create a `DomainEventDispatcher` bean. The Eventuate framework automatically discovers and registers event handlers annotated with `@EventuateDomainEventHandler`.

## Event Publishing Requirements (Customer Service)

### Files to Create/Modify

#### 1. Create New Event Classes

Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/`

Files to create:
- `TeamMemberAdded.java`
- `TeamMemberRemoved.java`
- `TeamAssignedLocationRole.java`
- `TeamLocationRoleRemoved.java`

#### 2. Modify CustomerService - Add Event Publishing to Existing Methods

Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerService.java`

**Existing methods that need event publishing added**:
- `addTeamMember()` - line 329: Add publishing of `TeamMemberAdded` event
- `assignTeamRole()` - line 410: Add publishing of `TeamAssignedLocationRole` event

#### 3. Extend CustomerService - Create New Methods

Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerService.java`

**New methods that need to be implemented**:
- `removeTeamMember(Long teamId, Long customerEmployeeId)`: Remove member and publish `TeamMemberRemoved` event
- `removeTeamRole(Long teamId, Long locationId, String roleName)`: Remove role and publish `TeamLocationRoleRemoved` event

**Note**: The `Team` entity class already has helper methods `removeMember()` and `removeRole()` that can be used by these new service methods.

### Event Publishing Pattern

#### Type-Safe Event Publishing

The `CustomerService` uses a type-safe `CustomerEventPublisher` that extends `DomainEventPublisherForAggregate<Customer, Long, CustomerEvent>`. This provides compile-time type safety and simplifies event publishing.

#### Modifying Existing Methods

```java
// Example: Modify CustomerService.addTeamMember() to add event publishing
public void addTeamMember(Long teamId, Long customerEmployeeId) {
    Team team = teamRepository.findRequiredById(teamId);
    CustomerEmployee employee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

    team.addMember(employee.getId());
    employee.addToTeam(teamId);

    // ADD THIS: Publish event using type-safe publisher
    Customer customer = customerRepository.findRequiredById(team.getCustomerId());
    customerEventPublisher.publish(customer,
        new TeamMemberAdded(teamId, customerEmployeeId)
    );
}
```

#### Creating New Methods

```java
// Example: New method to implement in CustomerService
public void removeTeamMember(Long teamId, Long customerEmployeeId) {
    Team team = teamRepository.findRequiredById(teamId);
    CustomerEmployee employee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

    // Use existing Team entity helper method
    team.removeMember(employee.getId());
    employee.removeFromTeam(teamId);

    // Publish event using type-safe publisher
    Customer customer = customerRepository.findRequiredById(team.getCustomerId());
    customerEventPublisher.publish(customer,
        new TeamMemberRemoved(teamId, customerEmployeeId)
    );
}

// Example: New method to implement in CustomerService
public void removeTeamRole(Long teamId, Long locationId, String roleName) {
    Team team = teamRepository.findRequiredById(teamId);

    // Find the specific role to remove
    TeamLocationRole roleToRemove = team.getRoles().stream()
        .filter(r -> r.getLocationId().equals(locationId) && r.getRoleName().equals(roleName))
        .findFirst()
        .orElseThrow(() -> new EntityNotFoundException("Team role not found"));

    // Use existing Team entity helper method
    team.removeRole(roleToRemove);
    teamLocationRoleRepository.delete(roleToRemove);

    // Publish event using type-safe publisher
    Customer customer = customerRepository.findRequiredById(team.getCustomerId());
    customerEventPublisher.publish(customer,
        new TeamLocationRoleRemoved(teamId, locationId, roleName)
    );
}
```

## Data Migration Strategy

**Not Required**: This is a demo application with no legacy data to migrate. The Oso fact tables will be populated organically as events are published during normal application usage after the implementation is complete.

## Testing Requirements

### Unit Tests

1. **Event Consumer Tests** (`OsoFactTablesEventConsumerTest.java`)
   - Mock `OsoFactTablesReplicaService`
   - Verify each event handler calls correct service method with correct parameters

2. **Service Tests** (`OsoFactTablesReplicaServiceTest.java`)
   - Use H2 in-memory database
   - Verify INSERT operations populate tables correctly
   - Verify DELETE operations remove data correctly
   - Verify ON CONFLICT clauses handle duplicates correctly

### Integration Tests

1. **End-to-End Test** (`OsoFactTablesReplicaIntegrationTest.java`)
   - Use Docker Compose to start Kafka
   - Publish events to Kafka
   - Verify events consumed and tables populated
   - Verify data matches expected state

2. **Customer Service Tests** (update existing)
   - Verify `CustomerService.addTeamMember()` publishes `TeamMemberAdded` event
   - Verify `CustomerService.assignTeamRole()` publishes `TeamAssignedLocationRole` event

## Implementation Checklist

### Phase 1: Customer Service (Event Publishing)

**Create Event Classes:**
- [ ] Create `TeamMemberAdded.java` event class
- [ ] Create `TeamMemberRemoved.java` event class
- [ ] Create `TeamAssignedLocationRole.java` event class
- [ ] Create `TeamLocationRoleRemoved.java` event class

**Modify Existing Methods (Add Event Publishing):**
- [ ] Modify `CustomerService.addTeamMember()` (line 329) to publish `TeamMemberAdded` event
- [ ] Modify `CustomerService.assignTeamRole()` (line 410) to publish `TeamAssignedLocationRole` event

**Implement New Methods (Business Logic + Event Publishing):**
- [ ] Implement `CustomerService.removeTeamMember(Long teamId, Long customerEmployeeId)`
  - Use `Team.removeMember()` helper method
  - Publish `TeamMemberRemoved` event
- [ ] Implement `CustomerService.removeTeamRole(Long teamId, Long locationId, String roleName)`
  - Use `Team.removeRole()` helper method
  - Publish `TeamLocationRoleRemoved` event

**Testing:**
- [ ] Write unit tests for modified methods (event publishing)
- [ ] Write unit tests for new methods (business logic + event publishing)
- [ ] Write integration tests to verify events published to Kafka

### Phase 2: Security System Service (Event Consumption)

- [ ] Copy event class definitions to Security System Service (same package)
- [ ] Create `OsoFactTablesEventConsumer.java`
- [ ] Create `OsoFactTablesReplicaService.java`
- [ ] Update `LocationRolesReplicaConfiguration.java`
- [ ] Write unit tests for event consumers
- [ ] Write unit tests for replica service
- [ ] Write integration tests for end-to-end event flow

### Phase 3: Testing & Validation

- [ ] Run all unit tests
- [ ] Run all integration tests
- [ ] Verify tables populated correctly in test environment
- [ ] Test authorization scenarios using Oso with populated facts

## Notes

### Business Logic Implementation Status

**Important**: The `Team` entity class has helper methods for removing members and roles:
- `Team.removeMember(Long employeeId)` - line 103
- `Team.removeRole(TeamLocationRole role)` - line 86

However, these are **entity-level methods only** and:
- Are NOT exposed through `CustomerService` (the domain service)
- Do NOT publish domain events
- Are NOT accessible via REST API
- Cannot be used directly without implementing service-level methods

To support removal operations, new public methods must be added to `CustomerService` that use these entity helpers and publish appropriate events.

### Event Class Duplication

Event classes must be copied to the Security System Service:
- **Customer Service package**: `io.eventuate.examples.realguardio.customerservice.customermanagement.domain` (source)
- **Security System Service package**: `io.eventuate.examples.realguardio.customerservice.domain` (shorter package for event copies)

This ensures proper serialization/deserialization by Eventuate Tram. Events implement the `CustomerEvent` marker interface which extends `DomainEvent`.

### Idempotency

All INSERT operations use `ON CONFLICT ... DO NOTHING` to ensure idempotency. This handles:
- Duplicate events
- Event replay during recovery
- Multiple subscribers

### Eventual Consistency

The replica tables are eventually consistent with the source data:
- Small lag expected (typically milliseconds to seconds)
- Authorization decisions use replica data (fast reads)
- Command operations use source data (Customer Service)

### Aggregate Type Selection

All events use the fully qualified aggregate type `"io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"` with the Customer ID (Long) as the aggregate ID. This approach:
- Maintains consistency with existing events (`CustomerEmployeeAssignedCustomerRole`, `LocationCreatedForCustomer`)
- Uses the fully qualified class name as required by newer Eventuate Tram versions
- Keeps all customer-related events under a single aggregate stream
- Simplifies event handler configuration (single subscriber ID for all events)
- Supports type-safe event publishing through `CustomerEventPublisher`

An alternative approach would be to use `"Team"` aggregate type for team-related events with Team ID as the aggregate ID, but this would require separate event handler configuration and more complex subscriber setup.

## References

- Existing implementation: `CustomerEmployeeLocationEventConsumer.java`
- Existing events: `CustomerEmployeeAssignedLocationRole.java`, `LocationCreatedForCustomer.java`
- Eventuate Tram documentation: https://eventuate.io/docs/manual/eventuate-tram/latest/
- Oso authorization: https://www.osohq.com/docs
