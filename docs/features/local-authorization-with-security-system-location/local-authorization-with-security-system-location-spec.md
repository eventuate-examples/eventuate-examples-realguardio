# Local Authorization with Security System Location - Didactic Example Specification

## Overview

This specification defines an educational example demonstrating how to configure Oso local authorization (data bindings) for the SecuritySystem-Location relationship in the RealGuardio security system service.

## Learning Goals

By studying and running this example, developers will learn:

1. **Oso Local Authorization Concepts**
   - How Oso's `isAuthorized()` can use local data bindings instead of cloud-stored facts
   - The difference between cloud-based fact storage and local database queries for authorization
   - When and why to use local authorization vs cloud-based authorization

2. **Spring Profile-Based Feature Toggling**
   - How to use additive Spring profiles to modify authorization behavior
   - Pattern for conditionally enabling/disabling event publishing based on profiles
   - Configuring multiple profiles to work together (`UseOsoService,OsoLocalSecuritySystemLocation`)

3. **Oso Data Bindings Configuration**
   - Structure of `local_authorization_config.yaml` fact definitions
   - Writing SQL queries that map to Oso fact patterns
   - How Oso translates policy rules into SQL when using data bindings

4. **Authorization Architecture Patterns**
   - Trade-offs between event-driven fact synchronization and local queries
   - Reducing external service dependencies for authorization decisions
   - Maintaining consistent authorization behavior across different modes

## Concepts and Patterns Demonstrated

### 1. Oso Authorization Modes

The example demonstrates two authorization modes for the same policy:

| Mode | Profile | Fact Source | Use Case |
|------|---------|-------------|----------|
| Cloud-based | `UseOsoService` | Facts published to Oso Cloud via events | Centralized authorization, cross-service queries |
| Local | `UseOsoService,OsoLocalSecuritySystemLocation` | SQL queries against local database | Reduced latency, offline capability, data locality |

### 2. Conditional Event Publishing

Demonstrates the pattern of conditionally suppressing domain events based on runtime configuration:

```
Profile Active?
    ├── OsoLocalSecuritySystemLocation: Do NOT publish SecuritySystemAssignedToLocation event
    └── (profile not active): Publish event normally
```

### 3. Oso Data Bindings Fact Definition

Shows how to define a fact query in `local_authorization_config.yaml`:

```yaml
"has_relation(SecuritySystem:_, String:location, Location:_)":
  query: 'SELECT id, location_id FROM security_system WHERE location_id IS NOT NULL'
```

This maps the Oso policy's `has_relation(SecuritySystem, "location", Location)` to a direct database query.

### 4. Authorization Flow Comparison

**Cloud-based flow (current):**
```
SecuritySystem created with Location
  → SecuritySystemAssignedToLocation event published
  → oso-integration-service consumes event
  → Oso Cloud stores has_relation fact
  → isAuthorized() queries Oso Cloud
```

**Local authorization flow (new profile):**
```
SecuritySystem created with Location
  → locationId stored in security_system table
  → (no event published)
  → isAuthorized() queries local DB via data bindings
```

## End-to-End Example Flows

### Flow 1: Security System Authorization with Local Data Bindings

**Preconditions:**
- Security system service running with profiles `UseOsoService,OsoLocalSecuritySystemLocation`
- Customer employee has `SECURITY_SYSTEM_DISARMER` role at a location
- Security system exists and is assigned to that location

**Steps:**
1. Customer employee authenticates and receives JWT token
2. Customer employee calls `PUT /securitysystems/{id}/disarm`
3. Security system service calls `isAuthorized(user, "disarm", "SecuritySystem", id)`
4. Oso evaluates policy using local data bindings:
   - Queries `security_system` table for SecuritySystem → Location relationship
   - Uses existing data bindings for Location → Customer and role assignments
5. Authorization succeeds, security system is disarmed
6. Response returned to customer employee

**Verification:**
- No `SecuritySystemAssignedToLocation` events in message broker
- Authorization decision made using local database queries
- Same authorization result as cloud-based mode

### Flow 2: Listing Accessible Security Systems (Unchanged Behavior)

**Preconditions:**
- Same as Flow 1

**Steps:**
1. Customer employee calls `GET /securitysystems`
2. Security system service calls `listLocal(user, "view", "SecuritySystem", "ss.id")`
3. Oso returns SQL WHERE clause based on data bindings
4. Query executed against local database
5. Only authorized security systems returned

**Verification:**
- This flow works identically in both modes (already uses local authorization)
- Demonstrates that `listLocal()` is unaffected by the new profile

### Flow 3: Security System Creation Without Event Publishing

**Preconditions:**
- Security system service running with profiles `UseOsoService,OsoLocalSecuritySystemLocation`
- Orchestration service initiates security system creation for a location

**Steps:**
1. `CreateSecuritySystemCommand` received by security system service
2. Security system created with `locationId` set
3. Security system saved to database
4. `SecuritySystemAssignedToLocation` event is NOT published (profile suppresses it)
5. Reply sent to orchestration service

**Verification:**
- Security system exists in database with correct `locationId`
- No `SecuritySystemAssignedToLocation` event in message broker
- Subsequent authorization calls work correctly using local data bindings

## Capabilities and Constraints

### Capabilities

1. **Profile-based activation**: The feature activates only when both `UseOsoService` AND `OsoLocalSecuritySystemLocation` profiles are active

2. **Backward compatibility**: Existing behavior unchanged when `OsoLocalSecuritySystemLocation` is not active

3. **Same authorization semantics**: Authorization decisions are identical regardless of which mode is used

4. **Contained scope**: All changes within security-system-service; no changes to other services

### Constraints

1. **Requires UseOsoService**: The `OsoLocalSecuritySystemLocation` profile only works in conjunction with `UseOsoService`

2. **Local data only**: When active, the SecuritySystem-Location relationship is only available in the security-system-service's database, not in Oso Cloud

3. **No cross-service queries**: Other services cannot query Oso Cloud for SecuritySystem-Location facts when this profile is active (they were never expected to in the current architecture)

## Scenarios for Steel-Thread Planning

The following scenarios support later steel-thread implementation planning:

### Primary Scenario: Disarm Security System with Local Authorization

A customer employee with the `SECURITY_SYSTEM_DISARMER` role at a location successfully disarms a security system assigned to that location, with authorization evaluated entirely using local database queries.

**Key verification points:**
- `isAuthorized()` uses local data bindings
- No SecuritySystem-Location facts in Oso Cloud
- Authorization succeeds with correct permissions
- Authorization fails with incorrect permissions

### Supporting Scenarios

1. **Arm Security System with Local Authorization**
   - Similar to disarm, but with `SECURITY_SYSTEM_ARMER` role and `arm` action

2. **View Security System with Local Authorization**
   - Customer employee with any security system role can view the system

3. **Authorization Denied with Local Authorization**
   - Customer employee without appropriate role is denied access
   - Verifies that local authorization correctly denies unauthorized requests

4. **Security System Creation Without Event**
   - New security system created when profile active
   - Verify no `SecuritySystemAssignedToLocation` event published
   - Verify subsequent authorization works correctly

5. **List Security Systems (Unchanged)**
   - Verify `findAllAccessible()` continues to work correctly
   - Demonstrates `listLocal()` is unaffected

## Technical Requirements

### Configuration Changes

1. **`local_authorization_config.yaml`** must include:
   ```yaml
   facts:
     "has_relation(SecuritySystem:_, String:location, Location:_)":
       query: 'SELECT id, location_id FROM security_system WHERE location_id IS NOT NULL'

   sql_types:
     Location: integer
     SecuritySystem: integer
   ```

2. **Spring profile**: `OsoLocalSecuritySystemLocation` activates the feature

### Code Changes

1. **Conditional event publishing**: `SecuritySystemServiceImpl.createSecuritySystemWithLocation()` must conditionally skip publishing `SecuritySystemAssignedToLocation` event when profile is active

2. **Profile-aware configuration**: New configuration class(es) activated by `OsoLocalSecuritySystemLocation` profile

## Testing Requirements

Comprehensive testing matching existing patterns:

| Test Type | Purpose | Example |
|-----------|---------|---------|
| Unit tests | Verify conditional event publishing logic | Test that event publisher is/isn't called based on profile |
| Integration tests | Verify Oso data bindings work correctly | Test `isAuthorized()` returns correct results with local data |
| Component tests | Verify service behavior with profile active | Similar to `SecuritySystemServiceComponentTest` but with new profile |
| End-to-end tests | Verify complete authorization flow | New subclass of `AbstractRealGuardioEndToEndTest` |

## Acceptance Criteria

1. **Profile activation works**: When `UseOsoService,OsoLocalSecuritySystemLocation` profiles are active, the service uses local authorization for SecuritySystem-Location relationship

2. **Event suppression works**: No `SecuritySystemAssignedToLocation` events are published when the profile is active

3. **Authorization correctness**: All authorization decisions (arm, disarm, view) produce the same results as cloud-based mode

4. **Backward compatibility**: Existing behavior unchanged when `OsoLocalSecuritySystemLocation` profile is not active

5. **Tests pass**: All unit, integration, component, and end-to-end tests pass

6. **Educational clarity**: Code is well-structured and demonstrates the patterns clearly for learning purposes

## Change History

### 2026-01-03: Initial specification

Created didactic example specification based on brainstorming discussion. Classified as Educational/Example type. Key decisions:
- Additive profile `OsoLocalSecuritySystemLocation` works with `UseOsoService`
- Scope contained to security-system-service only
- Comprehensive testing required including E2E and component tests
