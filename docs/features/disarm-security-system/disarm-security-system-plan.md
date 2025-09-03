# Security System Arm/Disarm Implementation Plan

## Overview

This implementation plan uses the Steel Thread methodology to implement the arm/disarm security system feature. Each thread represents a narrow end-to-end flow that delivers value, building progressively on previous threads.

The plan follows an **outside-in development approach**, starting with the controller layer and working inward through the service and entity layers, then adding authorization, inter-service communication, and finally comprehensive testing and error handling.

The plan consists of 12 progressive threads that build the feature from the outside-in.

## Implementation Instructions for Coding Agent

When implementing this plan:
1. Mark each checkbox with '[x]' when the task/step is completed
2. Follow TDD principles: write test first, make it pass, refactor
3. Run all tests after each thread to ensure nothing is broken
4. Commit after each successful thread completion

## Steel Thread 1: Create Controller Endpoint with Basic DTO

This thread starts with the controller layer, creating the endpoint and basic request DTO.

```text
[x] Create PUT /securitysystems/{id} endpoint with basic DTO

Steps:
[x] 1. Write test in SecuritySystemControllerTest: test PUT request with DISARM action returns 200
     - Create test class with MockMVC setup
     - Mock SecuritySystemService
     - Create minimal SecuritySystemActionRequest DTO inline for test
     - Write test for DISARM action expecting 200 response
[x] 2. Make the test pass:
     - Create SecuritySystemAction enum with values: ARM, DISARM
     - Create SecuritySystemActionRequest class with action field and getter/setter
     - Create or update SecuritySystemController with PUT mapping for /{id}
     - Accept SecuritySystemActionRequest in request body
     - Call service.disarm() for DISARM action (create stub method)
     - Return 200 OK
[x] 3. Refactor and run all tests
[x] 4. Write test: test PUT request with ARM action returns 200
[x] 5. Make the test pass:
     - Update controller to handle ARM action
     - Call service.arm() for ARM action (create stub method)
[x] 6. Refactor and run all tests
[ ] 7. Write test: test invalid action returns 400
[ ] 8. Make the test pass:
     - Add validation for invalid actions
     - Return 400 Bad Request for invalid actions
[ ] 9. Refactor and run all tests
[ ] 10. Write test: test non-existent system returns 404
[ ] 11. Make the test pass:
     - Handle NotFoundException from service
     - Return 404 Not Found
[ ] 12. Refactor and run all tests
[ ] 13. Commit the changes
```

## Steel Thread 2: Implement Service Layer Methods

This thread implements the service layer that the controller depends on.

```text
[x] Implement arm() and disarm() methods in SecuritySystemService

Steps:
[x] 1. Write unit test in SecuritySystemServiceTest: test disarm() method changes state to DISARMED
     - Mock SecuritySystemRepository
     - Create test SecuritySystem entity
     - Verify disarm() loads system, calls entity method, and saves
[x] 2. Make the test pass:
     - Implement disarm() method in SecuritySystemService
     - Load SecuritySystem by id
     - Check if locationId is present (throw BadRequestException if missing)
     - Call entity.disarm() and save
[x] 3. Refactor and run all tests
[x] 4. Write unit test: test arm() method changes state to ARMED
[x] 5. Make the test pass:
     - Implement arm() method in SecuritySystemService
     - Load SecuritySystem by id
     - Check if locationId is present (throw BadRequestException if missing)
     - Call entity.arm() and save
[x] 6. Refactor and run all tests
[ ] 7. Write test: test service throws NotFoundException for non-existent system
[ ] 8. Make the test pass:
     - Throw NotFoundException when system not found
[ ] 9. Refactor and run all tests
[ ] 10. Write test: test service throws BadRequestException when locationId is null
[ ] 11. Make the test pass:
     - Add locationId null check
     - Throw BadRequestException with appropriate message
[ ] 12. Refactor and run all tests
[ ] 13. Commit the changes
```

## Steel Thread 3: Implement Entity State Transitions

This thread implements the entity methods for state transitions.

```text
[x] Implement arm() and disarm() methods in SecuritySystem entity

Steps:
[x] 1. Write unit test in SecuritySystemTest: test disarm() state transition
     - Test transition from ARMED to DISARMED
     - Test transition from ALARMED to DISARMED
     - Test already DISARMED stays DISARMED
[x] 2. Make the test pass:
     - Add disarm() method to SecuritySystem entity
     - Set state to DISARMED
[x] 3. Refactor and run all tests
[x] 4. Write unit test: test arm() state transition
     - Test transition from DISARMED to ARMED
     - Test cannot arm when ALARMED (throws exception)
     - Test already ARMED stays ARMED
[x] 5. Make the test pass:
     - Add arm() method to SecuritySystem entity
     - Transition to ARMED state
     - Throw IllegalStateException if already ALARMED
[x] 6. Refactor and run all tests
[x] 7. Commit the changes
```

## Steel Thread 4: Add Top-Level Role Authorization

This thread adds Spring Security authorization for REALGUARDIO_ADMIN and REALGUARDIO_CUSTOMER_EMPLOYEE roles.

```text
[ ] Add Spring Security authorization for top-level roles

Steps:
[ ] 1. Write test in SecuritySystemControllerSecurityTest: test request without authentication returns 401
     - Create test class with MockMVC and Spring Security test support
     - Mock SecuritySystemService
     - Write test for unauthenticated request
[ ] 2. Make the test pass:
     - Add @PreAuthorize annotation to controller endpoint
     - @PreAuthorize("hasRole('REALGUARDIO_ADMIN') or hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
     - Update SecurityConfig if needed
[ ] 3. Refactor and run all tests
[ ] 4. Write test: test request with invalid role returns 403
[ ] 5. Make the test pass:
     - Verify @PreAuthorize correctly rejects invalid roles
[ ] 6. Refactor and run all tests
[ ] 7. Write test: test request with REALGUARDIO_ADMIN role succeeds
[ ] 8. Make the test pass:
     - Create test fixture for admin JWT token
     - Verify admin can access endpoint
[ ] 9. Refactor and run all tests
[ ] 10. Write test: test request with REALGUARDIO_CUSTOMER_EMPLOYEE role succeeds
[ ] 11. Make the test pass:
     - Create test fixture for employee JWT token
     - Verify employee can access endpoint
[ ] 12. Refactor and run all tests
[ ] 13. Commit the changes
```

## Steel Thread 5: Create Customer Service Location Roles Endpoint

This thread implements the Customer Service endpoint that returns user roles for a location.

```text
[x] Implement GET /locations/{locationId}/roles endpoint in Customer Service

Steps:
[x] 1. Write test in LocationRoleControllerTest: test returns roles for user with direct location roles
     - Create test class with MockMVC setup
     - Mock LocationRoleService
     - Write test expecting roles to be returned
[x] 2. Make the test pass:
     - Create RolesResponse DTO with Set<String> roles field
     - Create LocationRoleController with GET mapping for /{locationId}/roles
     - Add @PreAuthorize for REALGUARDIO_CUSTOMER_EMPLOYEE or REALGUARDIO_ADMIN
     - Create LocationRoleService stub
[x] 3. Refactor and run all tests
[x] 4. Write test: test returns 404 when user has no roles at location
[x] 5. Make the test pass:
     - Update LocationRoleController to return 404 when roles set is empty
[x] 6. Refactor and run all tests
[x] 7. Write test: test requires authentication
[x] 8. Make the test pass:
     - Verify Spring Security configuration is correct
[x] 9. Refactor and run all tests
[x] 10. Write unit test in LocationRoleServiceTest: test getUserRolesAtLocation returns direct roles
[x] 11. Make the test pass:
     - Implement getUserRolesAtLocation method
     - Add repository method findDirectRolesAtLocation(employeeId, locationId)
     - Query direct roles from CustomerEmployeeLocationRole
[x] 12. Refactor and run all tests
[x] 13. Commit the changes
```

## Steel Thread 6: Add Team-Based Roles to Customer Service

This thread enhances the Customer Service to include team-based roles in the response.

```text
[ ] Enhance Customer Service to include team-based roles

Steps:
[ ] 1. Write unit test in LocationRoleServiceTest:
     - Test aggregates direct and team-based roles
     - Test handles user with only team roles
     - Test handles user with both direct and team roles
[ ] 2. Add repository method to find team-based roles:
     - findTeamRolesAtLocation(employeeId, locationId)
     - Join team_members, team_location_roles tables
[ ] 3. Update LocationRoleService.getUserRolesAtLocation:
     - Query both direct and team-based roles
     - Return union of both sets
[ ] 4. Write integration test to verify team roles are included
[ ] 5. Run all tests and make them pass
[ ] 6. Run all tests to ensure nothing is broken
[ ] 7. Commit the changes
```

## Steel Thread 7: Create Customer Service Client in Security Service

This thread creates the client component in Security Service to call Customer Service.

```text
[ ] Create CustomerServiceClient in Security Service

Steps:
[ ] 1. Write unit test for CustomerServiceClient:
     - Test successful role retrieval
     - Test handling of 404 response (returns empty set)
     - Test handling of service unavailable
     - Test JWT token forwarding
[ ] 2. Create CustomerServiceClient class:
     - Inject RestTemplate and customer service URL
     - Implement getUserRolesAtLocation method
     - Forward JWT token in Authorization header
     - Handle different response scenarios
[ ] 3. Create RolesResponse DTO in Security Service
[ ] 4. Add configuration for customer service URL:
     - Add to application.yml
     - Add @Value annotation for URL injection
[ ] 5. Configure RestTemplate bean if not exists
[ ] 6. Run the unit test with mocked RestTemplate
[ ] 7. Run all tests to ensure nothing is broken
[ ] 8. Commit the changes
```

## Steel Thread 8: Integrate Location-Based Authorization

This thread integrates the Customer Service client to enforce location-based permissions.

```text
[ ] Integrate location-based authorization in SecuritySystemService

Steps:
[ ] 1. Write test in SecuritySystemServiceTest: admin bypasses location check when disarming
[ ] 2. Make the test pass:
     - Inject CustomerServiceClient (mock in test)
     - Add isCustomerEmployee() helper method
     - Update disarm() method to check if user is customer employee
     - Skip location check for admin
[ ] 3. Refactor and run all tests
[ ] 4. Write test: employee with CAN_DISARM permission can disarm
[ ] 5. Make the test pass:
     - Add validateLocationPermission() method
     - Add extractJwtFromCurrentAuthentication() method
     - Call validateLocationPermission for employees in disarm()
[ ] 6. Refactor and run all tests
[ ] 7. Write test: employee without CAN_DISARM permission gets ForbiddenException
[ ] 8. Make the test pass:
     - Create ForbiddenException class
     - Throw ForbiddenException when permission missing
[ ] 9. Refactor and run all tests
[ ] 10. Write test: admin bypasses location check when arming
[ ] 11. Make the test pass:
     - Update arm() method similar to disarm()
[ ] 12. Refactor and run all tests
[ ] 13. Write test: employee with CAN_ARM permission can arm
[ ] 14. Make the test pass:
     - Call validateLocationPermission for employees in arm()
[ ] 15. Refactor and run all tests
[ ] 16. Write test: employee without CAN_ARM permission gets ForbiddenException
[ ] 17. Make the test pass:
     - Ensure arm() throws ForbiddenException when permission missing
[ ] 18. Refactor and run all tests
[ ] 19. Write test: handle Customer Service unavailable
[ ] 20. Make the test pass:
     - Create ServiceUnavailableException class
     - Handle service failures in validateLocationPermission
[ ] 21. Refactor and run all tests
[ ] 22. Commit the changes
```

## Steel Thread 9: Add Audit Logging

This thread adds audit logging for all arm/disarm operations.

```text
[ ] Implement audit logging for security system operations

Steps:
[ ] 1. Write unit test to verify logging occurs:
     - Test successful operations log at INFO level
     - Test authorization failures log at WARN level
     - Test service failures log at ERROR level
[ ] 2. Add SLF4J logger to SecuritySystemService
[ ] 3. Add logOperation() method:
     - Log user, action, systemId, locationId, result, timestamp
[ ] 4. Update arm() and disarm() methods:
     - Call logOperation() after successful operation
     - Log failures in catch blocks
[ ] 5. Update validateLocationPermission():
     - Log authorization failures at WARN level
[ ] 6. Configure logging levels in application.yml
[ ] 7. Run tests with log output verification
[ ] 8. Run all tests to ensure nothing is broken
[ ] 9. Commit the changes
```

## Steel Thread 10: Add Optimistic Locking

This thread adds optimistic locking for version tracking.

```text
[ ] Implement optimistic locking for SecuritySystem entity

Steps:
[ ] 1. Write unit test to verify version field behavior:
     - Test version is incremented on updates
     - Test version field is included in DTO
[ ] 2. Add @Version field to SecuritySystem entity:
     - Add private Long version field
     - Add @Version annotation
[ ] 3. Add version to SecuritySystemDto
[ ] 4. Update SecuritySystemMapper to include version
[ ] 5. Create migration to add version column:
     - ALTER TABLE security_systems ADD COLUMN version BIGINT DEFAULT 0
[ ] 6. Handle OptimisticLockException in controller advice (return 409 Conflict)
[ ] 7. Run the unit tests
[ ] 8. Run all tests to ensure nothing is broken
[ ] 9. Commit the changes
```

## Steel Thread 11: Add Error Handling and Responses

This thread implements proper error handling and standardized error responses.

```text
[ ] Implement comprehensive error handling

Steps:
[ ] 1. Write integration test: test 400 for invalid action
[ ] 2. Make the test pass:
     - Create ErrorResponse DTO with error, message, timestamp, path fields
     - Create @ControllerAdvice class for global exception handling
     - Handle BadRequestException → 400
[ ] 3. Refactor and run all tests
[ ] 4. Write integration test: test 400 for missing locationId
[ ] 5. Make the test pass:
     - Ensure BadRequestException is thrown for missing locationId
     - Verify error response format
[ ] 6. Refactor and run all tests
[ ] 7. Write integration test: test 403 for insufficient permissions
[ ] 8. Make the test pass:
     - Handle ForbiddenException → 403
     - Update exception classes with meaningful messages
[ ] 9. Refactor and run all tests
[ ] 10. Write integration test: test 404 for non-existent security system
[ ] 11. Make the test pass:
     - Handle NotFoundException → 404
[ ] 12. Refactor and run all tests
[ ] 13. Write integration test: test 503 when Customer Service is unavailable
[ ] 14. Make the test pass:
     - Handle ServiceUnavailableException → 503
[ ] 15. Refactor and run all tests
[ ] 16. Run all tests to ensure nothing is broken
[ ] 17. Commit the changes
```

## Steel Thread 12: Add End-to-End Integration Tests

This thread adds comprehensive end-to-end integration tests using real services.

```text
[ ] Create comprehensive end-to-end integration tests with real services

Steps:
[ ] 1. Create SecuritySystemE2ETest class with @SpringBootTest
[ ] 2. Set up test configuration to use real services via test containers:
     - Configure test containers for PostgreSQL database
     - Configure test containers for both Security Service and Customer Service
     - Start all services in containers with proper networking
     - Configure service URLs for inter-service communication
[ ] 3. Write test: Admin arms and disarms system successfully
     - Create real JWT token with REALGUARDIO_ADMIN role
     - Create security system with locationId
     - Test ARM and DISARM operations
[ ] 4. Write test: Employee with permissions operates system
     - Set up customer employee with location roles in Customer Service
     - Create JWT with REALGUARDIO_CUSTOMER_EMPLOYEE role
     - Test successful operations with proper permissions
[ ] 5. Write test: Employee without permissions is denied
     - Set up employee without required roles
     - Verify 403 response
[ ] 6. Write test: System without locationId returns error
     - Create system without locationId
     - Verify 400 response
[ ] 7. Write test: Inter-service communication flow
     - Verify Security Service correctly calls Customer Service
     - Verify role aggregation works correctly
[ ] 8. Add error scenario tests to E2E test suite:
     - Verify error responses in end-to-end context
     - Test error flows with real services
[ ] 9. Configure test data fixtures and cleanup
[ ] 10. Run all integration tests
[ ] 11. Run all tests to ensure nothing is broken
[ ] 12. Commit the changes
```

## Verification Checklist

After completing all threads, verify:

```text
[ ] All unit tests pass
[ ] All integration tests pass
[ ] Security Service can arm/disarm systems
[ ] Admin role bypasses location checks
[ ] Employee role enforces location permissions
[ ] Customer Service returns correct roles
[ ] Errors are handled gracefully
[ ] Operations are logged for audit
[ ] Performance is acceptable
[ ] Documentation is complete
```

## Change History

- 2025-09-03: Reordered plan to follow outside-in development approach, starting with controller layer
- 2025-09-03: Removed Steel Thread 1 (Add locationId to SecuritySystem Entity) as SecuritySystem already has locationId field; renumbered remaining threads
- 2025-08-29: Removed caching and rate limiting threads (original Steel Thread 12 and 13) as they are not requirements
- 2025-08-29: Updated Steel Thread 11 (originally 12) to clarify end-to-end tests use real services via test containers (not mocks)
- 2025-08-29: Updated Steel Thread 10 (originally 11) to remove concurrent modification testing
- 2025-08-29: Moved Error Handling thread from Steel Thread 9 to Steel Thread 12 (after E2E tests) to implement/test error scenarios after happy path is complete
- 2025-08-29: Renumbered threads: Audit Logging (9), Optimistic Locking (10), E2E Tests (11), Error Handling (12), Documentation (13)
- 2025-08-29: Removed specific commit messages from all threads (commit messages to be determined at commit time)
- 2025-08-29: Updated controller tests to use MockMVC unit tests instead of integration tests
- 2025-08-29: Restructured all threads to follow strict TDD methodology: write one test, make it pass, refactor, repeat
- 2025-08-29: Total: 12 threads