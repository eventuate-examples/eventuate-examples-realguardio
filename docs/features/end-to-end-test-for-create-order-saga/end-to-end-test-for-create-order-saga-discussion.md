# End-to-End Test for Create Order Saga - Discussion

## Initial Idea
Create a new Gradle project called end-to-end-tests that will:
1. Launch containers using testcontainers' support for docker compose
2. Create a customer (referencing CustomerServiceComponentTest.java)
3. Create a security system (referencing OrchestrationServiceComponentTest.java)  
4. Use the security services REST API to retrieve the security system and verify it has been assigned a locationID

## Q&A Development Process

### Question 1: Project Structure and Location
Where should the end-to-end-tests project be created within your codebase structure?

**Options:**
A. As a sibling directory to the existing services (at the same level as realguardio-customer-service, realguardio-orchestration-service, etc.)
B. Inside a dedicated testing folder (e.g., /testing/end-to-end-tests)
C. As a subdirectory under the root project
D. In a completely separate repository

**Default recommendation:** Option A - This keeps it alongside the other service modules and makes it easy to reference in the multi-project Gradle build.

**Answer:** A - As a sibling directory to the existing services

### Question 2: Docker Compose File Selection
Which Docker Compose file should the end-to-end tests use to launch the services?

**Options:**
A. Use the existing docker-compose.yml in the root directory
B. Create a dedicated docker-compose-e2e.yml specifically for end-to-end testing
C. Use docker-compose.yml but with an override file (docker-compose.e2e.override.yml)
D. Dynamically generate a compose file based on test requirements

**Default recommendation:** Option A - Use the existing docker-compose.yml since it already contains all the necessary service configurations for the complete system.

**Answer:** A - Use the existing docker-compose.yml in the root directory

### Question 3: Service Container Strategy
How should the end-to-end tests handle the service containers for customer-service, orchestration-service, and security-service?

**Options:**
A. Launch all services as separate Testcontainers using ServiceContainer (similar to component tests)
B. Use docker-compose via Testcontainers ComposeContainer to launch all services together
C. Mix approach: Use ComposeContainer for infrastructure (Kafka, DB) and ServiceContainer for application services
D. Build and run services locally outside of containers

**Default recommendation:** Option B - Use ComposeContainer to launch all services together via docker-compose, as this better simulates the production environment and simplifies network configuration between services.

**Answer:** B - Use docker-compose via Testcontainers ComposeContainer

### Question 4: Wait Strategy for Service Readiness
How should the tests wait for all services to be ready before starting test execution?

**Options:**
A. Use fixed delays (Thread.sleep) with conservative timing
B. Poll health endpoints for each service until all return 200 OK
C. Use Testcontainers' built-in wait strategies (WaitingFor.forLogMessage, forHttp, etc.)
D. Combine health endpoint polling with checking for specific Kafka topics/consumer groups

**Default recommendation:** Option C - Use Testcontainers' built-in wait strategies, specifically waiting for HTTP health endpoints to return 200 OK for each service.

**Answer:** C - Use Testcontainers' built-in wait strategies

### Question 5: Test Data Management
How should the tests handle test data isolation and cleanup between test runs?

**Options:**
A. Use unique identifiers (timestamps/UUIDs) for all test data to avoid conflicts
B. Reset the database between each test using Flyway or similar
C. Use database transactions with rollback after each test
D. Create separate database schemas for each test run

**Default recommendation:** Option A - Use unique identifiers for all test data, as this is the simplest approach and matches the pattern used in the component tests (e.g., using timestamps for IDs).

**Answer:** A - Use unique identifiers (timestamps/UUIDs) for all test data

### Question 6: Security System Service Verification
After creating a security system through the orchestration service, how should the test verify that the security system has been assigned a locationId?

**Options:**
A. Query the security-service REST API directly to GET the security system by ID
B. Check the database directly using JDBC/JPA queries
C. Subscribe to Kafka events and verify the SecuritySystemCreated event contains locationId
D. Use the orchestration service's API to retrieve the complete saga result

**Default recommendation:** Option A - Query the security-service REST API directly, as this validates the end-to-end flow through the actual service interfaces that clients would use.

**Answer:** A - Query the security-service REST API directly to GET the security system by ID

### Question 7: Timing and Retry Strategy
How should the test handle the asynchronous nature of the saga and wait for the security system to be fully created?

**Options:**
A. Use a fixed sleep time (e.g., Thread.sleep(5000))
B. Use Awaitility library with configurable timeout and polling intervals
C. Use Eventually pattern (as seen in component tests) with custom retry logic
D. Implement exponential backoff retry pattern

**Default recommendation:** Option C - Use the Eventually pattern that's already used in the component tests, as it provides consistent behavior across the test suite and the team is familiar with it.

**Answer:** C - Use Eventually pattern (as seen in component tests) with custom retry logic

### Question 8: Authentication Token Management
How should the end-to-end tests handle JWT authentication tokens for API calls?

**Options:**
A. Reuse the JwtTokenHelper class from component tests to get tokens from the IAM service
B. Use a pre-generated long-lived token stored in test configuration
C. Disable authentication for end-to-end test profile
D. Mock the authentication server responses

**Default recommendation:** Option A - Reuse the existing JwtTokenHelper pattern from component tests to maintain consistency and test the full authentication flow.

**Answer:** A - Reuse the JwtTokenHelper class from component tests to get tokens from the IAM service

### Question 9: Test Organization and Naming
How should the test class and test methods be organized and named?

**Options:**
A. Single test class "CreateOrderSagaE2ETest" with one comprehensive test method
B. Single test class with multiple test methods for different scenarios (happy path, error cases)
C. Multiple test classes organized by saga type (CreateOrderSagaE2ETest, UpdateOrderSagaE2ETest, etc.)
D. Test classes organized by service (CustomerE2ETest, OrchestrationE2ETest, SecurityE2ETest)

**Default recommendation:** Option B - Single test class with multiple test methods for different scenarios, starting with the happy path test case you described, with room to add error scenarios later.

**Answer:** B - Single test class with multiple test methods for different scenarios

### Question 10: Gradle Build Configuration
How should the end-to-end-tests module be integrated into the existing Gradle build?

**Options:**
A. Add as a new subproject in settings.gradle with its own build.gradle
B. Create a standalone Gradle project with its own settings.gradle
C. Add as a source set within an existing service module
D. Create a Gradle composite build that includes the main project

**Default recommendation:** Option A - Add as a new subproject in settings.gradle, allowing it to share dependency versions and build configurations with other modules while maintaining its own build file.

**Answer:** B - Create a standalone Gradle project with its own settings.gradle

### Question 11: Project Dependencies
Since this is a standalone project, how should it reference the API/DTO classes from the services (e.g., CreateSecuritySystemResponse)?

**Options:**
A. Copy the necessary DTO/API classes into the end-to-end-tests project
B. Publish service API JARs to a local Maven repository and reference them
C. Use REST API responses as Maps/JsonNodes without strongly-typed DTOs
D. Create simplified mirror DTOs in the test project with only needed fields

**Default recommendation:** Option D - Create simplified mirror DTOs in the test project with only the fields needed for validation, avoiding complex dependency management while maintaining type safety.

**Answer:** D - Create simplified mirror DTOs in the test project with only needed fields

### Question 12: Directory Structure
What should be the internal structure of the end-to-end-tests project?

**Options:**
A. Standard Maven/Gradle structure: src/test/java for tests, src/test/resources for configs
B. src/main/java for test utilities/DTOs, src/test/java for actual tests
C. src/e2e/java for tests with custom source set configuration
D. Flat structure with all Java files in src/test/java including DTOs and utilities

**Default recommendation:** Option B - Use src/main/java for reusable test utilities, DTOs, and helpers, with src/test/java for the actual test classes, providing clear separation of concerns.

**Answer:** Custom - Everything in src/endToEndTest/java (custom source set)

### Question 13: Package Structure
How should the packages be organized within src/endToEndTest/java?

**Options:**
A. Flat package: com.realguardio.e2e with all classes in one package
B. By type: com.realguardio.e2e.tests, com.realguardio.e2e.dto, com.realguardio.e2e.utils
C. By feature/saga: com.realguardio.e2e.createorder, com.realguardio.e2e.updateorder
D. By service: com.realguardio.e2e.customer, com.realguardio.e2e.orchestration, com.realguardio.e2e.security

**Default recommendation:** Option B - Organize by type to clearly separate test classes from DTOs and utilities, making the codebase easy to navigate.

**Answer:** Custom - Use com.realguardio.endtoendtests as the base package

### Question 14: Error Handling and Assertions
When the saga fails or times out, how should the test report the failure?

**Options:**
A. Simple assertion failure with default message
B. Detailed assertion with context about which step failed and current state
C. Collect all service logs and attach to test failure report
D. Take screenshots/dumps of database state on failure

**Default recommendation:** Option B - Provide detailed assertions with context about which step failed, what was expected vs actual, helping with debugging without overwhelming output.

**Answer:** A - Simple assertion failure with default message (matching component test patterns)

## Summary of Decisions

### Project Structure
- **Location:** Standalone Gradle project at sibling level to services
- **Build:** Standalone project with own settings.gradle
- **Source Set:** Custom source set at src/endToEndTest/java
- **Package:** com.realguardio.endtoendtests

### Technical Choices
- **Container Strategy:** Use Docker Compose via Testcontainers ComposeContainer
- **Docker Compose:** Use existing docker-compose.yml from root
- **Wait Strategy:** Testcontainers' built-in wait strategies for health endpoints
- **Test Framework:** JUnit 5 with RestAssured (consistent with component tests)
- **Authentication:** Reuse JwtTokenHelper from component tests
- **Retry Pattern:** Eventually pattern as used in component tests
- **Assertions:** Simple assertions matching component test style

### Test Design
- **Data Isolation:** Unique identifiers (timestamps/UUIDs) for test data
- **DTOs:** Simplified mirror DTOs in test project
- **Verification:** Query security-service REST API directly to verify locationId
- **Organization:** Single test class with multiple test methods for scenarios

### Test Scenario (as defined in original idea)
The test will execute these three steps:
1. Create a customer using customer-service API
2. Create a security system using orchestration-service API
3. Verify the security system has been assigned a locationId by querying security-service API

This specification provides a complete blueprint for implementing the end-to-end tests that validate the Create Order Saga workflow across the customer, orchestration, and security services.

### Question 16: Test Execution Configuration
How should the end-to-end tests be executed in the build pipeline?

**Answer:** Based on the existing EndToEndTestsPlugin.groovy from the eventuate-tram-sagas-examples-customers-and-orders project, the pattern is already established:
- Use the existing EndToEndTestsPlugin which creates an 'endToEndTest' task
- The plugin configures the src/endToEndTest/java source set
- The task is automatically added to the 'check' task
- Can be run explicitly with `gradle endToEndTest`

### Question 17: Service Port Management
How should the test handle accessing service ports when running in Docker Compose?

**Options:**
A. Use fixed ports defined in docker-compose.yml
B. Use Testcontainers' getMappedPort() for dynamic port mapping
C. Configure services to use host networking mode
D. Use service discovery via Docker networks

**Default recommendation:** Option B - Use Testcontainers' getMappedPort() to get the dynamically mapped ports, ensuring tests work regardless of local port availability.

**Answer:** A - Use fixed ports defined in docker-compose.yml

### Question 18: Helper Class Organization
What helper/utility classes should be created in the test project?

**Options:**
A. Minimal - just the test class and inline all helpers
B. ServiceClients class with methods for each service API
C. Separate client classes for each service (CustomerClient, OrchestrationClient, SecurityClient)
D. Full abstraction with builders, factories, and data generators

**Default recommendation:** Option C - Create separate client classes for each service to encapsulate the REST API calls, making tests cleaner and more maintainable.

**Answer:** C - Separate client classes for each service (CustomerClient, OrchestrationClient, SecurityClient)

### Question 19: Logging and Debugging
How should the tests handle logging for debugging failures?

**Options:**
A. Use SLF4J with Logback (same as services)
B. System.out.println for simplicity
C. Use JUnit 5's built-in logging support
D. No logging, rely on assertion messages

**Default recommendation:** Option A - Use SLF4J with Logback to maintain consistency with the services and provide configurable log levels.

**Answer:** A - Use SLF4J with Logback (same as services)

## Final Specification Summary

### Project Structure
- **Project Name:** end-to-end-tests
- **Location:** Standalone Gradle project at sibling level to services
- **Build:** Standalone project with own settings.gradle
- **Source Set:** Custom source set at src/endToEndTest/java
- **Package:** com.realguardio.endtoendtests
- **Gradle Plugin:** Use existing EndToEndTestsPlugin pattern

### Technical Stack
- **Container Management:** Docker Compose via Testcontainers ComposeContainer
- **Docker Compose:** Use existing docker-compose.yml from root
- **Port Access:** Use fixed ports defined in docker-compose.yml
- **Wait Strategy:** Testcontainers' built-in wait strategies for health endpoints
- **Test Framework:** JUnit 5 with RestAssured
- **Authentication:** Reuse JwtTokenHelper from component tests
- **Retry Pattern:** Eventually pattern as used in component tests
- **Assertions:** Simple assertions matching component test style
- **Logging:** SLF4J with Logback

### Code Organization
- **Helper Classes:** Separate client classes for each service (CustomerClient, OrchestrationClient, SecurityClient)
- **DTOs:** Simplified mirror DTOs in test project with only needed fields
- **Test Class:** Single test class with multiple test methods for scenarios
- **Data Isolation:** Unique identifiers (timestamps/UUIDs) for test data

### Test Workflow
1. Launch all services using Docker Compose
2. Wait for services to be healthy
3. Get JWT token from IAM service
4. Create a customer using customer-service API
5. Create a security system using orchestration-service API
6. Poll security-service API to verify the security system has been assigned a locationId

### Execution
- Run with `gradle endToEndTest`
- Automatically included in `gradle check`
- Can be run independently for faster development cycles

This complete specification is ready for implementation. All design decisions have been made to ensure consistency with existing patterns while providing robust end-to-end testing capabilities.
