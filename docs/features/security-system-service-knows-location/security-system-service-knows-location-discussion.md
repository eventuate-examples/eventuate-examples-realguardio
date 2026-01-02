# Security System Service Knows Location - Discussion

## Initial Idea

1. Locations are explicitly created in the Customer Service via a new REST endpoint
2. The SecuritySystem-Location relationship is maintained by the Security System Service - not the Customer Service

## Current Architecture (from codebase analysis)

### Location Entity (Customer Service)
- **Owner**: Customer Service (`customer-service-domain`)
- **Fields**: `id`, `name`, `customerId`, `securitySystemId` (optional)
- **Constraint**: A Location can have at most ONE security system
- **Creation**: Only via saga command handler (`CreateLocationWithSecuritySystemCommand`) - no direct REST endpoint

### SecuritySystem Entity (Security System Service)
- **Owner**: Security System Service (`security-system-service-domain`)
- **Fields**: `id`, `locationName`, `locationId`, `state`, `rejectionReason`, `version`
- **States**: `CREATION_PENDING` → `DISARMED` ↔ `ARMED` → `ALARMED` (or `CREATION_FAILED`)

### Current Relationship
- **Bidirectional references** across services:
  - Location has `securitySystemId`
  - SecuritySystem has `locationId` and `locationName`
- **Established via CreateSecuritySystemSaga** (3 steps):
  1. Create SecuritySystem (CREATION_PENDING)
  2. Create Location with SecuritySystem reference
  3. Update SecuritySystem with locationId (state → DISARMED)

### Key Observation
Currently, Location and SecuritySystem are **always created together** via the saga. There is no way to:
- Create a Location independently
- Assign a SecuritySystem to an existing Location

## Questions and Answers

### Q1: Decoupling Location from SecuritySystem Creation

Based on my analysis, the proposed change would allow Locations to exist independently of SecuritySystems. This implies:
- A Customer can have Locations without any SecuritySystem
- A SecuritySystem can later be assigned to an existing Location

Is this the intended behavior, or is there a different goal?

A. Yes - Locations should be able to exist without SecuritySystems
B. No - Locations and SecuritySystems are always 1:1, but we want to change who owns the relationship
C. Other (please explain)

**Answer**: The proposed idea is:
1. First create a Location for a Customer (independently)
2. Creating a SecuritySystem takes the location as a parameter - it does not create the location

This means Locations can exist without SecuritySystems (at least temporarily, before a SecuritySystem is assigned).

### Q2: Relationship Ownership

Currently, both services maintain a reference to each other:
- **Customer Service**: Location has `securitySystemId` field
- **Security System Service**: SecuritySystem has `locationId` field

The idea states the relationship should be "maintained by the Security System Service - not the Customer Service."

Does this mean:

A. Remove `securitySystemId` from Location entirely - only SecuritySystem knows the relationship
B. Keep `securitySystemId` in Location but as a read-only replica (populated by consuming events from Security System Service)
C. Other (please explain)

**Answer**: A - Remove `securitySystemId` from Location entirely. Only SecuritySystem knows the relationship.

**Implications**:
- Location entity becomes simpler (no `securitySystemId` field)
- `addSecuritySystem()` method on Location is removed
- The "one security system per location" constraint must be enforced by Security System Service
- Customer Service no longer publishes `SecuritySystemAssignedToLocation` event

### Q3: SecuritySystem Creation API

When creating a SecuritySystem, how should the Location be specified?

Currently SecuritySystem stores both `locationName` and `locationId`. With the new design where Location is created first:

A. Pass `locationId` only - SecuritySystem references Location by ID
B. Pass `customerId` and `locationName` - SecuritySystem Service looks up the Location (requires a replica or API call)
C. Pass both `locationId` and `locationName` - for convenience/display purposes
D. Other (please explain)

**Answer**: A - Pass `locationId` only.

*Note: This was an obvious conclusion - since Locations are created first and have IDs, passing locationId is the natural choice.*

### Derived Conclusions

Based on the decisions so far, the following can be derived:

1. **Saga simplification**: The `CreateSecuritySystemSaga` becomes simpler or unnecessary. Since Location exists first, creating a SecuritySystem is potentially a single-service operation (just create SecuritySystem with locationId reference).

2. **No more CREATION_PENDING state?**: Currently SecuritySystem starts in `CREATION_PENDING` because the saga creates Location after. If Location already exists, SecuritySystem could start directly in `DISARMED` state.

### Q4: One SecuritySystem Per Location Constraint

Currently, the Customer Service enforces "one security system per location" via `addSecuritySystem()` which throws `LocationAlreadyHasSecuritySystemException`.

With the relationship now owned by Security System Service, should this constraint still be enforced?

A. Yes - enforce "one SecuritySystem per Location" in Security System Service (reject if locationId already has a SecuritySystem)
B. No - allow multiple SecuritySystems per Location
C. This is a future consideration - for now, don't enforce it

**Answer**: A - Enforce "one SecuritySystem per Location" in Security System Service.

### Q5: SecuritySystem Creation Flow

With Locations created separately, what is the intended flow for creating a SecuritySystem?

A. Client calls Security System Service directly (no saga needed) - Security System Service validates locationId exists via CQRS replica of Locations
B. Client calls Orchestration Service, which orchestrates validation and creation (saga still exists but simpler)
C. Other (please explain)

**Answer**: B - The saga must validate locationId. Orchestration Service still orchestrates the flow.

### Derived: New Saga Structure

The `CreateSecuritySystemSaga` becomes:
1. **Step 1**: Validate locationId exists (command to Customer Service)
2. **Step 2**: Create SecuritySystem with locationId (command to Security System Service)

No compensation needed for Step 1 (validation only). If Step 2 fails, no Location cleanup needed since Location was pre-existing.

### Q6: Location Details in SecuritySystem

Currently, SecuritySystem stores `locationName` (in addition to `locationId`) for display purposes. With the new design:

A. Keep `locationName` in SecuritySystem - saga fetches it during validation and passes it to SecuritySystem creation
B. Remove `locationName` from SecuritySystem - only store `locationId`, fetch name on-demand when needed for display
C. Security System Service maintains a CQRS replica of Locations (consumes `LocationCreatedForCustomer` events)

**Answer**: A - Keep `locationName` in SecuritySystem. The saga fetches it during validation and passes it to SecuritySystem creation.

### Q7: Location Creation REST Endpoint

The new REST endpoint for creating Locations in Customer Service - what should the URL pattern be?

A. `POST /customers/{customerId}/locations` with body `{ "name": "..." }` - Location as a sub-resource of Customer
B. `POST /locations` with body `{ "customerId": ..., "name": "..." }` - Location as a top-level resource
C. Other (please explain)

**Answer**: A - `POST /customers/{customerId}/locations` with body `{ "name": "..." }`

## Summary of Decisions

| Aspect | Decision |
|--------|----------|
| Location creation | New REST endpoint: `POST /customers/{customerId}/locations` |
| Relationship ownership | Security System Service only (remove `securitySystemId` from Location) |
| SecuritySystem creation input | `locationId` (not locationName) |
| One-to-one constraint | Enforced by Security System Service |
| SecuritySystem creation flow | Orchestration Service saga: (1) validate locationId, (2) create SecuritySystem |
| Location details in SecuritySystem | Keep `locationName` - saga fetches during validation |

### Derived Implementation Changes

1. **Customer Service**:
   - Add `POST /customers/{customerId}/locations` REST endpoint
   - Add command handler for validating Location exists (returns locationId + locationName)
   - Remove `securitySystemId` field from Location entity
   - Remove `addSecuritySystem()` method from Location
   - Remove `SecuritySystemAssignedToLocation` event

2. **Security System Service**:
   - Modify SecuritySystem creation to take `locationId` and `locationName`
   - Enforce one SecuritySystem per locationId constraint
   - Remove `CREATION_PENDING` state (SecuritySystem starts as `DISARMED`)
   - Remove `noteLocationCreated()` method

3. **Orchestration Service**:
   - Modify `CreateSecuritySystemRequest` to take `locationId` instead of `locationName`
   - Refactor `CreateSecuritySystemSaga`:
     - Step 1: Validate locationId (get locationName)
     - Step 2: Create SecuritySystem with locationId + locationName

## Classification

**Type: A. User-facing feature**

**Rationale**: This change introduces new user-visible functionality:
- A new REST endpoint for creating Locations independently
- Changes to the SecuritySystem creation flow (now requires an existing Location)
- Visible in the API contract and user workflows

While it involves architectural changes (moving relationship ownership), the primary purpose is to enable a new user workflow where Locations are created before SecuritySystems.

## Implementation Constraint: No Breaking Changes

**CRITICAL**: The sequence of changes must be backwards compatible:
- Add new functionality before removing old functionality
- Only remove old code paths when they are no longer used
- Each deployment should be safe with no breaking changes

**Example sequencing approach**:
1. Add new Location REST endpoint (additive, no breaking change)
2. Add new saga step for validating locationId (keep old flow working)
3. Add support for locationId-based SecuritySystem creation (alongside existing locationName flow)
4. Migrate clients to new flow
5. Only then remove old code paths (securitySystemId from Location, old saga steps, etc.)

## Oso Integration Impact

### Current Oso Fact Creation

| Event | Source | Oso Fact Created |
|-------|--------|------------------|
| `LocationCreatedForCustomer` | Customer Service | `has_relation(Location, "customer", Customer)` |
| `SecuritySystemAssignedToLocation` | Customer Service | `has_relation(SecuritySystem, "location", Location)` |

The Oso policy uses these facts to determine permissions:
- SecuritySystem permissions check roles on the related Location
- Location roles can be inherited from Customer

### Required Changes

**The Oso policy itself does NOT need to change** - it will work as long as facts are correctly populated.

The key change is **where the SecuritySystem-Location relationship event is published**:

| Aspect | Current | After Change |
|--------|---------|--------------|
| Event Publisher | Customer Service | Security System Service |
| Event Channel | `...customerservice...domain.Customer` | `...securitysystemservice...domain.SecuritySystem` |

### Oso Integration Changes Required

1. **Security System Service**: Publish `SecuritySystemAssignedToLocation` event when SecuritySystem is created with a locationId

2. **OSO Integration Service**: Update `CustomerEventConsumer` to listen for `SecuritySystemAssignedToLocation` from Security System Service's event channel (not Customer Service)

3. **Backwards-compatible migration**:
   - First: Add new event listener for Security System Service events
   - Later: Remove old listener after Customer Service stops publishing the event

