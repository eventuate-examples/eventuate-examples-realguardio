# End-to-End Test for Create Order Saga - Implementation Plan

## Overview

This implementation plan uses TDD to build end-to-end tests for the RealGuardio Create Order Saga. The focus is on testing the actual end-to-end flow, not intermediate components.

## Instructions for the Coding Agent

When executing this plan:
1. Follow TDD: Write ONE @Test method → Make it compile and pass → Refactor → Commit (if requested) → Repeat
2. Mark each checkbox `[x]` when completed
3. DTO tests go in src/test/java, end-to-end tests go in src/endToEndTest/java

## Step 1: Project Setup and Docker Compose Verification

### TDD Cycle 1.1: Create project and verify Docker Compose starts

```text
[ ] Create project structure:
    [ ] Create end-to-end-tests directory
    [ ] Create .gitignore with Gradle and IDE patterns
    [ ] Create settings.gradle with rootProject.name = 'end-to-end-tests'
    [ ] Create build.gradle with:
        - Java 17
        - endToEndTest source set
        - JUnit 5, Testcontainers, RestAssured dependencies
        - Jackson for JSON
        - SLF4J/Logback for logging
    [ ] Create directory structure:
        - src/endToEndTest/java/com/realguardio/endtoendtests/
        - src/test/java/com/realguardio/endtoendtests/dto/

[ ] Write first test - Docker Compose starts:
    [ ] Create RealGuardioEndToEndTest.java in src/endToEndTest/java
    [ ] Write @Test void dockerComposeShouldStart()
    [ ] Use ComposeContainer pointing to ../docker-compose.yml
    [ ] Start all services with wait strategies
    [ ] Assert containers are running

[ ] Make test pass:
    [ ] Add @Testcontainers annotation
    [ ] Configure ComposeContainer with proper service names
    [ ] Add wait strategies for health endpoints
    [ ] Fix any startup issues
    [ ] Run ./gradlew endToEndTest until it passes

[ ] Refactor:
    [ ] Extract container configuration to static field
    [ ] Add appropriate timeouts
```

## Step 2: Create Customer Test with DTOs

### TDD Cycle 2.1: Write test to create customer

```text
[ ] Write test shouldCreateCustomerAndSecuritySystem():
    [ ] Add @Test void shouldCreateCustomerAndSecuritySystem() to RealGuardioEndToEndTest
    [ ] Get JWT token from IAM service
    [ ] POST to customer service /customers endpoint
    [ ] Assert customer ID is returned (will fail - no DTOs yet)

[ ] Create DTOs to make test compile:
    [ ] Create Administrator.java record in src/main/java with firstName, lastName, email
    [ ] Create CreateCustomerRequest.java record with name, initialAdministrator
    [ ] Create CreateCustomerResponse.java record with customerId

[ ] Write DTO JSON serialization tests:
    [ ] Create AdministratorTest.java in src/test/java
    [ ] Write @Test void shouldSerializeAdministrator()
    [ ] Write @Test void shouldDeserializeAdministrator()
    [ ] Make tests pass

[ ] Write CreateCustomerRequest JSON tests:
    [ ] Create CreateCustomerRequestTest.java in src/test/java
    [ ] Write @Test void shouldSerializeCreateCustomerRequest()
    [ ] Write @Test void shouldDeserializeCreateCustomerRequest()
    [ ] Make tests pass with nested Administrator

[ ] Write CreateCustomerResponse JSON tests:
    [ ] Create CreateCustomerResponseTest.java in src/test/java
    [ ] Write @Test void shouldSerializeCreateCustomerResponse()
    [ ] Write @Test void shouldDeserializeCreateCustomerResponse()
    [ ] Make tests pass

[ ] Make end-to-end test pass:
    [ ] Create JwtTokenHelper utility class
    [ ] Implement token retrieval from IAM service
    [ ] Send POST request with proper JSON
    [ ] Parse response to get customer ID
    [ ] Assert customer ID > 0
    [ ] Run ./gradlew endToEndTest until it passes

[ ] Refactor:
    [ ] Extract customer creation to helper method
    [ ] Add logging for debugging
```

## Step 3: Enhance Test to Create Security System

### TDD Cycle 3.1: Extend test to create security system

```text
[ ] Enhance shouldCreateCustomerAndSecuritySystem() test:
    [ ] Add POST to orchestration service /securitysystems
    [ ] Pass customerId and locationName
    [ ] Assert security system ID is returned (will fail - no DTOs)

[ ] Create security system DTOs:
    [ ] Create CreateSecuritySystemRequest.java record with customerId, locationName
    [ ] Create CreateSecuritySystemResponse.java record with securitySystemId

[ ] Write CreateSecuritySystemRequest JSON tests:
    [ ] Create CreateSecuritySystemRequestTest.java in src/test/java
    [ ] Write @Test void shouldSerializeCreateSecuritySystemRequest()
    [ ] Write @Test void shouldDeserializeCreateSecuritySystemRequest()
    [ ] Make tests pass

[ ] Write CreateSecuritySystemResponse JSON tests:
    [ ] Create CreateSecuritySystemResponseTest.java in src/test/java
    [ ] Write @Test void shouldSerializeCreateSecuritySystemResponse()
    [ ] Write @Test void shouldDeserializeCreateSecuritySystemResponse()
    [ ] Make tests pass

[ ] Make enhanced test pass:
    [ ] Send POST to orchestration service with request DTO
    [ ] Parse response to get security system ID
    [ ] Assert security system ID > 0
    [ ] Run ./gradlew endToEndTest until it passes

[ ] Refactor:
    [ ] Extract security system creation to helper method
    [ ] Consolidate common HTTP request logic
```

## Step 4: Verify Location ID Assignment

### TDD Cycle 4.1: Verify saga completion with location ID

```text
[ ] Further enhance shouldCreateCustomerAndSecuritySystem() test:
    [ ] Add GET to security service /securitysystems/{id}
    [ ] Poll until locationId is assigned
    [ ] Assert locationId is not null and > 0 (will fail - no DTO or retry logic)

[ ] Create SecuritySystemDetails DTO:
    [ ] Create SecuritySystemDetails.java record with id, customerId, locationId, status
    [ ] Handle nullable locationId field

[ ] Write SecuritySystemDetails JSON tests:
    [ ] Create SecuritySystemDetailsTest.java in src/test/java
    [ ] Write @Test void shouldSerializeSecuritySystemDetails()
    [ ] Write @Test void shouldDeserializeSecuritySystemDetails()
    [ ] Test with and without locationId
    [ ] Make tests pass

[ ] Create Eventually utility:
    [ ] Create Eventually.java utility class
    [ ] Implement retry logic with timeout
    [ ] Add proper exception handling with context

[ ] Make location ID verification pass:
    [ ] Use Eventually to poll security service
    [ ] Retrieve SecuritySystemDetails
    [ ] Check for locationId assignment
    [ ] Assert locationId > 0 within timeout
    [ ] Run ./gradlew endToEndTest until it passes

[ ] Refactor:
    [ ] Extract polling logic to helper method
    [ ] Add progress logging during retries
    [ ] Optimize polling interval and timeout
```

## Step 5: Create Service Clients (Refactoring)

### TDD Cycle 5.1: Extract service clients

```text
[ ] Refactor to create service clients:
    [ ] Create CustomerServiceClient.java
    [ ] Move customer creation logic from test
    [ ] Keep test working (no new functionality)

[ ] Create OrchestrationServiceClient:
    [ ] Extract security system creation logic
    [ ] Keep test working

[ ] Create SecurityServiceClient:
    [ ] Extract security system retrieval logic
    [ ] Keep test working

[ ] Update test to use clients:
    [ ] Initialize clients in @BeforeEach
    [ ] Use client methods instead of inline RestAssured
    [ ] Verify test still passes

[ ] Final cleanup:
    [ ] Add comprehensive logging
    [ ] Ensure unique test data (UUIDs, timestamps)
    [ ] Document client methods
```

## Step 6: Add Error Handling Tests

### TDD Cycle 6.1: Test error cases

```text
[ ] Write test for invalid customer data:
    [ ] Create @Test void shouldFailWithInvalidCustomerData()
    [ ] Send request with missing required fields
    [ ] Assert 400 Bad Request response

[ ] Make error test pass:
    [ ] Handle error responses properly
    [ ] Verify appropriate HTTP status codes

[ ] Write test for timeout handling:
    [ ] Create @Test void shouldHandleTimeoutGracefully()
    [ ] Use very short timeout in Eventually
    [ ] Assert meaningful timeout exception

[ ] Make timeout test pass:
    [ ] Improve timeout error messages
    [ ] Include context about what was being waited for
```

## Step 7: Build and Documentation

### TDD Cycle 7.1: Final setup

```text
[ ] Configure build:
    [ ] Ensure check task depends on endToEndTest
    [ ] Add test reporting configuration
    [ ] Create buildSrc plugin if beneficial

[ ] Create documentation:
    [ ] Create README.md with:
        - How to run tests
        - Prerequisites
        - Troubleshooting
    [ ] Add JavaDoc to public methods
    [ ] Document expected test execution time

[ ] Final verification:
    [ ] Run ./gradlew clean endToEndTest
    [ ] Run ./gradlew clean check
    [ ] Ensure all tests pass consistently
```

## Success Criteria

Each step is complete when:
1. Test is written first and fails
2. Minimal code makes it pass
3. Code is refactored while keeping tests green
4. All tests continue to pass

## Key Principles

- **Focus on end-to-end flow** - Not testing individual components
- **DTOs are implementation details** - Created to support the main test
- **One test method drives development** - shouldCreateCustomerAndSecuritySystem() is enhanced incrementally
- **Separate concerns** - DTO tests in src/test/java, E2E tests in src/endToEndTest/java

## Change History

- Complete rewrite to focus on actual end-to-end testing
- Removed unnecessary intermediate tests
- DTOs are created as needed to support the main test
- Single test method (shouldCreateCustomerAndSecuritySystem) is progressively enhanced
- DTO JSON tests are separate unit tests in src/test/java