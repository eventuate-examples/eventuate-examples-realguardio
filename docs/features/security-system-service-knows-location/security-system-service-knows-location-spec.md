# Security System Service Knows Location - Specification

## Purpose and Background

### Purpose

Enable independent creation of Locations before SecuritySystems are assigned, and move the SecuritySystem-Location relationship ownership from Customer Service to Security System Service.

### Background

Currently, the RealGuardIO system creates Locations and SecuritySystems together via a saga orchestrated by the Orchestration Service. This tight coupling means:

1. Locations cannot exist without a SecuritySystem
2. Both Customer Service and Security System Service maintain references to each other (bidirectional relationship)
3. The relationship constraint ("one SecuritySystem per Location") is enforced by Customer Service

This change decouples Location creation from SecuritySystem creation, simplifies the domain model, and clarifies ownership of the relationship.

## Target Users and Personas

| Persona | Description | Relevance |
|---------|-------------|-----------|
| RealGuardIO Administrator | Manages customers, locations, and security systems | Primary user - will use the new Location creation endpoint |
| Customer Employee | Operates security systems at assigned locations | Affected by authorization - no workflow changes |
| API Consumer | External systems integrating with RealGuardIO | Must update integration to use new flow |

## Problem Statement and Goals

### Problem Statement

The current design couples Location and SecuritySystem creation, preventing the following use cases:

1. A customer cannot define their physical locations before deciding on security system deployment
2. The bidirectional relationship between services creates unnecessary complexity
3. The constraint enforcement ("one SecuritySystem per Location") is in the wrong service - Customer Service shouldn't know about SecuritySystems

### Goals

1. **Decouple Location lifecycle** - Locations can be created independently and exist without a SecuritySystem
2. **Simplify domain model** - Remove `securitySystemId` from Location entity
3. **Clarify ownership** - Security System Service owns the SecuritySystem-Location relationship
4. **Maintain authorization** - Oso-based authorization continues to work correctly
5. **No breaking changes** - Implementation must be backwards-compatible at each step

## In-Scope and Out-of-Scope

### In-Scope

1. New REST endpoint for creating Locations: `POST /customers/{customerId}/locations`
2. Modify SecuritySystem creation to take `locationId` as input
3. Refactor `CreateSecuritySystemSaga` to validate locationId and create SecuritySystem
4. Move relationship ownership to Security System Service
5. Update Oso integration to consume events from Security System Service
6. Enforce "one SecuritySystem per Location" constraint in Security System Service

### Out-of-Scope

1. Location update/delete operations (future work)
2. Multiple SecuritySystems per Location (explicitly rejected)
3. UI/BFF changes (may be addressed separately)
4. Location listing/query endpoints (may be addressed separately)

## High-Level Functional Requirements

### FR-1: Location Creation

| ID | Requirement |
|----|-------------|
| FR-1.1 | Customer Service SHALL expose `POST /customers/{customerId}/locations` endpoint |
| FR-1.2 | The endpoint SHALL accept `{ "name": "..." }` as the request body |
| FR-1.3 | The endpoint SHALL return the created `locationId` |
| FR-1.4 | The endpoint SHALL publish `LocationCreatedForCustomer` event |
| FR-1.5 | A Location SHALL be created in `ACTIVE` state (no pending state needed) |

### FR-2: SecuritySystem Creation

| ID | Requirement |
|----|-------------|
| FR-2.1 | Orchestration Service SHALL accept `locationId` (not `locationName`) when creating a SecuritySystem |
| FR-2.2 | The saga SHALL validate that the `locationId` exists by querying Customer Service |
| FR-2.3 | The saga SHALL retrieve `locationName` during validation |
| FR-2.4 | Security System Service SHALL create SecuritySystem with `locationId` and `locationName` |
| FR-2.5 | SecuritySystem SHALL start in `DISARMED` state (no `CREATION_PENDING` needed) |
| FR-2.6 | Security System Service SHALL publish `SecuritySystemAssignedToLocation` event |

### FR-3: Relationship Constraints

| ID | Requirement |
|----|-------------|
| FR-3.1 | Security System Service SHALL enforce "one SecuritySystem per Location" constraint |
| FR-3.2 | Creating a SecuritySystem for an already-assigned Location SHALL fail with an error |
| FR-3.3 | Customer Service SHALL NOT maintain `securitySystemId` on Location (after migration) |

### FR-4: Authorization (Oso Integration)

| ID | Requirement |
|----|-------------|
| FR-4.1 | OSO Integration Service SHALL consume `SecuritySystemAssignedToLocation` from Security System Service |
| FR-4.2 | The Oso fact `has_relation(SecuritySystem, "location", Location)` SHALL be created correctly |
| FR-4.3 | Existing authorization rules SHALL continue to work without policy changes |

## Non-Functional Requirements

### NFR-1: Backwards Compatibility

| ID | Requirement |
|----|-------------|
| NFR-1.1 | Each deployment SHALL be backwards compatible with the previous version |
| NFR-1.2 | New functionality SHALL be added before removing old functionality |
| NFR-1.3 | Old code paths SHALL only be removed after all clients have migrated |

### NFR-2: Performance

| ID | Requirement |
|----|-------------|
| NFR-2.1 | Location creation SHALL complete within 500ms (p99) |
| NFR-2.2 | SecuritySystem creation flow SHALL not degrade compared to current performance |

### NFR-3: Reliability

| ID | Requirement |
|----|-------------|
| NFR-3.1 | Saga compensation SHALL work correctly if SecuritySystem creation fails |
| NFR-3.2 | Event publishing SHALL be reliable (at-least-once delivery) |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| API adoption | 100% of clients use new locationId-based flow | API logs / deprecation warnings |
| Authorization correctness | 0 authorization failures due to missing facts | Oso Cloud audit logs |
| Backwards compatibility | 0 breaking changes during rollout | Deployment verification tests |

## Epics and User Stories

### Epic 1: Location Creation Endpoint

**E1-US1**: As an administrator, I want to create a Location for a Customer without specifying a SecuritySystem, so that I can define physical locations before deploying security systems.

**Acceptance Criteria:**
- `POST /customers/{customerId}/locations` with `{ "name": "Main Office" }` creates a Location
- Response includes the `locationId`
- Location exists independently (can be queried later)

### Epic 2: SecuritySystem Creation with LocationId

**E2-US1**: As an administrator, I want to create a SecuritySystem for an existing Location by providing its locationId, so that I can deploy security systems to pre-defined locations.

**Acceptance Criteria:**
- `POST /securitysystems` with `{ "locationId": 123 }` creates a SecuritySystem
- The saga validates that locationId 123 exists
- SecuritySystem is created in `DISARMED` state
- SecuritySystem is linked to the Location

**E2-US2**: As an administrator, I want to be prevented from creating multiple SecuritySystems for the same Location, so that the system maintains data integrity.

**Acceptance Criteria:**
- Attempting to create a second SecuritySystem for a Location fails with an error
- The first SecuritySystem remains unaffected

### Epic 3: Authorization Continuity

**E3-US1**: As a customer employee, I want my existing location-based permissions to continue working, so that I can arm/disarm security systems at my assigned locations.

**Acceptance Criteria:**
- Employee with `SECURITY_SYSTEM_ARMER` role at a Location can arm the SecuritySystem
- No changes to the Oso policy are required
- Facts are correctly populated from Security System Service events

## User-Facing Scenarios

### Scenario 1: Create Location and SecuritySystem (Primary End-to-End Scenario)

**Preconditions:**
- Customer "Acme Corp" exists with `customerId = 1`

**Steps:**
1. Administrator creates a Location:
   - `POST /customers/1/locations` with `{ "name": "Main Office" }`
   - Response: `{ "locationId": 100 }`

2. Administrator creates a SecuritySystem for the Location:
   - `POST /securitysystems` with `{ "locationId": 100 }`
   - Response: `{ "securitySystemId": 200 }` (async, returns 202)

3. SecuritySystem is created and linked to Location:
   - SecuritySystem 200 is in `DISARMED` state
   - SecuritySystem 200 has `locationId = 100` and `locationName = "Main Office"`

4. Authorization works:
   - Employee with `SECURITY_SYSTEM_ARMER` role at Location 100 can arm SecuritySystem 200

**Postconditions:**
- Location 100 exists for Customer 1
- SecuritySystem 200 is linked to Location 100
- Oso fact `has_relation(SecuritySystem:200, "location", Location:100)` exists

### Scenario 2: Location Exists Without SecuritySystem

**Preconditions:**
- Customer "Acme Corp" exists with `customerId = 1`

**Steps:**
1. Administrator creates a Location:
   - `POST /customers/1/locations` with `{ "name": "Warehouse" }`
   - Response: `{ "locationId": 101 }`

2. Location exists without a SecuritySystem (no further action taken)

**Postconditions:**
- Location 101 exists for Customer 1
- No SecuritySystem is associated with Location 101
- Location can later have a SecuritySystem assigned

### Scenario 3: Prevent Duplicate SecuritySystem Assignment

**Preconditions:**
- Customer 1 has Location 100
- SecuritySystem 200 already exists for Location 100

**Steps:**
1. Administrator attempts to create another SecuritySystem for Location 100:
   - `POST /securitysystems` with `{ "locationId": 100 }`
   - Response: Error - "Location already has a SecuritySystem"

**Postconditions:**
- Only one SecuritySystem (200) exists for Location 100
- No duplicate SecuritySystem was created

### Scenario 4: SecuritySystem Creation with Invalid LocationId

**Preconditions:**
- Location 999 does not exist

**Steps:**
1. Administrator attempts to create a SecuritySystem:
   - `POST /securitysystems` with `{ "locationId": 999 }`
   - Response: Error - "Location not found"

**Postconditions:**
- No SecuritySystem was created
- Error message indicates the Location does not exist

## Technical Changes Summary

### Customer Service

| Change | Type |
|--------|------|
| Add `POST /customers/{customerId}/locations` REST endpoint | Addition |
| Add command handler for `ValidateLocationCommand` | Addition |
| Remove `securitySystemId` field from Location entity | Removal (after migration) |
| Remove `addSecuritySystem()` method | Removal (after migration) |
| Stop publishing `SecuritySystemAssignedToLocation` | Removal (after migration) |

### Security System Service

| Change | Type |
|--------|------|
| Accept `locationId` + `locationName` in SecuritySystem creation | Modification |
| Enforce one-SecuritySystem-per-Location constraint | Addition |
| Publish `SecuritySystemAssignedToLocation` event | Addition |
| Remove `CREATION_PENDING` state | Removal (after migration) |
| Remove `noteLocationCreated()` method | Removal (after migration) |

### Orchestration Service

| Change | Type |
|--------|------|
| Accept `locationId` in `CreateSecuritySystemRequest` | Modification |
| Refactor `CreateSecuritySystemSaga` to validate + create | Modification |

### OSO Integration Service

| Change | Type |
|--------|------|
| Add event listener for Security System Service events | Addition |
| Remove listener for Customer Service `SecuritySystemAssignedToLocation` | Removal (after migration) |

## Implementation Constraint: Backwards-Compatible Migration

The implementation MUST follow this sequencing:

1. **Phase 1: Additive Changes**
   - Add Location REST endpoint
   - Add `ValidateLocationCommand` handler
   - Add Security System Service event publisher
   - Add OSO event listener for Security System Service

2. **Phase 2: New Flow Support**
   - Support `locationId` in saga (alongside existing flow)
   - Start SecuritySystem in `DISARMED` when using new flow

3. **Phase 3: Client Migration**
   - Migrate clients to use new `locationId`-based flow
   - Verify all clients have migrated

4. **Phase 4: Removal**
   - Remove old saga flow
   - Remove `securitySystemId` from Location
   - Remove old event listeners
   - Remove `CREATION_PENDING` state
