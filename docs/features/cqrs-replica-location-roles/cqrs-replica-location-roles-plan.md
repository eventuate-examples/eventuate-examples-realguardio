# CQRS Replica Location Roles - Implementation Plan

## Overview
This plan implements the CQRS pattern for maintaining a replica of customer employee location roles in the Security Service using the Steel Thread methodology. Each thread represents a narrow end-to-end flow that delivers value.

## Implementation Instructions for Coding Agent
- Mark each checkbox `[x]` when a task or step is completed
- Follow TDD principles: Write test first, then implementation, then refactor
- Run tests after each implementation to ensure nothing breaks
- Commit changes after each completed thread when tests pass

## Steel Thread 1: Customer Service Event Publishing Foundation

### Thread Goal
Enable the Customer Service to publish domain events when location roles are assigned.

### Implementation Tasks

[x] **Task 1.1: Write unit test for event publishing**

```text
Write a unit test in CustomerServiceTest that verifies the CustomerService publishes a CustomerEmployeeAssignedLocationRole event when assignLocationRole() is called.

Steps:
[x] Create test method shouldCreateCustomerEmployeeAndAssignLocationRoles() in CustomerServiceTest
[x] Mock DomainEventPublisher
[x] Call assignLocationRole() method
[x] Verify that publish() was called with correct event data
[x] Run the test - it should fail (red phase)
```

[x] **Task 1.2: Create the domain event class**

```text
Create the CustomerEmployeeAssignedLocationRole event class in the Customer Service.

Steps:
[x] Create the event class in package io.eventuate.examples.realguardio.customerservice.domain
[x] Implement as a record with fields: userName (String), locationId (Long), roleName (String)
[x] Implement DomainEvent interface from Eventuate Tram
[x] Run the test - it should still fail
```

[x] **Task 1.3: Implement event publishing in CustomerService**

```text
Modify CustomerService to publish the event when assignLocationRole() is called.

Steps:
[x] Inject DomainEventPublisher into CustomerService
[x] In assignLocationRole() method, after business logic, publish CustomerEmployeeAssignedLocationRole event
[x] Use domainEventPublisher.publish("Customer", customerId.toString(), Collections.singletonList(event))
[x] Ensure the method is @Transactional for transactional event publishing
[x] Run the test - it should now pass (green phase)
[x] Run all CustomerService tests to ensure nothing broke
[x] Commit changes with message "Implement event publishing for location role assignment"
```

## Steel Thread 2: Security Service Event Consumer Infrastructure

### Thread Goal
Set up the Security Service infrastructure to consume events and store them in a replica database.

### Implementation Tasks

[ ] **Task 2.1: Create location-roles-replica subproject structure**

```text
Create a new subproject for the location roles replica functionality in the Security Service.

Steps:
[ ] Create directory structure: security-service/location-roles-replica
[ ] Add the subproject to settings.gradle
[ ] Create build.gradle with dependencies:
    - io.eventuate.tram.core:eventuate-tram-spring-events-subscriber-starter
    - spring-boot-starter-jdbc
    - spring-boot-starter-web
[ ] Create main source directory structure
[ ] Run ./gradlew build to verify setup
```

[ ] **Task 2.2: Copy event class to Security Service**

```text
Create a copy of the CustomerEmployeeAssignedLocationRole event class in the Security Service.

Steps:
[ ] Create package io.eventuate.examples.realguardio.customerservice.domain in location-roles-replica
[ ] Copy CustomerEmployeeAssignedLocationRole event class to this package (must be exact same package!)
[ ] Ensure it implements DomainEvent interface
[ ] Verify the package and class name match exactly with Customer Service version
```

[ ] **Task 2.3: Create database schema for replica**

```text
Create the database table for storing location role replicas.

Steps:
[ ] Create SQL migration file for customer_employee_location_role table
[ ] Include columns: id (BIGSERIAL PRIMARY KEY), user_name (VARCHAR), location_id (BIGINT), role_name (VARCHAR), created_at (TIMESTAMP)
[ ] Add migration to appropriate location for Security Service database
[ ] Run migration to create table
```

## Steel Thread 3: Event Consumer Implementation

### Thread Goal
Implement the event consumer that processes CustomerEmployeeAssignedLocationRole events and updates the replica database.

### Implementation Tasks

[ ] **Task 3.1: Write component test for event consumption**

```text
Write a test that verifies the Security Service can consume events and update the database.

Steps:
[ ] Create LocationRolesReplicaServiceTest or add to SecurityServiceTest
[ ] Write test shouldConsumeEventAndUpdateDatabase()
[ ] Use in-memory message broker or test utilities
[ ] Publish a test CustomerEmployeeAssignedLocationRole event
[ ] Verify the database is updated with correct data
[ ] Run test - it should fail (red phase)
```

[ ] **Task 3.2: Implement LocationRolesReplicaService**

```text
Create the service layer for managing the replica data.

Steps:
[ ] Create LocationRolesReplicaService class with @Service and @Transactional
[ ] Inject JdbcTemplate
[ ] Implement saveLocationRole(userName, locationId, roleName) method
[ ] Use JDBC to insert into customer_employee_location_role table
[ ] Run test - still failing (no event consumer yet)
```

[ ] **Task 3.3: Implement event consumer**

```text
Create the event consumer that handles incoming events.

Steps:
[ ] Create CustomerEmployeeLocationEventConsumer class
[ ] Inject LocationRolesReplicaService
[ ] Create domainEventHandlers() method returning DomainEventHandlers
[ ] Use DomainEventHandlersBuilder.forAggregateType("Customer").onEvent(CustomerEmployeeAssignedLocationRole.class, this::handle).build()
[ ] In handler method, extract event from DomainEventEnvelope and call replicaService.saveLocationRole()
[ ] Create @Configuration class with @Bean for DomainEventDispatcher
[ ] Inject DomainEventDispatcherFactory and use it to create dispatcher with unique subscriberId
[ ] Run test - should now pass (green phase)
[ ] Run all Security Service tests
[ ] Commit changes with message "Implement event consumer for location roles replica"
```

## Steel Thread 4: Query API for Replica Data

### Thread Goal
Provide a REST API to query the replicated location role data.

### Implementation Tasks

[ ] **Task 4.1: Write test for query functionality**

```text
Write tests for querying location roles from the replica.

Steps:
[ ] Add test method to LocationRolesReplicaServiceTest
[ ] Test findLocationRoles() with different parameter combinations
[ ] Test filtering by userName
[ ] Test filtering by locationId
[ ] Run tests - they should fail (red phase)
```

[ ] **Task 4.2: Implement query methods in service**

```text
Add query functionality to LocationRolesReplicaService.

Steps:
[ ] Create LocationRole DTO/record class
[ ] Implement findLocationRoles(userName, locationId) in service
[ ] Build dynamic SQL query based on provided parameters
[ ] Use JdbcTemplate to execute query and map results
[ ] Run tests - should pass (green phase)
```

[ ] **Task 4.3: Create REST controller**

```text
Implement the REST API endpoint for querying location roles.

Steps:
[ ] Create LocationRolesController with @RestController
[ ] Add @RequestMapping("/location-roles")
[ ] Inject LocationRolesReplicaService
[ ] Implement GET endpoint with optional userName and locationId parameters
[ ] Return list of LocationRole objects
[ ] Write integration test for the REST endpoint
[ ] Run all tests
[ ] Commit changes with message "Add query API for location roles replica"
```

## Steel Thread 5: Component Integration Tests

### Thread Goal
Verify the complete event flow works within each service using component tests.

### Implementation Tasks

[ ] **Task 5.1: Update Customer Service component test**

```text
Verify events are written to the outbox table in Customer Service.

Steps:
[ ] Update CustomerServiceInProcessComponentTest
[ ] Add test to verify event is written to outbox when assignLocationRole() is called
[ ] Use actual database and Eventuate Tram
[ ] Verify outbox contains CustomerEmployeeAssignedLocationRole event
[ ] Run test - adjust implementation if needed
```

[ ] **Task 5.2: Update Security Service component test**

```text
Test the complete event consumption flow in Security Service.

Steps:
[ ] Update SecuritySystemServiceComponentTest
[ ] Add test that publishes event and verifies database update
[ ] Test that query API returns correct data after event processing
[ ] Verify eventual consistency behavior
[ ] Run all component tests
[ ] Commit changes with message "Add component tests for CQRS replica"
```

## Steel Thread 6: End-to-End Integration

### Thread Goal
Verify the complete CQRS flow works across both services in a real environment.

### Implementation Tasks

[ ] **Task 6.1: Implement end-to-end test**

```text
Create an end-to-end test that verifies the complete CQRS flow.

Steps:
[ ] Update RealGuardioEndToEndTest
[ ] Add test shouldReplicateLocationRolesToSecurityService()
[ ] Use Customer Service API to create customer and assign location role
[ ] Wait for event propagation (use appropriate wait/retry mechanism)
[ ] Query Security Service location-roles API
[ ] Verify replica contains expected data
[ ] Handle eventual consistency with retry logic
[ ] Run end-to-end test with Docker Compose or similar
```

[ ] **Task 6.2: Configuration and deployment updates**

```text
Update configuration files for both services.

Steps:
[ ] Update Customer Service application.yml with Eventuate configuration
[ ] Update Security Service application.yml with consumer configuration
[ ] Verify Kafka/messaging infrastructure is properly configured
[ ] Update Docker Compose or deployment files if needed
[ ] Run full end-to-end test suite
[ ] Verify all tests pass
```

[ ] **Task 6.3: Final verification and cleanup**

```text
Perform final verification and any necessary cleanup.

Steps:
[ ] Run complete test suite (unit, component, end-to-end)
[ ] Fix any failing tests
[ ] Remove any TODO comments or placeholder code
[ ] Verify error handling works correctly
[ ] Test with multiple events to verify ordering
[ ] Run gradle build to ensure everything compiles
[ ] Commit final changes with message "Complete CQRS replica implementation for location roles"
```

## Steel Thread 7: Observability and Monitoring (Optional Enhancement)

### Thread Goal
Add logging and monitoring to track event flow and diagnose issues.

### Implementation Tasks

[ ] **Task 7.1: Add structured logging**

```text
Add appropriate logging to track event publishing and consumption.

Steps:
[ ] Add debug logs when events are published
[ ] Add info logs when events are consumed
[ ] Add error logs for failures
[ ] Include correlation IDs if available
[ ] Test logging output in local environment
```

[ ] **Task 7.2: Add metrics**

```text
Add metrics to track event processing performance.

Steps:
[ ] Add counter for published events
[ ] Add counter for consumed events
[ ] Add timer for event processing duration
[ ] Add gauge for replica lag if applicable
[ ] Verify metrics are exposed properly
```

## Testing Checklist

### Unit Tests
[ ] CustomerServiceTest.shouldCreateCustomerEmployeeAndAssignLocationRoles()
[ ] LocationRolesReplicaServiceTest.shouldConsumeEventAndUpdateDatabase()
[ ] LocationRolesReplicaServiceTest.shouldQueryLocationRoles()

### Component Tests
[ ] CustomerServiceInProcessComponentTest - Event in outbox
[ ] SecuritySystemServiceComponentTest - Event consumption and query

### End-to-End Tests
[ ] RealGuardioEndToEndTest.shouldReplicateLocationRolesToSecurityService()

## Success Criteria
[ ] Customer Service publishes events when location roles are assigned
[ ] Security Service successfully consumes and stores events
[ ] Query API returns correct replica data
[ ] All tests pass (unit, component, end-to-end)
[ ] No data loss during normal operations
[ ] System handles transient failures gracefully

## Notes for Implementation
- Always use TDD: Write test first, then implementation
- Run tests after each change to ensure nothing breaks
- Commit after each completed thread when tests pass
- The event class MUST be in the exact same package in both services
- Use ./gradlew (not gradle) for all build commands
- Check TEST*.xml files for detailed test failure information
- Throw UnsupportedOperationException("Implement me") for placeholder methods

## Change History
<!-- Record any changes to the plan requested during implementation -->

### 2025-09-05: Initial plan updated with Eventuate Tram documentation
- Updated Task 1.3 to use correct DomainEventPublisher.publish() signature with aggregate type string and ID
- Added @Transactional requirement for event publishing
- Updated Task 2.1 to use eventuate-tram-spring-events-subscriber-starter dependency
- Updated Task 3.3 with proper DomainEventDispatcher configuration using DomainEventDispatcherFactory
- Aligned with official Eventuate Tram patterns from documentation