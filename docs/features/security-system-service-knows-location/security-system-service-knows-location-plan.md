# Security System Service Knows Location - Implementation Plan

## Instructions for Coding Agent

When implementing this plan:

1. **ALWAYS** use the `idea-to-code:plan-tracking` skill to track task completion in this file
2. **ALWAYS** write code using TDD:
   - Use the `idea-to-code:tdd` skill when implementing code
   - NEVER write production code (`src/main/java/**/*.java`) without first writing a failing test (`src/test/java/**/*.java`)
   - Before using the Write tool on any `.java` file in `src/main/`, ask: "Do I have a failing test for this?" If not, write the test first
   - When task direction changes mid-implementation, return to TDD PLANNING state and write a test first
3. **ALWAYS** after completing a task, when the tests pass and the task has been marked as complete, commit the changes

## Overview

This plan implements the "Security System Service Knows Location" feature in backwards-compatible phases:

- **Phase 1**: Additive changes (new endpoints, commands, event publishers)
- **Phase 2**: New flow support (locationId-based saga alongside existing flow)
- **Phase 3**: Migrate E2E tests to use new flow
- **Phase 4**: Remove old code paths (one service at a time, E2E tests must pass after each)

**Key constraint**: After each service update, end-to-end tests for backend services and BFF must pass.

---

## Steel Thread 1 – Location Creation Endpoint (E1-US1)

Implements: "As an administrator, I want to create a Location for a Customer without specifying a SecuritySystem"

### Task 1.1: Add LocationController REST endpoint

- [x] Create `LocationController` class in `customer-service-restapi`
- [x] Create `CreateLocationRequest` record with `name` field
- [x] Create `CreateLocationResponse` record with `locationId` field
- [x] Add `POST /customers/{customerId}/locations` endpoint
- [x] Endpoint accepts `CreateLocationRequest`, returns `CreateLocationResponse`
- [x] Endpoint calls `CustomerService.createLocationForCustomer(customerId, name)`

### Task 1.2: Add CustomerService.createLocationForCustomer method

- [x] Add `createLocationForCustomer(Long customerId, String name)` method to `CustomerService`
- [x] Method finds Customer by ID, creates Location, returns locationId
- [x] Method publishes `LocationCreatedForCustomer` event (already exists)
- [x] Throws exception if Customer not found

### Task 1.3: Integration test for Location creation endpoint

- [x] Create `LocationControllerIntegrationTest` in `customer-service-restapi`
- [x] Test: POST creates Location and returns locationId
- [x] Test: POST with invalid customerId returns 404

---

## Steel Thread 2 – ValidateLocation Command Handler

Implements support for saga to validate locationId exists and retrieve locationName.

### Task 2.1: Implement ValidateLocationCommand handler

- [x] Create `ValidateLocationCommand` record with `locationId` field in `customer-service-api-messaging`
- [x] Create `LocationValidated` reply record with `locationId`, `locationName`, `customerId` fields
- [x] Create `LocationNotFound` reply record
- [x] Add handler method for `ValidateLocationCommand` in `CustomerCommandHandler`
- [x] Handler looks up Location by ID
- [x] Returns `LocationValidated` with location details if found
- [x] Returns `LocationNotFound` if not found

### Task 2.2: Unit test for ValidateLocationCommand handler

- [x] Test: Valid locationId returns `LocationValidated` with correct details
- [x] Test: Invalid locationId returns `LocationNotFound`

---

## Steel Thread 3 – Security System Service Publishes SecuritySystemAssignedToLocation Event

Implements: Security System Service becomes the source of the SecuritySystem-Location relationship event.

### Task 3.1: Publish SecuritySystemAssignedToLocation event when SecuritySystem is created with locationId

- [x] Create `SecuritySystemAssignedToLocation` event record in `security-system-service-domain`
- [x] Event contains `securitySystemId` and `locationId` fields
- [x] Create `SecuritySystemEventPublisher` class in `security-system-service-domain`
- [x] Use Eventuate Tram domain events framework
- [x] Modify `SecuritySystemService.createSecuritySystem` to accept optional `locationId` and `locationName` parameters
- [x] When locationId is provided, publish `SecuritySystemAssignedToLocation` event
- [x] SecuritySystem starts in `DISARMED` state when created with locationId

---

## Steel Thread 4 – OSO Integration Service Consumes Event from Security System Service

Implements: OSO Integration Service listens for `SecuritySystemAssignedToLocation` from Security System Service.

### Task 4.1: Consume SecuritySystemAssignedToLocation event and create Oso fact

- [x] Create `SecuritySystemEventConsumer` class in `oso-event-subscribers`
- [x] Subscribe to Security System Service's domain event channel
- [x] Handle `SecuritySystemAssignedToLocation` event
- [x] Call `RealGuardOsoFactManager.assignSecuritySystemToLocation(securitySystemId, locationId)` when event received
- [x] This creates `has_relation(SecuritySystem, "location", Location)` fact in Oso Cloud

### Task 4.2: Integration test for Oso fact creation from Security System Service event

- [x] Test: When `SecuritySystemAssignedToLocation` event is published by Security System Service, Oso fact is created
- [x] Verify fact exists in Oso Cloud

---

## Steel Thread 5 – One SecuritySystem Per Location Constraint (E2-US2)

Implements: "As an administrator, I want to be prevented from creating multiple SecuritySystems for the same Location"

### Task 5.1: Enforce one-SecuritySystem-per-Location constraint in SecuritySystemService

- [x] Add unique constraint on `locationId` column in `SecuritySystem` entity
- [x] Create `LocationAlreadyHasSecuritySystemException` exception class
- [x] Catch `DataIntegrityViolationException` on save and throw `LocationAlreadyHasSecuritySystemException`

### Task 5.2: Unit test for one-SecuritySystem-per-Location constraint

- [x] Test: Creating second SecuritySystem for same locationId throws exception

---

## Steel Thread 6 – CreateSecuritySystemWithLocationId Command

Implements: New command for saga to create SecuritySystem with locationId.

### Task 6.1: Implement CreateSecuritySystemWithLocationIdCommand handler

- [x] Create `CreateSecuritySystemWithLocationIdCommand` record in `security-system-service-api-messaging`
- [x] Command contains `locationId` and `locationName` fields
- [x] Create `SecuritySystemCreatedWithLocationId` reply record with `securitySystemId`
- [x] Add handler for `CreateSecuritySystemWithLocationIdCommand` in `SecuritySystemCommandHandler`
- [x] Handler calls `SecuritySystemService.createSecuritySystem(locationId, locationName)`
- [x] Returns `SecuritySystemCreatedWithLocationId` on success
- [x] Returns error reply if constraint violated

### Task 6.2: Unit test for CreateSecuritySystemWithLocationIdCommand handler

- [x] Test: Valid command creates SecuritySystem and returns securitySystemId
- [x] Test: Duplicate locationId returns error reply

---

## Steel Thread 7 – Saga Supports LocationId-Based Flow (E2-US1)

Implements: "As an administrator, I want to create a SecuritySystem for an existing Location by providing its locationId"

### Task 7.1: Implement CreateSecuritySystemWithLocationIdSaga and route requests

- [x] Add optional `locationId` field to `CreateSecuritySystemRequest` in `orchestration-restapi`
- [x] Keep existing `locationName` field for backwards compatibility
- [x] Request is valid if either `locationId` OR (`customerId` + `locationName`) is provided
- [x] Create `CreateSecuritySystemWithLocationIdSagaData` class in `orchestration-sagas`
- [x] Contains: `locationId`, `locationName`, `customerId`, `securitySystemId`
- [x] Create new saga class `CreateSecuritySystemWithLocationIdSaga` in `orchestration-sagas`
- [x] Step 1: Send `ValidateLocationCommand` to Customer Service
- [x] On reply: Store `locationName` and `customerId` from `LocationValidated`
- [x] Step 2: Send `CreateSecuritySystemWithLocationIdCommand` to Security System Service
- [x] On reply: Store `securitySystemId`
- [x] No compensation needed (validation is read-only, creation failure doesn't require rollback)
- [x] Modify `SecuritySystemController` in `orchestration-restapi`
- [x] If `locationId` is provided in request, start `CreateSecuritySystemWithLocationIdSaga`
- [x] If `locationName` + `customerId` is provided, use existing `CreateSecuritySystemSaga`

### Task 7.2: Integration test for locationId-based SecuritySystem creation

- [x] Test: Create Location via Customer Service endpoint
- [x] Test: Create SecuritySystem with locationId via Orchestration Service
- [x] Verify: SecuritySystem is created in DISARMED state
- [x] Verify: SecuritySystem has correct locationId and locationName

---

## Steel Thread 8 – Authorization Works with New Flow (E3-US1)

Implements: "As a customer employee, I want my existing location-based permissions to continue working"

### Task 8.1: End-to-end authorization test with new flow

- [x] Create Location via new endpoint
- [x] Create SecuritySystem with locationId
- [x] Assign `SECURITY_SYSTEM_ARMER` role to employee at Location
- [x] Verify: Employee can arm the SecuritySystem
- [x] Verify: Employee without role cannot arm the SecuritySystem

---

## Steel Thread 9 – Error Handling: Invalid LocationId (Scenario 4)

Implements: "SecuritySystem Creation with Invalid LocationId" scenario.

### Task 9.1: Handle LocationNotFound error in saga and REST endpoint

- [x] When `ValidateLocationCommand` returns `LocationNotFound`, saga fails
- [x] Saga state reflects failure reason
- [x] No SecuritySystem is created
- [x] Modify `SecuritySystemController` to handle saga failure
- [x] Return 404 with error message "Location not found"

### Task 9.2: Integration test for invalid locationId

- [x] Test: POST /securitysystems with non-existent locationId returns 404
- [x] Test: No SecuritySystem is created

---

## Steel Thread 10 – Error Handling: Duplicate SecuritySystem (Scenario 3)

Implements: "Prevent Duplicate SecuritySystem Assignment" scenario.

### Task 10.1: Handle constraint violation error in saga and REST endpoint

- [x] When `CreateSecuritySystemWithLocationIdCommand` returns constraint error, saga fails
- [x] Saga state reflects failure reason
- [x] Return 409 Conflict with error message "Location already has a SecuritySystem"

### Task 10.2: Integration test for duplicate SecuritySystem

- [x] Test: Create Location and SecuritySystem successfully
- [x] Test: Attempt to create second SecuritySystem for same Location
- [x] Verify: Returns 409 Conflict
- [x] Verify: First SecuritySystem remains unaffected

---

## Steel Thread 11 – Migrate E2E Tests to New Flow

Migrates existing end-to-end tests to use the new locationId-based flow. This validates the new flow works before removing old code.

### Task 11.1: Update backend E2E tests to create Location first

- [x] Identify existing E2E tests that create SecuritySystems via the old flow
- [x] Update tests to: (1) create Location via new endpoint, (2) create SecuritySystem with locationId
- [x] Run E2E tests and verify they pass

### Task 11.2: Update BFF tests if applicable

- [x] Identify BFF tests that use the old SecuritySystem creation flow
- [x] Update to use locationId-based flow (N/A - BFF tests only display, don't create)
- [x] Run BFF tests and verify they pass

### Task 11.3: Verify all E2E tests pass with both flows available

- [x] Run full E2E test suite
- [x] Confirm both old and new flows work (backwards compatibility maintained)

---

## Steel Thread 12 – Remove Old Code from Orchestration Service

Removes the old `CreateSecuritySystemSaga` that creates Location during SecuritySystem creation.

### Task 12.1: Remove old CreateSecuritySystemSaga

- [x] Delete `CreateSecuritySystemSaga` class (the one that takes locationName)
- [x] Delete `CreateSecuritySystemSagaData` class
- [x] Remove `customerId` and `locationName` fields from `CreateSecuritySystemRequest` (keep only `locationId`)

### Task 12.2: Update SecuritySystemController to only accept locationId

- [x] Remove routing logic that chose between old and new saga
- [x] Controller now only starts `CreateSecuritySystemWithLocationIdSaga`
- [x] Return 400 Bad Request if `locationId` is not provided

### Task 12.3: Run E2E tests after Orchestration Service changes

- [x] Run full E2E test suite
- [x] Verify all tests pass

---

## Steel Thread 13 – Remove Old Code from Customer Service

Removes `securitySystemId` field from Location and related code.

### Task 13.1: Remove securitySystemId from Location entity

- [x] Remove `securitySystemId` field from `Location` entity
- [x] Remove `addSecuritySystem(Long securitySystemId)` method
- [x] Remove `LocationAlreadyHasSecuritySystemException` (if it exists in Customer Service)

### Task 13.2: Remove CreateLocationWithSecuritySystemCommand handler

- [x] Remove `CreateLocationWithSecuritySystemCommand` class
- [x] Remove `LocationCreatedWithSecuritySystem` reply class
- [x] Remove handler from `CustomerCommandHandler`

### Task 13.3: Stop publishing SecuritySystemAssignedToLocation from Customer Service

- [x] Remove `SecuritySystemAssignedToLocation` event class from Customer Service (if it exists there)
- [x] Remove any code that publishes this event from Customer Service

### Task 13.4: Run E2E tests after Customer Service changes

- [x] Run full E2E test suite
- [x] Verify all tests pass

---

## Steel Thread 14 – Remove Old Code from Security System Service

Removes `CREATION_PENDING` state and `noteLocationCreated` method.

### Task 14.1: Remove CREATION_PENDING state

- [ ] Remove `CREATION_PENDING` from `SecuritySystemState` enum
- [ ] Update any code that references this state
- [ ] SecuritySystem now only has: `DISARMED`, `ARMED`, `ALARMED`, `CREATION_FAILED`

### Task 14.2: Remove noteLocationCreated method

- [ ] Remove `noteLocationCreated(Long locationId)` method from `SecuritySystem` entity
- [ ] Remove `NoteLocationCreatedCommand` class
- [ ] Remove handler for `NoteLocationCreatedCommand` from `SecuritySystemCommandHandler`
- [ ] Remove `LocationNoted` reply class

### Task 14.3: Run E2E tests after Security System Service changes

- [ ] Run full E2E test suite
- [ ] Verify all tests pass

---

## Steel Thread 15 – Remove Old Event Listener from OSO Integration Service

Removes the listener for `SecuritySystemAssignedToLocation` from Customer Service channel.

### Task 15.1: Remove old event handler from CustomerEventConsumer

- [ ] Remove `handleSecuritySystemAssignedToLocation` method that listens to Customer Service channel
- [ ] Keep `SecuritySystemEventConsumer` that listens to Security System Service channel

### Task 15.2: Run E2E tests after OSO Integration Service changes

- [ ] Run full E2E test suite
- [ ] Verify all tests pass
- [ ] Verify authorization still works correctly

---

## Steel Thread 16 – Rename "WithLocationId" Classes and Methods

Now that all old code is removed, simplify the names by removing the "WithLocationId" suffix.

### Task 16.1: Rename classes and methods

- [ ] Rename `CreateSecuritySystemWithLocationIdSaga` → `CreateSecuritySystemSaga`
- [ ] Rename `CreateSecuritySystemWithLocationIdSagaData` → `CreateSecuritySystemSagaData`
- [ ] Rename `CreateSecuritySystemWithLocationIdCommand` → `CreateSecuritySystemCommand`
- [ ] Rename `SecuritySystemCreatedWithLocationId` → `SecuritySystemCreated` (if separate from existing)
- [ ] Rename `createSecuritySystemWithLocationId()` method → `createSecuritySystem()`
- [ ] Update all references in configuration, tests, and other classes
- [ ] Run tests and verify

---

## Steel Thread 17 – Final Verification

Comprehensive verification that all old code is removed and new flow works correctly.

### Task 17.1: Verify no references to old code remain

- [ ] Search codebase for `securitySystemId` in Location-related code
- [ ] Search for `CREATION_PENDING` references
- [ ] Search for `CreateLocationWithSecuritySystemCommand` references
- [ ] Search for `noteLocationCreated` references

### Task 17.2: Run full test suite

- [ ] Run `./gradlew check` across all modules
- [ ] Run E2E tests
- [ ] Run BFF tests

### Task 17.3: Document migration complete

- [ ] Update specification to reflect current state
- [ ] Note that old flow has been fully removed

---

## Change History

### 2026-01-02: Consolidated granular tasks into meaningful units

Consolidated tasks that represented implementation details (DTOs, commands, events, data classes) into the tasks that use them. A meaningful task represents an independently testable, deliverable unit of functionality.

**Changes made:**

| Steel Thread | Before | After |
|--------------|--------|-------|
| ST1 | Task 1.1 (DTOs) + Task 1.2 (controller) | Merged into Task 1.1 (controller includes DTOs) |
| ST2 | Task 2.1 (command/replies) + Task 2.2 (handler) | Merged into Task 2.1 (handler includes command/replies) |
| ST3 | Task 3.1 (event) + Task 3.2 (publisher) + Task 3.3 (publish logic) | Merged into Task 3.1 (single task for event publishing) |
| ST4 | Task 4.1 (consumer) + Task 4.2 (create fact) | Merged into Task 4.1 (consumer that creates fact) |
| ST5 | Task 5.1 (repository method) + Task 5.2 (enforce constraint) | Merged into Task 5.1 (constraint enforcement includes repository) |
| ST6 | Task 6.1 (command/replies) + Task 6.2 (handler) | Merged into Task 6.1 (handler includes command/replies) |
| ST7 | Task 7.1 (request field) + Task 7.2 (saga data) + Task 7.3 (saga) + Task 7.4 (routing) | Merged into Task 7.1 (saga implementation includes all supporting artifacts) |
| ST9 | Task 9.1 (saga error) + Task 9.2 (REST error) | Merged into Task 9.1 (error handling end-to-end) |
| ST10 | Task 10.1 (saga error) + Task 10.2 (REST error) | Merged into Task 10.1 (error handling end-to-end) |

**Rationale:** Creating DTOs, commands, events, and data classes are not meaningful standalone tasks - they are implementation details of the functionality that uses them.
