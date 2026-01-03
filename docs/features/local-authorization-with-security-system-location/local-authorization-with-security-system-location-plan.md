# Local Authorization with Security System Location - Steel Thread Implementation Plan

## Instructions for Coding Agent

Before implementing any task in this plan:

1. **ALWAYS** use the `idea-to-code:plan-tracking` skill to track task completion
2. **ALWAYS** write code using TDD:
   - Use the `idea-to-code:tdd` skill when implementing code
   - **NEVER** write production code (`src/main/java/**/*.java`) without first writing a failing test (`src/test/java/**/*.java`)
   - Before using the Write tool on any `.java` file in `src/main/`, ask: "Do I have a failing test for this?" If not, write the test first.
   - When task direction changes mid-implementation, return to TDD PLANNING state and write a test first
3. **ALWAYS** after completing a task, when the tests pass and the task has been marked as complete, commit the changes

## Overview

**Idea Type:** D - Educational/Example Repository

This plan implements the `OsoLocalSecuritySystemLocation` profile for demonstrating Oso local authorization with data bindings for the SecuritySystem-Location relationship.

**Primary Scenario:** A customer employee with the `SECURITY_SYSTEM_DISARMER` role at a location successfully disarms a security system assigned to that location, with authorization evaluated entirely using local database queries (no SecuritySystem-Location facts in Oso Cloud).

---

## Steel Thread 1: Configure Local Authorization Data Bindings with Profile-Based Selection

**Goal:** Create two versions of `local_authorization_config.yaml` - one for cloud-based authorization (existing) and one for local authorization with the SecuritySystem-Location fact definition. The profile selects which version to use.

### Task 1.1: Rename existing local_authorization_config.yaml to indicate it's for cloud mode

- [x] The existing `local_authorization_config.yaml` does NOT include the SecuritySystem-Location fact (it relies on Oso Cloud)
- [x] Keep it as the default configuration (no rename needed - it will be loaded when `OsoLocalSecuritySystemLocation` profile is NOT active)

**File:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/resources/local_authorization_config.yaml` (unchanged)

### Task 1.2: Create local_authorization_config_with_security_system_location.yaml

- [x] Create new file `local_authorization_config_with_security_system_location.yaml`
- [x] Copy contents from existing `local_authorization_config.yaml`
- [x] Add the `has_relation(SecuritySystem:_, String:location, Location:_)` fact definition with SQL query
- [x] Query should be: `SELECT id, location_id FROM security_system WHERE location_id IS NOT NULL`
- [x] Ensure `sql_types` section includes both `Location: integer` and `SecuritySystem: integer`

**File to create:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/resources/local_authorization_config_with_security_system_location.yaml`

### Task 1.3: Refactor ClasspathLocalAuthorizationConfigFileSupplier to accept file name

- [x] Modify `ClasspathLocalAuthorizationConfigFileSupplier` to accept config file path as constructor parameter
- [x] Default value should be `/local_authorization_config.yaml` for backward compatibility
- [x] The class loads the config file from the classpath using the provided path

**File to modify:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/ClasspathLocalAuthorizationConfigFileSupplier.java`

### Task 1.4: Create profile-based @Bean methods for LocalAuthorizationConfigFileSupplier

- [x] Modify `OsoSecuritySystemActionAuthorizerConfiguration` to have two `@Bean` methods:
  - `@Bean @Profile("!OsoLocalSecuritySystemLocation")` → `new ClasspathLocalAuthorizationConfigFileSupplier("/local_authorization_config.yaml")`
  - `@Bean @Profile("OsoLocalSecuritySystemLocation")` → `new ClasspathLocalAuthorizationConfigFileSupplier("/local_authorization_config_with_security_system_location.yaml")`
- [x] Remove any existing bean definition for `LocalAuthorizationConfigFileSupplier` that doesn't have profile annotation

**File to modify:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/OsoSecuritySystemActionAuthorizerConfiguration.java`

---

## Steel Thread 2: Implement Local Authorization for SecuritySystemActionAuthorizer

**Goal:** When the `OsoLocalSecuritySystemLocation` profile is active, authorization checks must use `authorizeLocal()` instead of `authorize()`. The `authorizeLocal()` method returns a SQL query that must be executed against the local database.

**Key insight:** `oso.authorize()` returns a boolean directly from Oso Cloud. `oso.authorizeLocal()` returns a SQL query string that must be executed locally. These are fundamentally different approaches.

### Task 2.1: Add authorizeLocal() method to OsoService

- [x] Add method `String authorizeLocal(String actorType, String actorId, String action, String resourceType, String resourceId)` to `OsoService`
- [x] Implementation calls `oso.authorizeLocal()` which returns a SQL WHERE clause
- [x] The SQL can be used to check if a specific resource is authorized

**File to modify:** `realguardio-oso-integration-service/oso-service/src/main/java/io/realguardio/osointegration/ososervice/OsoService.java`

### Task 2.2: Add authorizeLocal() method to RealGuardOsoAuthorizer

- [x] Add method `String authorizeLocal(String user, String action, String resourceType, String resourceId)` to `RealGuardOsoAuthorizer`
- [x] Calls `osoService.authorizeLocal()` with appropriate parameters
- [x] Consider resilience patterns (circuit breaker, retry) similar to `isAuthorized()`

**File to modify:** `realguardio-oso-integration-service/oso-service/src/main/java/io/realguardio/osointegration/ososervice/RealGuardOsoAuthorizer.java`

### Task 2.3: Create OsoLocalSecuritySystemActionAuthorizer

- [ ] Create new class `OsoLocalSecuritySystemActionAuthorizer` implementing `SecuritySystemActionAuthorizer`
- [ ] Inject `RealGuardOsoAuthorizer` and `JdbcTemplate` (or appropriate repository)
- [ ] In `verifyCanDo(securitySystemId, permission)`:
  - Call `realGuardOsoAuthorizer.authorizeLocal(user, permission, "SecuritySystem", securitySystemId)`
  - Execute the returned SQL query against the local database
  - If query returns the securitySystemId, authorization passes
  - If query returns empty, throw `ForbiddenException`
- [ ] Annotate with `@Profile("OsoLocalSecuritySystemLocation")`

**Location:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/`

### Task 2.4: Update OsoSecuritySystemActionAuthorizer with profile annotation

- [ ] Add `@Profile("!OsoLocalSecuritySystemLocation")` to the existing `OsoSecuritySystemActionAuthorizer` bean definition
- [ ] This ensures the cloud-based authorizer is used when the new profile is NOT active

**File to modify:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/OsoSecuritySystemActionAuthorizerConfiguration.java`

---

## Steel Thread 3: Implement Conditional Event Publishing

**Goal:** When the `OsoLocalSecuritySystemLocation` profile is active, the `SecuritySystemAssignedToLocation` event should NOT be published.

### Task 3.1: Create SecuritySystemLocationEventPublishingPolicy interface

- [ ] Create interface `SecuritySystemLocationEventPublishingPolicy` in `security-system-service-domain`
- [ ] Define method `boolean shouldPublishSecuritySystemAssignedToLocation()`
- [ ] This interface abstracts the decision of whether to publish the event

**Location:** `realguardio-security-system-service/security-system-service-domain/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/domain/`

### Task 2.2: Implement DefaultSecuritySystemLocationEventPublishingPolicy

- [ ] Create `DefaultSecuritySystemLocationEventPublishingPolicy` that returns `true` (publish event)
- [ ] Annotate with `@Profile("!OsoLocalSecuritySystemLocation")` so it's active when profile is NOT set
- [ ] Register as a Spring bean

**Location:** `realguardio-security-system-service/security-system-service-domain/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/domain/`

### Task 2.3: Implement LocalSecuritySystemLocationEventPublishingPolicy

- [ ] Create `LocalSecuritySystemLocationEventPublishingPolicy` that returns `false` (suppress event)
- [ ] Annotate with `@Profile("OsoLocalSecuritySystemLocation")` so it's active when profile IS set
- [ ] Register as a Spring bean in a configuration class in `security-system-service-oso-integration`

**Location:** `realguardio-security-system-service/security-system-service-oso-integration/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/`

### Task 2.4: Modify SecuritySystemServiceImpl to use publishing policy

- [ ] Inject `SecuritySystemLocationEventPublishingPolicy` into `SecuritySystemServiceImpl`
- [ ] Modify `createSecuritySystemWithLocation()` to check policy before publishing event
- [ ] If `shouldPublishSecuritySystemAssignedToLocation()` returns false, skip the publish call
- [ ] Update existing unit tests to verify both behaviors (with and without the policy)

**File to modify:** `realguardio-security-system-service/security-system-service-domain/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/domain/SecuritySystemServiceImpl.java`

---

## Steel Thread 4: Integration Test for Local Authorization with authorizeLocal()

**Goal:** Verify that `authorizeLocal()` works correctly with local data bindings for the SecuritySystem-Location relationship.

### Task 4.1: Create integration test for authorizeLocal() with local data bindings

- [ ] Create test class `LocalAuthorizationWithSecuritySystemLocationIntegrationTest` in `security-system-service-oso-integration/src/integrationTest/`
- [ ] Use `@ActiveProfiles({"UseOsoService", "OsoLocalSecuritySystemLocation"})`
- [ ] Set up test data: create a security system with locationId in the local database
- [ ] Create user with `SECURITY_SYSTEM_DISARMER` role at the location (via Oso facts for the role)
- [ ] Verify `authorizeLocal(user, "disarm", "SecuritySystem", securitySystemId)` returns SQL that matches the security system
- [ ] Verify that NO `has_relation(SecuritySystem, location, Location)` fact was created in Oso Cloud
- [ ] Test should verify the SecuritySystem-Location relationship is resolved via local data bindings

**Location:** `realguardio-security-system-service/security-system-service-oso-integration/src/integrationTest/java/io/eventuate/examples/realguardio/securitysystemservice/osointegration/`

### Task 4.2: Add authorization denied test with local data bindings

- [ ] Add test case in the same test class
- [ ] Create user WITHOUT the required role at the location
- [ ] Verify `authorizeLocal(user, "disarm", "SecuritySystem", securitySystemId)` returns SQL that does NOT match the security system
- [ ] Confirms local authorization correctly denies unauthorized access

---

## Steel Thread 5: Component Test for Security System Service with Local Authorization

**Goal:** Verify the complete security system service behavior with the `OsoLocalSecuritySystemLocation` profile active.

### Task 5.1: Create component test for local authorization profile

- [ ] Create `SecuritySystemServiceWithLocalAuthorizationComponentTest` in `security-system-service-main/src/componentTest/`
- [ ] Configure service container with `SPRING_PROFILES_ACTIVE=UseOsoService,OsoLocalSecuritySystemLocation`
- [ ] Test that `CreateSecuritySystemCommand` creates a security system with correct locationId
- [ ] Verify no `SecuritySystemAssignedToLocation` event is published (check Kafka topic or outbox)
- [ ] Test that subsequent authorization works correctly

**Location:** `realguardio-security-system-service/security-system-service-main/src/componentTest/java/io/eventuate/examples/realguardio/securitysystemservice/`

---

## Steel Thread 6: End-to-End Test for Local Authorization

**Goal:** Create a complete end-to-end test that exercises the full flow with local authorization.

### Task 6.1: Add useOsoLocalSecuritySystemLocation() method to ApplicationUnderTest interface

- [ ] Add method `void useOsoLocalSecuritySystemLocation()` to `ApplicationUnderTest` interface
- [ ] Implement in `ApplicationUnderTestUsingTestContainers`:
  - Set `SPRING_PROFILES_ACTIVE=UseOsoService,OsoLocalSecuritySystemLocation` on securitySystemService
  - Set `SPRING_PROFILES_ACTIVE=UseOsoService` on customerService (unchanged)

**Files to modify:**
- `end-to-end-tests/src/endToEndTest/java/com/realguardio/endtoendtests/ApplicationUnderTest.java`
- `end-to-end-tests/src/endToEndTest/java/com/realguardio/endtoendtests/ApplicationUnderTestUsingTestContainers.java`

### Task 6.2: Create RealGuardioLocalAuthorizationEndToEndTest

- [ ] Create new test class extending `AbstractRealGuardioEndToEndTest`
- [ ] In `@BeforeAll`, call `aut.useOsoLocalSecuritySystemLocation()` before starting
- [ ] Use a unique network name (e.g., `"e2e-local-auth"`)
- [ ] Override `waitForUntilPermissionsHaveBeenAssigned()` - may need shorter/no wait since no cloud fact propagation needed for SecuritySystem-Location
- [ ] Override `waitForCustomerAdminPermission()` - still needs Oso propagation for customer roles
- [ ] Inherits `shouldCreateCustomerAndSecuritySystem()` test which will verify:
  - Customer and location created
  - Security system created with locationId
  - Arm/disarm roles assigned
  - Security system can be armed (authorization works with local data bindings)

**Location:** `end-to-end-tests/src/endToEndTest/java/com/realguardio/endtoendtests/`

### Task 6.3: Add test to verify SecuritySystemAssignedToLocation event is NOT published

- [ ] Add a new test method in `RealGuardioLocalAuthorizationEndToEndTest`
- [ ] Create security system and verify it exists with correct locationId
- [ ] Verify that authorization works (can arm/disarm)
- [ ] Add logging/verification that no `SecuritySystemAssignedToLocation` event was consumed by oso-integration-service
- [ ] This confirms the event suppression is working correctly in the end-to-end scenario

---

## Completion Checklist

After all steel threads are complete, verify:

- [ ] `./gradlew :security-system-service:check` passes (all unit and integration tests)
- [ ] Component tests pass with new profile
- [ ] End-to-end tests pass:
  - [ ] `RealGuardioEndToEndTest` (existing, without new profile)
  - [ ] `RealGuardioLocalAuthorizationEndToEndTest` (new, with new profile)
- [ ] Code demonstrates the educational concepts clearly
- [ ] Profile configuration is documented in code comments

---

## Change History

### 2026-01-03: Initial plan

Created steel-thread implementation plan based on the didactic example specification. Organized into 6 steel threads:
1. Configure local authorization data bindings with profile-based selection
2. Implement local authorization for SecuritySystemActionAuthorizer (authorizeLocal())
3. Implement conditional event publishing
4. Integration tests for local authorization
5. Component tests for service behavior
6. End-to-end tests for complete flow

Note: Backward compatibility is verified by existing tests passing throughout development, not a separate steel thread.

### 2026-01-03: Updated Steel Thread 1 for profile-based config selection

Changed approach from modifying a single `local_authorization_config.yaml` to having two separate config files:
- `local_authorization_config.yaml` - existing file for cloud-based mode (no SecuritySystem-Location fact)
- `local_authorization_config_with_security_system_location.yaml` - new file with SecuritySystem-Location fact definition

The profile selects which `LocalAuthorizationConfigFileSupplier` bean is active, which in turn determines which config file is loaded. This is cleaner and more educational.

### 2026-01-03: Simplified config file supplier approach

Instead of creating two separate supplier classes, use a single `ClasspathLocalAuthorizationConfigFileSupplier` class that accepts the config file path as a constructor parameter. Two `@Bean` methods with different `@Profile` annotations instantiate the same class with different file paths. This reduces code duplication.

### 2026-01-03: Added Steel Thread 2 for authorizeLocal() implementation

**Critical correction:** `oso.authorize()` and `oso.authorizeLocal()` are fundamentally different:
- `authorize()` → returns boolean directly from Oso Cloud
- `authorizeLocal()` → returns SQL query string that must be executed against local DB

When using local data bindings for authorization, `authorizeLocal()` must be used instead of `authorize()`. This requires:
- New `OsoLocalSecuritySystemActionAuthorizer` class that uses `authorizeLocal()`
- Methods added to `OsoService` and `RealGuardOsoAuthorizer` for `authorizeLocal()`
- Profile-based selection between cloud and local authorizer implementations

Steel threads renumbered: Conditional Event Publishing is now Steel Thread 3.
