# Oso Fact Tables - Implementation Plan

## Overview

This plan implements event publishing and consumption to populate the Oso fact tables defined in `V10002__oso__fact_tables.sql`.

**Scope**: This plan covers events where business logic already exists or events are already published:

**Events requiring publishing implementation:**
- `TeamMemberAdded` - business logic exists in `CustomerService.addTeamMember()`
- `TeamAssignedLocationRole` - business logic exists in `CustomerService.assignTeamRole()`

**Events already published (consumption only):**
- `LocationCreatedForCustomer` - already published by `CustomerService.createLocation()`

**Events NOT covered** (require new business logic):
- `CustomerEmployeeAssignedCustomerRole` - already published but consumption not yet implemented
- `TeamMemberRemoved` - requires new `CustomerService.removeTeamMember()` method
- `TeamLocationRoleRemoved` - requires new `CustomerService.removeTeamRole()` method

**Approach**: For each event:
1. Implement event publishing in Customer Service (with tests) and commit
2. Implement event consumption in Security System Service (with tests) and commit

**Test Strategy**: Following TDD with improved test architecture:
- **Unit Tests**: Mock dependencies using `@MockitoBean`, verify behavior with in-memory event bus (`TramInMemoryConfiguration`)
- **Integration Tests**: Use real database with TestContainers (Kafka + PostgreSQL), verify end-to-end event flow
- **Event Publishing**: Type-safe `CustomerEventPublisher` extends `DomainEventPublisherForAggregate`
- **Event Handling**: Annotation-based with `@EventuateDomainEventHandler`

---

## Implementation Architecture Options

There are two approaches for implementing event consumption in the Security System Service:

### Option A: Enhance Existing Consumer (Recommended)

Enhance the existing `CustomerEmployeeLocationEventConsumer` and `LocationRolesReplicaService` to handle all Customer events:

**Pros:**
- Single event consumer for all Customer aggregate events
- Consistent with existing architecture pattern
- Simpler configuration (one subscriber ID)
- Easier to maintain

**Implementation:**
1. Rename `CustomerEmployeeLocationEventConsumer` → `CustomerEventConsumer` (or keep existing name)
2. Add new event handler methods for each event type using `@EventuateDomainEventHandler`
3. Enhance `LocationRolesReplicaService` with methods for team members, team location roles, customer roles, and locations
4. All handlers use same `subscriberId` and `channel`

### Option B: Create Separate Consumer

Create a new `OsoFactTablesEventConsumer` alongside the existing consumer:

**Pros:**
- Clear separation between location roles replica and Oso fact tables
- Can evolve independently

**Cons:**
- Two consumers listening to same aggregate type with different subscriber IDs
- More configuration and maintenance
- Potential confusion about where to add new handlers

### Recommended Approach

**Use Option A**: Enhance the existing `CustomerEmployeeLocationEventConsumer` and `LocationRolesReplicaService`. This follows the established pattern and keeps all Customer event handling in one place.

**Steps:**
1. Add handler methods to `CustomerEmployeeLocationEventConsumer` for each new event type
2. Add service methods to `LocationRolesReplicaService` for the new tables (`team_members`, `team_location_roles`, `customer_employee_customer_roles`, `locations`)
3. Update tests to verify all event types are handled correctly

**Implementation Note:** This plan enhances the existing `CustomerEmployeeLocationEventConsumer` and `LocationRolesReplicaService` classes rather than creating new ones. All new event handlers and service methods are added to these existing classes.

### Example: Enhancing Existing Consumer

Here's how to add a new event handler to the existing `CustomerEmployeeLocationEventConsumer`:

**Current State:**
```java
public class CustomerEmployeeLocationEventConsumer {

    private final LocationRolesReplicaService replicaService;

    @EventuateDomainEventHandler(
        subscriberId = "locationRolesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleCustomerEmployeeAssignedLocationRole(
            DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        // existing implementation
    }
}
```

**Enhanced to Handle TeamMemberAdded:**
```java
public class CustomerEmployeeLocationEventConsumer {

    private final LocationRolesReplicaService replicaService;

    @EventuateDomainEventHandler(
        subscriberId = "locationRolesReplicaDispatcher",
        channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
    )
    public void handleCustomerEmployeeAssignedLocationRole(
            DomainEventEnvelope<CustomerEmployeeAssignedLocationRole> envelope) {
        // existing implementation
    }

    // NEW: Add handler for TeamMemberAdded
    @EventuateDomainEventHandler(
        subscriberId = "locationRolesReplicaDispatcher",
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
}
```

**Key Points:**
- Same `subscriberId` for all handlers ensures they're part of the same consumer group
- Same `channel` since all events come from Customer aggregate
- Add corresponding service methods to `LocationRolesReplicaService`

---

## Event 1: TeamMemberAdded

### Phase 1.1: Publish TeamMemberAdded Event (Customer Service)

#### Task 1.1.1: Write Unit Test FIRST (TDD - Won't Compile)

- [ ] Create test method in `CustomerServiceTest.java`
  - Location: `realguardio-customer-service/customer-service-domain/src/test/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerServiceTest.java`
  - Test name: `shouldPublishTeamMemberAddedEventWhenAddingTeamMember()`
  - Pattern: Mock `DomainEventPublisher`, call `addTeamMember()`, verify event published
  - **Expected**: Test WON'T COMPILE (TeamMemberAdded doesn't exist yet)

```java
@Test
public void shouldPublishTeamMemberAddedEventWhenAddingTeamMember() {
    // Given
    var customer = testCustomerFactory.createCustomer();
    var team = customer.createTeam("Operations Team");
    var employee = customer.createCustomerEmployee();

    Mockito.clearInvocations(customerEventPublisher);

    // When
    customer.addTeamMember(team.getId(), employee.getId());

    // Then
    TeamMemberAdded expectedEvent = new TeamMemberAdded(
        team.getId(),
        employee.getId()
    );

    verify(customerEventPublisher).publish(
        eq(customer),
        eq(expectedEvent)
    );
}
```

- [ ] Try to compile and verify it FAILS
  - Command: `./gradlew :customer-service-domain:compileTestJava`
  - Expected: Compilation fails - "cannot find symbol: class TeamMemberAdded"

#### Task 1.1.2: Create TeamMemberAdded Event Class (Make Test Compile)

- [ ] Create `TeamMemberAdded.java` event record class
  - Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/TeamMemberAdded.java`
  - Event fields: `Long teamId`, `Long customerEmployeeId`
  - Implements `CustomerEvent` (marker interface)

```java
package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamMemberAdded(
    Long teamId,
    Long customerEmployeeId
) implements CustomerEvent {
}
```

- [ ] Verify test now compiles
  - Command: `./gradlew :customer-service-domain:compileTestJava`
  - Expected: Compilation succeeds

#### Task 1.1.3: Run Test and Verify it FAILS (TDD - Red)

- [ ] Run the test and verify it FAILS
  - Command: `./gradlew :customer-service-domain:test --tests CustomerServiceTest.shouldPublishTeamMemberAddedEventWhenAddingTeamMember`
  - Expected: Test FAILS because event is not published (verification fails)

#### Task 1.1.4: Modify CustomerService to Publish Event (TDD - Green)

- [ ] Modify `CustomerService.addTeamMember()` to publish `TeamMemberAdded` event
  - Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerService.java`
  - Add event publishing after business logic
  - Use type-safe `customerEventPublisher.publish(customer, event)`

```java
public void addTeamMember(Long teamId, Long customerEmployeeId) {
    Team team = teamRepository.findRequiredById(teamId);
    CustomerEmployee employee = customerEmployeeRepository.findRequiredById(customerEmployeeId);

    team.addMember(employee.getId());
    employee.addToTeam(teamId);

    // Publish event using type-safe publisher
    Customer customer = customerRepository.findRequiredById(team.getCustomerId());
    customerEventPublisher.publish(customer,
        new TeamMemberAdded(teamId, customerEmployeeId)
    );
}
```

- [ ] Run the test and verify it PASSES
  - Command: `./gradlew :customer-service-domain:test --tests CustomerServiceTest.shouldPublishTeamMemberAddedEventWhenAddingTeamMember`
  - Expected: Test passes

#### Task 1.1.5: Run All Customer Service Tests

- [ ] Run all tests in customer-service-domain
  - Command: `./gradlew :customer-service-domain:test`
  - Expected: All tests pass

- [ ] Run full Customer Service build
  - Command: `cd realguardio-customer-service && ./gradlew check`
  - Expected: Build succeeds

#### Task 1.1.6: Commit Event Publishing Changes

- [ ] Commit the changes
  - Files: `TeamMemberAdded.java`, `CustomerService.java`, `CustomerServiceTest.java`
  - Message: "Add TeamMemberAdded event publishing to CustomerService.addTeamMember()"

---

### Phase 1.2: Consume TeamMemberAdded Event (Security System Service)

#### Task 1.2.1: Write Unit Test FIRST (TDD - Won't Compile)

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/test/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/common/CustomerEmployeeLocationEventConsumerTest.java`
  - Use `@MockitoBean` for `LocationRolesReplicaService`
  - Use `TramInMemoryConfiguration` for in-memory event bus
  - Test method: `shouldHandleTeamMemberAdded()`
  - **Expected**: Test WON'T COMPILE (TeamMemberAdded doesn't exist yet)

```java
@Test
public void shouldHandleTeamMemberAdded() {
    // Given
    Long teamId = 123L;
    Long employeeId = 456L;
    String customerId = "customer-1";

    TeamMemberAdded event = new TeamMemberAdded(teamId, employeeId);

    // When - publish with fully qualified aggregate type
    domainEventPublisher.publish(
        "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
        customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        verify(locationRolesReplicaService).saveTeamMember(
            teamId.toString(),
            employeeId.toString()
        );
    });
}
```

- [ ] Try to compile and verify it FAILS
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation fails - missing classes

#### Task 1.2.2: Copy Event Class to Security System Service (Make Test Compile - Part 1)

- [ ] Copy `TeamMemberAdded.java` to Security System Service in shorter package
  - From: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/TeamMemberAdded.java`
  - To: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/customerservice/domain/TeamMemberAdded.java`
  - **Important**: Event copy uses shorter package `...customerservice.domain` (not `...customermanagement.domain`) for deserialization
  - Event must implement `CustomerEvent` marker interface

#### Task 1.2.3: Add Service Method to LocationRolesReplicaService (Make Test Compile - Part 2)

- [ ] Add `saveTeamMember()` method to `LocationRolesReplicaService.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/common/LocationRolesReplicaService.java`
  - Implement `saveTeamMember(String teamId, String customerEmployeeId)` method
  - Use existing `JdbcTemplate` to insert into `team_members` table

```java
public void saveTeamMember(String teamId, String customerEmployeeId) {
    String sql = "INSERT INTO team_members (team_id, customer_employee_id) " +
                 "VALUES (?, ?) " +
                 "ON CONFLICT (team_id, customer_employee_id) DO NOTHING";
    jdbcTemplate.update(sql, teamId, customerEmployeeId);
    logger.info("Saved team member: teamId={}, employeeId={}", teamId, customerEmployeeId);
}
```

- [ ] Verify test now compiles
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation succeeds (but test configuration still incomplete)

#### Task 1.2.4: Add Event Handler to CustomerEmployeeLocationEventConsumer (Make Test Compile - Part 3)

- [ ] Add `handleTeamMemberAdded()` method to `CustomerEmployeeLocationEventConsumer.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/messaging/CustomerEmployeeLocationEventConsumer.java`
  - Use annotation-based event handling with `@EventuateDomainEventHandler`
  - Use same `subscriberId = "locationRolesReplicaDispatcher"` as existing handler
  - Implement handler method stub (doesn't call service yet)

```java
@EventuateDomainEventHandler(
    subscriberId = "locationRolesReplicaDispatcher",
    channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
)
public void handleTeamMemberAdded(DomainEventEnvelope<TeamMemberAdded> envelope) {
    TeamMemberAdded event = envelope.getEvent();
    logger.info("Handling TeamMemberAdded: teamId={}, employeeId={}",
               event.teamId(), event.customerEmployeeId());

    // TODO: Call service - stub for now
}
```

#### Task 1.2.5: Run Test and Verify it FAILS (TDD - Red)

- [ ] Run the unit test and verify it FAILS
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleTeamMemberAdded`
  - Expected: Test FAILS - service method never called (verification fails)

#### Task 1.2.6: Implement Event Handler to Call Service (TDD - Green)

- [ ] Update `handleTeamMemberAdded()` in `CustomerEmployeeLocationEventConsumer.java` to call service

```java
private void handleTeamMemberAdded(DomainEventEnvelope<TeamMemberAdded> envelope) {
    TeamMemberAdded event = envelope.getEvent();
    logger.info("Handling TeamMemberAdded: teamId={}, employeeId={}",
               event.teamId(), event.customerEmployeeId());

    replicaService.saveTeamMember(
        event.teamId().toString(),
        event.customerEmployeeId().toString()
    );
}
```

- [ ] Run the unit test and verify it PASSES
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleTeamMemberAdded`
  - Expected: Test passes

#### Task 1.2.7: Write Integration Test for Database Updates (TDD)

- [ ] Add test methods to `LocationRolesReplicaServiceTest.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/test/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/common/LocationRolesReplicaServiceTest.java`
  - Use `@SpringBootTest` with real database
  - Flyway migrations run automatically
  - Test method: `shouldSaveTeamMemberToDatabase()`

```java
// Add these test methods to existing LocationRolesReplicaServiceTest class

    @Test
    public void shouldSaveTeamMemberToDatabase() {
        // Given
        String teamId = "team-123";
        String employeeId = "employee-456";

        // When
        service.saveTeamMember(teamId, employeeId);

        // Then
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM team_members WHERE team_id = ? AND customer_employee_id = ?",
            Integer.class,
            teamId, employeeId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void shouldHandleDuplicateTeamMemberInserts() {
        // Given
        String teamId = "team-789";
        String employeeId = "employee-101";

        // When
        service.saveTeamMember(teamId, employeeId);
        service.saveTeamMember(teamId, employeeId); // Duplicate

        // Then
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM team_members WHERE team_id = ? AND customer_employee_id = ?",
            Integer.class,
            teamId, employeeId
        );
        assertThat(count).isEqualTo(1); // Only one row
    }
}
```

- [ ] Run integration tests
  - Command: `./gradlew :location-roles-replica:test --tests LocationRolesReplicaServiceTest`
  - Expected: Tests pass

#### Task 1.2.8: Write End-to-End Integration Test

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`
  - Test name: `shouldConsumeTeamMemberAddedEventAndUpdateDatabase()`
  - Publish event and verify database was updated
  - Flyway migrations run automatically

```java
@Autowired
private JdbcTemplate jdbcTemplate;

@Test
public void shouldConsumeTeamMemberAddedEventAndUpdateDatabase() {
    // Given
    Long teamId = 999L;
    Long employeeId = 888L;
    String customerId = "customer-999";

    TeamMemberAdded event = new TeamMemberAdded(teamId, employeeId);

    // When
    domainEventPublisher.publish("Customer", customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM team_members WHERE team_id = ? AND customer_employee_id = ?",
            Integer.class,
            teamId.toString(), employeeId.toString()
        );
        assertThat(count).isEqualTo(1);
    });
}
```

- [ ] Run the test
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldConsumeTeamMemberAddedEventAndUpdateDatabase`
  - Expected: Test passes

#### Task 1.2.9: Run All Security System Service Tests

- [ ] Run all tests in location-roles-replica
  - Command: `./gradlew :location-roles-replica:test`
  - Expected: All tests pass

- [ ] Run full Security System Service build
  - Command: `cd realguardio-security-system-service && ./gradlew check`
  - Expected: Build succeeds

#### Task 1.2.10: Run End-to-End Tests

- [ ] Run end-to-end tests to verify event flow works across services
  - Command: `./gradlew -p end-to-end-tests check`
  - Expected: All checks pass (includes unit tests, integration tests, and other validations)
  - **Important**: This verifies that Customer Service publishes events and Security System Service consumes them correctly

#### Task 1.2.11: Commit Event Consumption Changes

- [ ] Commit the changes
  - Files: `TeamMemberAdded.java` (copy), `CustomerEmployeeLocationEventConsumer.java` (enhanced), `LocationRolesReplicaService.java` (enhanced), test files
  - Message: "Add TeamMemberAdded event consumption to populate team_members table"

---

## Event 2: TeamAssignedLocationRole

**Note**: This event follows the same patterns as Event 1 (TeamMemberAdded):
- Event class implements `CustomerEvent` marker interface
- Publishing uses type-safe `customerEventPublisher.publish(customer, event)`
- Event handling uses `@EventuateDomainEventHandler` annotation
- Tests use `@MockitoBean` and `TramInMemoryConfiguration`
- Event copies to Security Service use shorter package `...customerservice.domain`

### Phase 2.1: Publish TeamAssignedLocationRole Event (Customer Service)

#### Task 2.1.1: Write Unit Test FIRST (TDD - Won't Compile)

- [ ] Add test method to `CustomerServiceTest.java`
  - Test name: `shouldPublishTeamAssignedLocationRoleEventWhenAssigningTeamRole()`
  - Pattern: Mock `DomainEventPublisher`, call `assignTeamRole()`, verify event published
  - **Expected**: Test WON'T COMPILE (TeamAssignedLocationRole doesn't exist yet)

```java
@Test
public void shouldPublishTeamAssignedLocationRoleEventWhenAssigningTeamRole() {
    // Given
    var customer = testCustomerFactory.createCustomer();
    var team = customer.createTeam("Security Team");
    var location = customer.createLocation();

    Mockito.clearInvocations(customerEventPublisher);

    // When
    customer.assignTeamRole(team.getId(), location.getId(), SECURITY_SYSTEM_DISARMER_ROLE);

    // Then
    TeamAssignedLocationRole expectedEvent = new TeamAssignedLocationRole(
        team.getId(),
        location.getId(),
        SECURITY_SYSTEM_DISARMER_ROLE
    );

    verify(customerEventPublisher).publish(
        eq(customer),
        eq(expectedEvent)
    );
}
```

- [ ] Try to compile and verify it FAILS
  - Command: `./gradlew :customer-service-domain:compileTestJava`
  - Expected: Compilation fails - "cannot find symbol: class TeamAssignedLocationRole"

#### Task 2.1.2: Create TeamAssignedLocationRole Event Class (Make Test Compile)

- [ ] Create `TeamAssignedLocationRole.java` event record class
  - Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/TeamAssignedLocationRole.java`
  - Event fields: `Long teamId`, `Long locationId`, `String roleName`
  - Implements `CustomerEvent` (marker interface)

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

- [ ] Verify test now compiles
  - Command: `./gradlew :customer-service-domain:compileTestJava`
  - Expected: Compilation succeeds

#### Task 2.1.3: Run Test and Verify it FAILS (TDD - Red)

- [ ] Run the test and verify it FAILS
  - Command: `./gradlew :customer-service-domain:test --tests CustomerServiceTest.shouldPublishTeamAssignedLocationRoleEventWhenAssigningTeamRole`
  - Expected: Test FAILS because event is not published (verification fails)

#### Task 2.1.4: Modify CustomerService to Publish Event (TDD - Green)

- [ ] Modify `CustomerService.assignTeamRole()` to publish `TeamAssignedLocationRole` event
  - Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerService.java`
  - Add event publishing after business logic using type-safe publisher

```java
public void assignTeamRole(Long teamId, Long locationId, String roleName) {
    Team team = teamRepository.findRequiredById(teamId);

    TeamLocationRole role = new TeamLocationRole();
    role.setLocationId(locationId);
    role.setRoleName(roleName);
    team.addRole(role);

    teamLocationRoleRepository.save(role);

    // Publish event using type-safe publisher
    Customer customer = customerRepository.findRequiredById(team.getCustomerId());
    customerEventPublisher.publish(customer,
        new TeamAssignedLocationRole(teamId, locationId, roleName)
    );
}
```

- [ ] Run the test and verify it PASSES
  - Command: `./gradlew :customer-service-domain:test --tests CustomerServiceTest.shouldPublishTeamAssignedLocationRoleEventWhenAssigningTeamRole`
  - Expected: Test passes

#### Task 2.1.5: Run All Customer Service Tests

- [ ] Run all tests in customer-service-domain
  - Command: `./gradlew :customer-service-domain:test`
  - Expected: All tests pass

- [ ] Run full Customer Service build
  - Command: `cd realguardio-customer-service && ./gradlew check`
  - Expected: Build succeeds

#### Task 2.1.6: Commit Event Publishing Changes

- [ ] Commit the changes
  - Files: `TeamAssignedLocationRole.java`, `CustomerService.java`, `CustomerServiceTest.java`
  - Message: "Add TeamAssignedLocationRole event publishing to CustomerService.assignTeamRole()"

---

### Phase 2.2: Consume TeamAssignedLocationRole Event (Security System Service)

#### Task 2.2.1: Write Unit Test FIRST (TDD - Won't Compile)

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`
  - Test name: `shouldHandleTeamAssignedLocationRole()`
  - **Expected**: Test WON'T COMPILE (TeamAssignedLocationRole doesn't exist yet)

```java
@Test
public void shouldHandleTeamAssignedLocationRole() {
    // Given
    Long teamId = 111L;
    Long locationId = 222L;
    String roleName = "SECURITY_SYSTEM_DISARMER";
    String customerId = "customer-2";

    TeamAssignedLocationRole event = new TeamAssignedLocationRole(teamId, locationId, roleName);

    // When
    domainEventPublisher.publish(
        "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
        customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        verify(locationRolesReplicaService).saveTeamLocationRole(
            teamId.toString(),
            roleName,
            locationId
        );
    });
}
```

- [ ] Try to compile and verify it FAILS
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation fails - missing TeamAssignedLocationRole class

#### Task 2.2.2: Copy Event Class to Security System Service (Make Test Compile)

- [ ] Copy `TeamAssignedLocationRole.java` to Security System Service
  - From: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/TeamAssignedLocationRole.java`
  - To: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/TeamAssignedLocationRole.java`
  - **Important**: Must be in exact same package for deserialization

- [ ] Verify test now compiles
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation succeeds

#### Task 2.2.3: Run Test and Verify it FAILS (TDD - Red)

- [ ] Run the test and verify it FAILS
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleTeamAssignedLocationRole`
  - Expected: Test FAILS - service method doesn't exist or handler not implemented

#### Task 2.2.4: Add Service Method (TDD - Green)

- [ ] Add `saveTeamLocationRole()` method to `LocationRolesReplicaService.java`

```java
public void saveTeamLocationRole(String teamId, String roleName, Long locationId) {
    String sql = "INSERT INTO team_location_roles (team_id, role_name, location_id) " +
                 "VALUES (?, ?, ?) " +
                 "ON CONFLICT (team_id, role_name, location_id) DO NOTHING";
    jdbcTemplate.update(sql, teamId, roleName, locationId);
    logger.info("Saved team location role: teamId={}, role={}, locationId={}",
               teamId, roleName, locationId);
}
```

#### Task 2.2.5: Add Event Handler (TDD - Green)

- [ ] Add handler method to `CustomerEmployeeLocationEventConsumer.java`
  - Add new `@EventuateDomainEventHandler` method for `TeamAssignedLocationRole`
  - Use same `subscriberId` as other handlers

```java
@EventuateDomainEventHandler(
    subscriberId = "locationRolesReplicaDispatcher",
    channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
)
public void handleTeamAssignedLocationRole(DomainEventEnvelope<TeamAssignedLocationRole> envelope) {
    TeamAssignedLocationRole event = envelope.getEvent();
    logger.info("Handling TeamAssignedLocationRole: teamId={}, locationId={}, role={}",
               event.teamId(), event.locationId(), event.roleName());

    replicaService.saveTeamLocationRole(
        event.teamId().toString(),
        event.roleName(),
        event.locationId()
    );
}
```

- [ ] Run the unit test and verify it PASSES
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleTeamAssignedLocationRole`
  - Expected: Test passes

#### Task 2.2.6: Write Integration Test for Database Updates

- [ ] Add test methods to `LocationRolesReplicaServiceTest.java`

```java
@Test
public void shouldSaveTeamLocationRoleToDatabase() {
    // Given
    String teamId = "team-200";
    String roleName = "SECURITY_SYSTEM_ARMER";
    Long locationId = 300L;

    // When
    service.saveTeamLocationRole(teamId, roleName, locationId);

    // Then
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM team_location_roles WHERE team_id = ? AND role_name = ? AND location_id = ?",
        Integer.class,
        teamId, roleName, locationId
    );
    assertThat(count).isEqualTo(1);
}

@Test
public void shouldHandleDuplicateTeamLocationRoleInserts() {
    // Given
    String teamId = "team-400";
    String roleName = "SECURITY_SYSTEM_DISARMER";
    Long locationId = 500L;

    // When
    service.saveTeamLocationRole(teamId, roleName, locationId);
    service.saveTeamLocationRole(teamId, roleName, locationId); // Duplicate

    // Then
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM team_location_roles WHERE team_id = ? AND role_name = ? AND location_id = ?",
        Integer.class,
        teamId, roleName, locationId
    );
    assertThat(count).isEqualTo(1); // Only one row
}
```

- [ ] Run integration tests
  - Command: `./gradlew :location-roles-replica:test --tests LocationRolesReplicaServiceTest`
  - Expected: Tests pass

#### Task 2.2.7: Write End-to-End Integration Test

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`
  - Flyway migrations run automatically

```java
@Test
public void shouldConsumeTeamAssignedLocationRoleEventAndUpdateDatabase() {
    // Given
    Long teamId = 777L;
    Long locationId = 666L;
    String roleName = "SECURITY_SYSTEM_ARMER";
    String customerId = "customer-777";

    TeamAssignedLocationRole event = new TeamAssignedLocationRole(teamId, locationId, roleName);

    // When
    domainEventPublisher.publish("Customer", customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM team_location_roles WHERE team_id = ? AND role_name = ? AND location_id = ?",
            Integer.class,
            teamId.toString(), roleName, locationId
        );
        assertThat(count).isEqualTo(1);
    });
}
```

- [ ] Run the test
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldConsumeTeamAssignedLocationRoleEventAndUpdateDatabase`
  - Expected: Test passes

#### Task 2.2.8: Run All Security System Service Tests

- [ ] Run all tests in location-roles-replica
  - Command: `./gradlew :location-roles-replica:test`
  - Expected: All tests pass

- [ ] Run full Security System Service build
  - Command: `cd realguardio-security-system-service && ./gradlew check`
  - Expected: Build succeeds

#### Task 2.2.9: Run End-to-End Tests

- [ ] Run end-to-end tests to verify event flow works across services
  - Command: `./gradlew -p end-to-end-tests check`
  - Expected: All checks pass (includes unit tests, integration tests, and other validations)
  - **Important**: This verifies that Customer Service publishes events and Security System Service consumes them correctly

#### Task 2.2.10: Commit Event Consumption Changes

- [ ] Commit the changes
  - Files: `TeamAssignedLocationRole.java` (copy), `CustomerEmployeeLocationEventConsumer.java` (enhanced), `LocationRolesReplicaService.java` (enhanced), test files
  - Message: "Add TeamAssignedLocationRole event consumption to populate team_location_roles table"

---

## Event 3: LocationCreatedForCustomer

**Note**: This event is already published by Customer Service, so only consumption needs to be implemented.

### Phase 3.1: Consume LocationCreatedForCustomer Event (Security System Service)

#### Task 3.1.1: Write Unit Test FIRST (TDD - Won't Compile)

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`
  - Test name: `shouldHandleLocationCreatedForCustomer()`
  - **Expected**: Test WON'T COMPILE (LocationCreatedForCustomer doesn't exist in Security Service yet)

```java
@Test
public void shouldHandleLocationCreatedForCustomer() {
    // Given
    Long locationId = 999L;
    String customerId = "customer-3";

    LocationCreatedForCustomer event = new LocationCreatedForCustomer(locationId);

    // When
    domainEventPublisher.publish(
        "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
        customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        verify(locationRolesReplicaService).saveLocation(
            locationId,
            customerId
        );
    });
}
```

- [ ] Try to compile and verify it FAILS
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation fails - missing LocationCreatedForCustomer class

#### Task 3.1.2: Copy Event Class to Security System Service (Make Test Compile - Part 1)

- [ ] Copy `LocationCreatedForCustomer.java` to Security System Service in shorter package
  - From: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/LocationCreatedForCustomer.java`
  - To: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/customerservice/domain/LocationCreatedForCustomer.java`
  - **Important**: Event copy uses shorter package `...customerservice.domain` for deserialization
  - Event must implement `CustomerEvent` marker interface

- [ ] Verify test now compiles
  - Command: `./gradlew :location-roles-replica:compileTestJava`
  - Expected: Compilation succeeds (but test will still fail)

#### Task 3.1.3: Add Service Method to LocationRolesReplicaService (Make Test Compile - Part 2)

- [ ] Add `saveLocation()` method to `LocationRolesReplicaService.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/common/LocationRolesReplicaService.java`
  - Implement `saveLocation(Long locationId, String customerId)` method
  - Use existing `JdbcTemplate` to insert into `locations` table

```java
public void saveLocation(Long locationId, String customerId) {
    String sql = "INSERT INTO locations (id, customer_id) " +
                 "VALUES (?, ?) " +
                 "ON CONFLICT (id) DO NOTHING";
    jdbcTemplate.update(sql, locationId, customerId);
    logger.info("Saved location: id={}, customerId={}", locationId, customerId);
}
```

#### Task 3.1.4: Add Event Handler to CustomerEmployeeLocationEventConsumer (Make Test Compile - Part 3)

- [ ] Add `handleLocationCreatedForCustomer()` method to `CustomerEmployeeLocationEventConsumer.java`
  - Location: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/messaging/CustomerEmployeeLocationEventConsumer.java`
  - Use annotation-based event handling with `@EventuateDomainEventHandler`
  - Use same `subscriberId = "locationRolesReplicaDispatcher"` as existing handler
  - Implement handler method stub (doesn't call service yet)

```java
@EventuateDomainEventHandler(
    subscriberId = "locationRolesReplicaDispatcher",
    channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
)
public void handleLocationCreatedForCustomer(DomainEventEnvelope<LocationCreatedForCustomer> envelope) {
    LocationCreatedForCustomer event = envelope.getEvent();
    String customerId = envelope.getAggregateId();

    logger.info("Handling LocationCreatedForCustomer: locationId={}, customerId={}",
               event.locationId(), customerId);

    // TODO: Call service - stub for now
}
```

#### Task 3.1.5: Run Test and Verify it FAILS (TDD - Red)

- [ ] Run the unit test and verify it FAILS
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleLocationCreatedForCustomer`
  - Expected: Test FAILS - service method never called (verification fails)

#### Task 3.1.6: Implement Event Handler to Call Service (TDD - Green)

- [ ] Update `handleLocationCreatedForCustomer()` in `CustomerEmployeeLocationEventConsumer.java` to call service

```java
@EventuateDomainEventHandler(
    subscriberId = "locationRolesReplicaDispatcher",
    channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
)
public void handleLocationCreatedForCustomer(DomainEventEnvelope<LocationCreatedForCustomer> envelope) {
    LocationCreatedForCustomer event = envelope.getEvent();
    String customerId = envelope.getAggregateId();

    logger.info("Handling LocationCreatedForCustomer: locationId={}, customerId={}",
               event.locationId(), customerId);

    replicaService.saveLocation(
        event.locationId(),
        customerId
    );
}
```

- [ ] Run the unit test and verify it PASSES
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldHandleLocationCreatedForCustomer`
  - Expected: Test passes

#### Task 3.1.6: Add Integration Test

- [ ] Add test method to `LocationRolesReplicaServiceTest.java`

```java
@Test
public void shouldSaveLocationToDatabase() {
    // Given
    Long locationId = 888L;
    String customerId = "customer-888";

    // When
    service.saveLocation(locationId, customerId);

    // Then
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM locations WHERE id = ? AND customer_id = ?",
        Integer.class,
        locationId, customerId
    );
    assertThat(count).isEqualTo(1);
}

@Test
public void shouldHandleDuplicateLocationInserts() {
    // Given
    Long locationId = 777L;
    String customerId = "customer-777";

    // When
    service.saveLocation(locationId, customerId);
    service.saveLocation(locationId, customerId); // Duplicate

    // Then
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM locations WHERE id = ? AND customer_id = ?",
        Integer.class,
        locationId, customerId
    );
    assertThat(count).isEqualTo(1); // Only one row
}
```

- [ ] Run integration tests
  - Command: `./gradlew :location-roles-replica:test --tests LocationRolesReplicaServiceTest`
  - Expected: Tests pass

#### Task 3.1.7: Add End-to-End Integration Test

- [ ] Add test method to `CustomerEmployeeLocationEventConsumerTest.java`

```java
@Test
public void shouldConsumeLocationCreatedForCustomerEventAndUpdateDatabase() {
    // Given
    Long locationId = 555L;
    String customerId = "customer-555";

    LocationCreatedForCustomer event = new LocationCreatedForCustomer(locationId);

    // When
    domainEventPublisher.publish(
        "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
        customerId,
        Collections.singletonList(event));

    // Then
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM locations WHERE id = ? AND customer_id = ?",
            Integer.class,
            locationId, customerId
        );
        assertThat(count).isEqualTo(1);
    });
}
```

- [ ] Run the test
  - Command: `./gradlew :location-roles-replica:test --tests CustomerEmployeeLocationEventConsumerTest.shouldConsumeLocationCreatedForCustomerEventAndUpdateDatabase`
  - Expected: Test passes

#### Task 3.1.8: Run All Security System Service Tests

- [ ] Run all tests in location-roles-replica
  - Command: `./gradlew :location-roles-replica:test`
  - Expected: All tests pass

- [ ] Run full Security System Service build
  - Command: `cd realguardio-security-system-service && ./gradlew check`
  - Expected: Build succeeds

#### Task 3.1.9: Run End-to-End Tests

- [ ] Run end-to-end tests to verify event flow works across services
  - Command: `./gradlew -p end-to-end-tests check`
  - Expected: All checks pass (includes unit tests, integration tests, and other validations)
  - **Important**: This verifies that Customer Service publishes events and Security System Service consumes them correctly

#### Task 3.1.10: Commit Event Consumption Changes

- [ ] Commit the changes
  - Files: `LocationCreatedForCustomer.java` (copy), `CustomerEmployeeLocationEventConsumer.java` (enhanced), `LocationRolesReplicaService.java` (enhanced), test files
  - Message: "Add LocationCreatedForCustomer event consumption to populate locations table"

---

## Final Verification

### Task 4.1: Run All Tests

- [ ] Run all Customer Service tests
  - Command: `cd realguardio-customer-service && ./gradlew check`
  - Expected: All tests pass

- [ ] Run all Security System Service tests
  - Command: `cd realguardio-security-system-service && ./gradlew check`
  - Expected: All tests pass

### Task 4.2: Manual Testing (Optional)

- [ ] Start services with Docker Compose
  - Command: `docker compose up -d`

- [ ] Create a customer, team, and employee via Customer Service API

- [ ] Add team member via API

- [ ] Verify `team_members` table populated in Security System Service database

- [ ] Assign team role via API

- [ ] Verify `team_location_roles` table populated in Security System Service database

---

## Notes

### Test Dependencies Required

Add to `realguardio-security-system-service/location-roles-replica/build.gradle`:

```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.eventuate.tram.core:eventuate-tram-spring-in-memory'
    testImplementation 'org.assertj:assertj-core'
    testImplementation 'org.awaitility:awaitility'
}
```

### TDD Workflow Summary

For each test:
1. **Red**: Write failing test
2. **Green**: Implement minimum code to pass
3. **Refactor**: Clean up code while keeping tests green
4. **Commit**: Commit with all tests passing

### Event Publishing Pattern (Type-Safe)

**Type-Safe Publisher Declaration:**
```java
public interface CustomerEventPublisher
    extends DomainEventPublisherForAggregate<Customer, Long, CustomerEvent> {
}
```

**Publishing Events:**
```java
Customer customer = customerRepository.findRequiredById(customerId);
customerEventPublisher.publish(customer, event);
```

**Benefits:**
- Compile-time type safety
- Automatic aggregate ID extraction
- Simplified API

### Event Consumption Pattern (Annotation-Based)

**Event Handler Methods:**
```java
@EventuateDomainEventHandler(
    subscriberId = "osoFactTablesReplicaDispatcher",
    channel = "io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer"
)
public void handleEvent(DomainEventEnvelope<EventClass> envelope) {
    EventClass event = envelope.getEvent();
    // Process event
}
```

**Benefits:**
- No need for explicit `DomainEventDispatcher` bean configuration
- Handlers are automatically discovered by the framework
- Cleaner, more declarative code

### Next Steps

After completing this plan:
1. Implement `CustomerEmployeeAssignedCustomerRole` event consumption (event already published by Customer Service)
2. Implement `TeamMemberRemoved` event (requires new `removeTeamMember()` business logic)
3. Implement `TeamLocationRoleRemoved` event (requires new `removeTeamRole()` business logic)
