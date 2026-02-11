# Local Authorization with Security System Location - Discussion

## Initial Idea

From the idea file:
> I want an optional profile OsoLocalSecuritySystemLocation that's used with UseOsoService.
> When active, the security service does not publish Security System Location information to Oso.
> Instead, it uses Oso Local authorization and queries its DB for SecuritySystem-Location information.

## Architecture Context (from codebase analysis)

### Two Oso Authorization Mechanisms in Use

**1. `isAuthorized()` - Per-Action Authorization (Cloud-based)**
- Used by `OsoSecuritySystemActionAuthorizer.verifyCanDo()` for arm/disarm/view operations
- Calls `realGuardOsoAuthorizer.isAuthorized(user, action, "SecuritySystem", securitySystemId)`
- **Requires Oso Cloud to know SecuritySystem -> Location relationship**
- Authorization flow: Can user X perform action Y on SecuritySystem Z?

**2. `listLocal()` - Query-Level Authorization (Local data bindings)**
- Used by `SecuritySystemRepositoryWithOsoImpl.findAllAccessible()` for listing
- Calls `realGuardOsoAuthorizer.listLocal(userName, "view", "SecuritySystem", "ss.id")`
- Returns SQL WHERE clause that filters query results
- **Already uses local database via data bindings in `local_authorization_config.yaml`**

### Current Fact Publishing Flow
1. When SecuritySystem is assigned to Location, `SecuritySystemAssignedToLocation` event is published
2. `SecuritySystemEventConsumer` in oso-event-subscribers consumes this event
3. It calls `osoFactManager.assignSecuritySystemToLocation(securitySystemId, locationId)`
4. This creates a `has_relation` fact in Oso Cloud: SecuritySystem -> location -> Location

### Authorization Check Points

| Endpoint | Check Type | Method Called |
|----------|-----------|---------------|
| GET /securitysystems | listLocal() | findAllAccessible() - already local |
| GET /securitysystems/{id} | isAuthorized() | verifyCanDo("view") - uses Oso Cloud |
| PUT arm | isAuthorized() | verifyCanDo("arm") - uses Oso Cloud |
| PUT disarm | isAuthorized() | verifyCanDo("disarm") - uses Oso Cloud |

### Domain Model
- SecuritySystem has a `locationId` field stored in the security system service database
- The SecuritySystem-Location relationship is already in the local DB

## Questions and Answers

### Q1: Understanding Confirmation

**Question:** I now see that authorization uses two different Oso mechanisms:
1. `listLocal()` for GET /securitysystems - already uses local DB via data bindings
2. `isAuthorized()` for view/arm/disarm on specific security systems - requires SecuritySystem-Location facts in Oso Cloud

Currently, when a security system is assigned to a location, the `SecuritySystemEventConsumer` publishes a `has_relation(SecuritySystem, location, Location)` fact to Oso Cloud. This fact is needed for `isAuthorized()` calls to work.

My understanding of your feature: With the `OsoLocalSecuritySystemLocation` profile active:
- Stop publishing SecuritySystem-Location facts to Oso Cloud
- Make `isAuthorized()` calls also use local data bindings for the SecuritySystem-Location relationship

Is this understanding correct?

**Answer:** Yes - this is exactly what is wanted.

---

### Q2: Classification

**Question:** What is the primary purpose of this work?
- A. Architecture POC - Validating that Oso's local authorization works correctly for `isAuthorized()` calls
- B. Platform capability - Enabling a production-ready deployment option
- C. Educational/Example - Demonstrating how to configure Oso local authorization
- D. Something else

**Answer:** C - Educational/Example - demonstrating how to configure Oso local authorization for different use cases in a learning context.

**Classification:** Educational/Example Repository

---

### Q3: Scope of Changes

**Question:** When `OsoLocalSecuritySystemLocation` is active, which services need to change?
- A. Security-system-service only - configure local data bindings; fact can still be published
- B. Both services - security-system-service uses local data bindings, oso-integration-service stops publishing fact
- C. Security-system-service, with conditional event publishing - the security-system-service should conditionally not publish the `SecuritySystemAssignedToLocation` event when this profile is active

**Answer:** C - Security-system-service with conditional event publishing. The change is contained within security-system-service: it uses local data bindings for authorization AND conditionally does not publish the `SecuritySystemAssignedToLocation` event.

---

### Q4: Testing Requirements

**Question:** Since this is an educational example, what level of testing is needed?
- A. Minimal - Just verify the configuration works
- B. Moderate - Integration tests covering main authorization scenarios
- C. Comprehensive - Unit tests, integration tests, AND end-to-end tests
- D. Parallel parity - Tests should mirror existing Oso Cloud tests

**Answer:** C - Comprehensive testing, including a new subclass of `AbstractRealGuardioEndToEndTest` that exercises the feature with the new profile active. This follows the existing testing patterns in the codebase.

**Additional note:** Component tests should also be included if applicable. The existing `SecuritySystemServiceComponentTest` uses `@ActiveProfiles("UseOsoService")` - a similar component test with the new profile could verify local authorization behavior.

---

### Q5: Profile Configuration

**Question:** The new profile is additive - works alongside `UseOsoService`, not as a replacement. So the Spring profiles would be:
- Current Oso Cloud mode: `UseOsoService`
- New local authorization mode: `UseOsoService,OsoLocalSecuritySystemLocation`

Is this correct?

**Answer:** A - Yes, `OsoLocalSecuritySystemLocation` is an additional profile that modifies behavior when `UseOsoService` is also active.

---

## Summary of Refined Idea

Based on our discussion, here is the refined understanding:

**Feature:** Optional profile `OsoLocalSecuritySystemLocation` for local authorization of SecuritySystem-Location relationship

**Classification:** Educational/Example

**Profile Configuration:** Additive - `UseOsoService,OsoLocalSecuritySystemLocation`

**Behavior when active:**
1. Security-system-service does NOT publish `SecuritySystemAssignedToLocation` events
2. `isAuthorized()` calls use Oso local data bindings to query the `security_system` table
3. `listLocal()` calls continue to work as before (already use local data bindings)

**Scope:** Changes contained within security-system-service only

**Testing:** Comprehensive - unit tests, integration tests, component tests (if applicable), and a new E2E test subclass

---

### Final Check

**Question:** Are there any additional requirements or concerns before we move to the next step (creating the detailed specification)?

**Answer:** Component tests should also be included if applicable. No other additional requirements.

---

## Ready for Next Step

All relevant details have been gathered. The discussion phase is complete and ready to proceed to specification creation.

