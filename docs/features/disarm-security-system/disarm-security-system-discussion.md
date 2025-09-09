# Security System Arm/Disarm Feature Discussion

## Original Idea

Add a PUT /securitysystems/{securitySystemID} endpoint to the Security Service for arming/disarming security systems with role-based access control.

Key requirements:
- Requires REALGUARDIO_CUSTOMER_EMPLOYEE or REALGUARDIO_ADMIN role
- Customer employees need location-specific permission verification
- Requires new GET endpoint in CustomerService for role retrieval
- Changes SecuritySystem state upon authorization

---

## Questions and Answers

### Q1: What specific operations should the PUT endpoint support?

Given the existing SecuritySystemAction enum (ARM, DISARM, ACKNOWLEDGE), which operations should the endpoint support?

A) ARM and DISARM only
B) ARM, DISARM, and ACKNOWLEDGE  
C) Just state changes (transition to any SecuritySystemState: ARMED, DISARMED, ALARMED)
D) Custom - please specify

**Default suggestion:** Option A (ARM and DISARM only) - These are the primary user operations. ACKNOWLEDGE might be handled separately for alarm acknowledgment, and ALARMED state would be triggered by sensors/events rather than user action.

**Answer:** A - ARM and DISARM only

---

### Q2: How should the SecuritySystem be identified in the endpoint path?

The SecuritySystem entity has:
- `id` (Long) - database primary key
- `locationName` (String) - location identifier

Which should be used in the endpoint path?

A) Use the database ID: `/securitysystems/{id}` (e.g., `/securitysystems/123`)
B) Use the location name: `/securitysystems/{locationName}` (e.g., `/securitysystems/warehouse-1`)
C) Support both with different endpoints
D) Custom - please specify

**Default suggestion:** Option A - Using the database ID is RESTful, unambiguous, and avoids issues with special characters in location names.

**Answer:** A - Use database ID

---

### Q3: What should the request body format be for the arm/disarm operation?

A) Action-based: `{"action": "ARM"}` or `{"action": "DISARM"}`
B) State-based: `{"state": "ARMED"}` or `{"state": "DISARMED"}`
C) Boolean flag: `{"armed": true}` or `{"armed": false}`
D) Custom - please specify

**Default suggestion:** Option A - Action-based approach clearly indicates the operation being performed and aligns with the existing SecuritySystemAction enum.

**Answer:** A - Action-based: `{"action": "ARM"}` or `{"action": "DISARM"}`

---

### Q4: How should the CustomerService endpoint for retrieving user roles be structured?

For verifying REALGUARDIO_CUSTOMER_EMPLOYEE permissions, we need to check their location-specific roles. What should the endpoint look like?

A) `GET /users/{userId}/locations/{locationName}/roles` - Returns roles for a specific user at a specific location
B) `GET /users/{userId}/roles?locationName={locationName}` - Query parameter approach  
C) `GET /locations/{locationName}/users/{userId}/roles` - Location-first approach
D) `GET /users/{userId}/locations` - Returns all locations and roles for the user

**Default suggestion:** Option A - Clear RESTful path that directly expresses the relationship between user, location, and roles.

**Answer:** Customer clarification - Will add locationId to SecuritySystem, and SecuritySystem will forward JWT to CustomerService (no userId in path needed)

Updated endpoint: `GET /locations/{locationId}/roles` - Returns the current user's roles at the specified location (user identity extracted from JWT)

---

### Q5: What specific roles should a REALGUARDIO_CUSTOMER_EMPLOYEE have at a location to perform ARM/DISARM operations?

A) Single role `SECURITY_OPERATOR` - allows both ARM and DISARM
B) Separate roles: `SECURITY_SYSTEM_ARMER` and `SECURITY_SYSTEM_DISARMER` - granular control
C) Role hierarchy: `SECURITY_MANAGER` (can do both), `SECURITY_GUARD` (can only disarm)
D) Custom - please specify

**Default suggestion:** Option A - A single `SECURITY_OPERATOR` role keeps it simple while providing the necessary access control at the location level.

**Answer:** B - Separate roles: `SECURITY_SYSTEM_ARMER` and `SECURITY_SYSTEM_DISARMER` for granular control

---

### Q6: What should the response format be for successful and unsuccessful operations?

A) Success: 200 OK with updated SecuritySystem JSON; Errors: 403 Forbidden (no permission), 404 Not Found, 400 Bad Request
B) Success: 204 No Content (no body); Errors: Same as A
C) Success: 200 OK with operation result `{"success": true, "newState": "ARMED"}`; Errors: JSON error responses
D) Custom - please specify

**Default suggestion:** Option A - Return the updated SecuritySystem entity following REST conventions, allowing clients to see the new state.

**Answer:** Upon review of the domain model, the authorization structure is:
- CustomerEmployee belongs to Teams (many-to-many via teamIds)
- Teams have TeamLocationRoles (roles at specific locations)
- CustomerEmployeeLocationRole provides direct employee-to-location role mappings

So employees can have roles at locations through:
1. Direct assignment via CustomerEmployeeLocationRole
2. Team membership (Team has roles at locations)

---

### Q6 (continued): Given the authorization model, how should we aggregate roles for authorization?

Since employees can have roles at locations through both:
1. Direct assignment (CustomerEmployeeLocationRole)
2. Team membership (Teams have TeamLocationRoles)

When checking if a user can ARM/DISARM, should we:

A) Check BOTH direct roles AND team roles - user needs `SECURITY_SYSTEM_ARMER`/`SECURITY_SYSTEM_DISARMER` from either source
B) Direct roles only - ignore team roles for security operations
C) Team roles take precedence - check team roles first, fall back to direct roles
D) Require the role from BOTH sources for extra security

**Default suggestion:** Option A - Aggregate roles from both sources. If the user has the required role (`SECURITY_SYSTEM_ARMER` or `SECURITY_SYSTEM_DISARMER`) from either their direct assignment OR their team membership, allow the operation.

**Answer:** The CustomerService endpoint should return the union of all roles (direct via CustomerEmployeeLocationRole and indirect via Team membership)

---

### Q7: What should the response format be for successful and unsuccessful operations?

A) Success: 200 OK with updated SecuritySystem JSON; Errors: 403 Forbidden (no permission), 404 Not Found, 400 Bad Request  
B) Success: 204 No Content (no body); Errors: Same as A
C) Success: 200 OK with operation result `{"success": true, "newState": "ARMED"}`; Errors: JSON error responses
D) Custom - please specify

**Default suggestion:** Option A - Return the updated SecuritySystem entity following REST conventions, allowing clients to see the new state including the locationId.

**Answer:** A - Success returns 200 OK with updated SecuritySystem JSON

---

### Q8: How should the SecurityService communicate with the CustomerService?

A) Direct REST API call using RestTemplate/WebClient  
B) Synchronous messaging via event sourcing (request-reply pattern)
C) Feign client for declarative REST calls
D) gRPC for efficient service-to-service communication

**Default suggestion:** Option A - Direct REST API call using RestTemplate or WebClient is straightforward and commonly used in Spring Boot microservices.

**Answer:** A - Direct REST API call using RestTemplate/WebClient

---

### Q9: How should we handle the case when SecuritySystem doesn't have a locationId yet?

Since we're adding locationId as a new field to SecuritySystem, existing records won't have it. How should we handle this?

A) Return 400 Bad Request if locationId is null - require migration first
B) Allow REALGUARDIO_ADMIN to arm/disarm without location check, deny for CUSTOMER_EMPLOYEE
C) Treat null locationId as "location 0" with special handling
D) Add a default locationId during migration and make it non-nullable

**Default suggestion:** Option B - Allow admins to manage legacy systems while requiring location-based auth for customer employees.

**Answer:** A - Return 400 Bad Request if locationId is null (system not properly configured)

---

### Q10: Should we add audit logging for arm/disarm operations?

A) No logging - keep it simple
B) Basic logging - log operation, user, timestamp to application logs
C) Database audit table - create SecuritySystemAudit entity to track all operations
D) Event sourcing - emit domain events for each operation

**Default suggestion:** Option B - Basic logging using standard application logging (slf4j) to track who performed what operation and when.

**Answer:** B - Basic logging to application logs

---

### Q11: What should be the exact response format from the CustomerService GET /locations/{locationId}/roles endpoint?

A) Simple array: `["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER", "OTHER_ROLE"]`
B) Object with roles array: `{"roles": ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER"]}`
C) Detailed object: `{"locationId": 123, "userId": 456, "roles": ["SECURITY_SYSTEM_ARMER"], "source": ["direct", "team"]}`
D) Custom - please specify

**Default suggestion:** Option A - Simple array of role names is sufficient for the authorization check.

**Answer:** B - Object with roles array: `{"roles": ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER"]}`

---

## Summary of Specification

Based on our discussion, here's the complete specification for the arm/disarm security system feature:

### 1. Security Service Endpoint

**Endpoint:** `PUT /securitysystems/{id}`
- **Path Parameter:** `id` - SecuritySystem database ID (Long)
- **Request Body:** `{"action": "ARM"}` or `{"action": "DISARM"}`
- **Authorization Required:** Bearer token with either:
  - `REALGUARDIO_ADMIN` role (full access)
  - `REALGUARDIO_CUSTOMER_EMPLOYEE` role (location-based access)

### 2. SecuritySystem Entity Changes

Add new field to SecuritySystem:
- `locationId` (Long) - References the location this system monitors
- If `locationId` is null, return 400 Bad Request (system not configured)

### 3. Authorization Flow

For users with `REALGUARDIO_CUSTOMER_EMPLOYEE` role:
1. Extract user identity from JWT token
2. Get SecuritySystem's locationId
3. Call CustomerService: `GET /locations/{locationId}/roles` with forwarded JWT
4. Check if returned roles include:
   - `SECURITY_SYSTEM_ARMER` for ARM action
   - `SECURITY_SYSTEM_DISARMER` for DISARM action
5. If authorized, proceed with operation
6. If not authorized, return 403 Forbidden

For users with `REALGUARDIO_ADMIN` role:
- Skip location-based authorization
- Proceed directly with operation

### 4. Customer Service Endpoint

**New Endpoint:** `GET /locations/{locationId}/roles`
- **Path Parameter:** `locationId` - Location ID (Long)
- **Authorization:** Bearer token (user identity extracted from JWT)
- **Response:** `{"roles": ["SECURITY_SYSTEM_ARMER", "SECURITY_SYSTEM_DISARMER", ...]}`
- **Logic:** Return union of:
  - Direct roles from CustomerEmployeeLocationRole
  - Indirect roles from Team memberships (TeamLocationRole)

### 5. Response Formats

**Success (200 OK):**
```json
{
  "id": 123,
  "locationId": 456,
  "locationName": "Warehouse-1",
  "state": "ARMED",
  "actions": ["ARM", "DISARM"],
  "version": 2
}
```

**Error Responses:**
- 400 Bad Request - Invalid action or missing locationId
- 403 Forbidden - User lacks required permission
- 404 Not Found - SecuritySystem not found

### 6. Inter-Service Communication

- Use RestTemplate or WebClient for REST calls
- SecurityService → CustomerService communication
- Forward JWT token in Authorization header

### 7. Audit Logging

Log all arm/disarm operations using SLF4J:
- User identity (from JWT)
- Action performed (ARM/DISARM)
- SecuritySystem ID
- Location ID
- Timestamp
- Success/failure status

### 8. Implementation Notes

- Operations supported: ARM and DISARM only
- ACKNOWLEDGE action reserved for future alarm handling
- ALARMED state triggered by sensors/events (not user action)
- All operations are atomic and transactional
- Use optimistic locking (version field) for concurrent updates
