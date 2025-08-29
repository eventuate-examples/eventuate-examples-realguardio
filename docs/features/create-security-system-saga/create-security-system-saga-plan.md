# Security System Creation Saga - Implementation Plan

## Overview

This plan implements the Security System creation saga using the Steel thread methodology. Each thread builds upon the previous one, delivering incremental value while establishing the complete end-to-end flow.

## Instructions for Coding Agent

When executing this plan:
1. Mark each checkbox `[x]` immediately after completing the task/step
2. Execute threads sequentially - complete all tasks in a thread before moving to the next
3. Run tests after each significant change to ensure nothing breaks
4. Use TDD principles: write test, implement code, refactor as needed
5. Commit after each successfully completed thread (when all tests pass)

## Thread 1: Project Setup and Module Structure

**Goal**: Establish the realguardio-orchestration-service project structure with proper Gradle configuration and dependencies.

```text
Create the realguardio-orchestration-service as a new standalone Gradle multi-module project (not part of an existing Gradle project) following the pattern from the example application. Set up the multi-module structure with proper separation of concerns.

Note: realguardio-orchestration-service is a separate Gradle multi-module project, similar to how the example customers-and-orders project is structured.

Tasks:
[ ] Create the realguardio-orchestration-service directory structure
    [ ] Create realguardio-orchestration-service/settings.gradle with module includes
    [ ] Create realguardio-orchestration-service/build.gradle with subprojects configuration
    
[ ] Create orchestration-sagas module
    [ ] Create orchestration-sagas/build.gradle with Eventuate Tram Saga dependencies
    [ ] Add dependency on eventuate-tram-sagas-spring-orchestration-simple-dsl-starter
    [ ] Create src/main/java directory structure with package io.realguardio.orchestration.sagas
    
[ ] Create orchestration-domain module  
    [ ] Create orchestration-domain/build.gradle with domain dependencies
    [ ] Create src/main/java directory structure with package io.realguardio.orchestration.domain
    [ ] Create SecuritySystemSagaService class (empty for now)
    
[ ] Create orchestration-restapi module
    [ ] Create orchestration-restapi/build.gradle with Spring Web dependencies
    [ ] Create src/main/java directory structure with package io.realguardio.orchestration.restapi
    [ ] Create SecuritySystemController class with empty POST endpoint
    
[ ] Create orchestration-main module
    [ ] Create orchestration-main/build.gradle with Spring Boot application dependencies
    [ ] Create OrchestrationServiceApplication class with @SpringBootApplication
    [ ] Create application.yml with basic Spring and Eventuate configuration
    [ ] Add dependencies on other orchestration modules
    
[ ] Create integration test to verify application context starts
    [ ] Apply IntegrationTestsPlugin in orchestration-main/build.gradle
    [ ] Create OrchestrationServiceApplicationTest class with @SpringBootTest in src/integrationTest/java
    [ ] Write test method that loads application context
    [ ] Run ./gradlew :realguardio-orchestration-service:orchestration-main:integrationTest
    [ ] Verify test passes and context loads successfully
```

## Thread 2: Command and Reply API Contracts

**Goal**: Define the command and reply records for cross-service communication using TDD.

```text
Create the API contract modules that define commands and replies for service communication using Java records. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] Create security-system-service-api module
    [ ] Create security-system-service-api/build.gradle with Eventuate Tram dependencies and Jackson for JSON
    [ ] Add test dependencies: JUnit 5, AssertJ, Jackson
    [ ] Configure Java 17+ for records support
    
    [ ] TDD: CreateSecuritySystemCommand record
        [ ] Write test that creates command, serializes to JSON, deserializes, and asserts equality
        [ ] Run test - verify it fails (record doesn't exist)
        [ ] Create record CreateSecuritySystemCommand(String locationName) implements Command
        [ ] Run test - verify it passes
        [ ] Add any necessary Jackson annotations if needed
        
    [ ] TDD: SecuritySystemCreated reply record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record SecuritySystemCreated(Long securitySystemId)
        [ ] Run test - verify it passes
        
    [ ] TDD: UpdateCreationFailedCommand record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record UpdateCreationFailedCommand(Long securitySystemId, String rejectionReason) implements Command
        [ ] Run test - verify it passes
        
    [ ] TDD: NoteLocationCreatedCommand record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record NoteLocationCreatedCommand(Long securitySystemId, Long locationId) implements Command
        [ ] Run test - verify it passes
        
    [ ] TDD: LocationNoted reply record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create empty record LocationNoted()
        [ ] Run test - verify it passes
    
[ ] Create customer-service-api module  
    [ ] Create customer-service-api/build.gradle with Eventuate Tram dependencies and Jackson
    [ ] Add test dependencies: JUnit 5, AssertJ, Jackson
    [ ] Configure Java 17+ for records support
    
    [ ] TDD: CreateLocationWithSecuritySystemCommand record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record CreateLocationWithSecuritySystemCommand(Long customerId, String locationName, Long securitySystemId) implements Command
        [ ] Run test - verify it passes
        
    [ ] TDD: LocationCreatedWithSecuritySystem reply record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record LocationCreatedWithSecuritySystem(Long locationId)
        [ ] Run test - verify it passes
        
    [ ] TDD: CustomerNotFound reply record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record CustomerNotFound() with @FailureReply annotation
        [ ] Run test - verify it passes
        
    [ ] TDD: LocationAlreadyHasSecuritySystem reply record
        [ ] Write test for JSON serialization/deserialization
        [ ] Run test - verify it fails
        [ ] Create record LocationAlreadyHasSecuritySystem() with @FailureReply annotation
        [ ] Run test - verify it passes

[ ] Integration test for all API contracts
    [ ] Write test that verifies all commands/replies can be serialized together
    [ ] Fix any issues found
    [ ] Run all module tests - verify everything passes
```

## Thread 3: Service Proxies for Remote Communication

**Goal**: Implement service proxy interfaces that abstract remote service communication using TDD.

```text
Create service proxy interfaces that encapsulate the logic for sending commands to remote services. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: SecuritySystemServiceProxy interface
    [ ] Write test for SecuritySystemServiceProxy that verifies command creation
    [ ] Run test - verify it fails (interface doesn't exist)
    [ ] Create SecuritySystemServiceProxy interface with @SagaParticipantProxy(channel="security-system-service")
    [ ] Add createSecuritySystem method signature returning CommandWithDestination
    [ ] Add updateCreationFailed method signature returning CommandWithDestination
    [ ] Add noteLocationCreated method signature returning CommandWithDestination
    [ ] Run test - verify proxy is generated and test passes
    [ ] Add proper parameter names and JavaDoc if needed
    
[ ] TDD: CustomerServiceProxy interface
    [ ] Write test for CustomerServiceProxy that verifies command creation
    [ ] Run test - verify it fails (interface doesn't exist)
    [ ] Create CustomerServiceProxy interface with @SagaParticipantProxy(channel="customer-service")
    [ ] Add createLocationWithSecuritySystem method signature returning CommandWithDestination
    [ ] Run test - verify proxy is generated and test passes
    
[ ] TDD: Proxy configuration and integration
    [ ] Write integration test for proxy bean creation and wiring
    [ ] Run test - verify it fails (configuration missing)
    [ ] Create SagaProxyConfiguration class with @Configuration
        [ ] Add @EnableSagaParticipantProxies annotation
        [ ] Import necessary Eventuate configurations
    [ ] Run test - verify beans are created and wired correctly
    [ ] Organize imports and clean up configuration
```

## Thread 4: Basic Saga Definition and Data Class

**Goal**: Implement the core saga orchestration logic with proper step sequencing using TDD.

```text
Create the CreateSecuritySystemSaga class that defines the saga flow and the saga data class that maintains state. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: CreateSecuritySystemSagaData class
    [ ] Write test for saga data class with all fields and getters/setters
    [ ] Run test - verify it fails
    [ ] Create CreateSecuritySystemSagaData class
        [ ] Add fields: securitySystemId, customerId, locationName, locationId, rejectionReason
        [ ] Implement constructors, getters, and setters
    [ ] Run test - verify it passes
    [ ] Consider using Lombok @Data if appropriate
    
[ ] TDD: CreateSecuritySystemSaga - Step 1
    [ ] Write test for first saga step (create security system)
    [ ] Run test - verify it fails
    [ ] Create CreateSecuritySystemSaga class implementing SimpleSaga<CreateSecuritySystemSagaData>
        [ ] Inject SecuritySystemServiceProxy
        [ ] Implement makeCreateSecuritySystemCommand method
        [ ] Define Step 1 with invokeParticipant
        [ ] Add onReply handler for SecuritySystemCreated
        [ ] Add withCompensation for updateCreationFailed
    [ ] Run test - verify it passes
    
[ ] TDD: CreateSecuritySystemSaga - Step 2
    [ ] Write test for second saga step (create location)
    [ ] Run test - verify it fails
    [ ] Add Step 2 to saga definition
        [ ] Inject CustomerServiceProxy
        [ ] Implement makeCreateLocationCommand method
        [ ] Add step with invokeParticipant
        [ ] Add onReply handlers for CustomerNotFound and LocationAlreadyHasSecuritySystem
    [ ] Run test - verify it passes
    
[ ] TDD: CreateSecuritySystemSaga - Step 3
    [ ] Write test for third saga step (note location created)
    [ ] Run test - verify it fails
    [ ] Add Step 3 to saga definition
        [ ] Implement makeNoteLocationCreatedCommand method
        [ ] Add step with invokeParticipant
        [ ] Implement getSagaDefinition method returning complete definition
    [ ] Run test - verify it passes
    [ ] Clean up saga definition for readability
    
[ ] TDD: Saga configuration
    [ ] Write integration test for saga bean creation
    [ ] Run test - verify it fails
    [ ] Create SagaConfiguration class
        [ ] Add @Configuration annotation
        [ ] Define CreateSecuritySystemSaga bean
        [ ] Import required Eventuate configurations
    [ ] Run test - verify saga bean is created correctly
```

## Thread 5: Saga Service with CompletableFuture Response

**Goal**: Implement the service layer that initiates sagas and handles HTTP response completion using TDD.

```text
Create the SecuritySystemSagaService that manages saga lifecycle and implements the CompletableFuture pattern. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: SecuritySystemSagaService - Basic structure
    [ ] Write test for createSecuritySystem method returning CompletableFuture
    [ ] Run test - verify it fails (class doesn't exist)
    [ ] Create SecuritySystemSagaService class with @Service
        [ ] Add SagaInstanceFactory dependency
        [ ] Add CreateSecuritySystemSaga dependency
        [ ] Create ConcurrentHashMap for pending saga responses
        [ ] Implement createSecuritySystem method stub returning CompletableFuture
    [ ] Run test - verify it passes
    
[ ] TDD: Saga initiation and future management
    [ ] Write test that verifies saga starts and future is stored
    [ ] Run test - verify it fails
    [ ] Implement createSecuritySystem method fully
        [ ] Create CreateSecuritySystemSagaData from parameters
        [ ] Create CompletableFuture<Long> for response
        [ ] Start saga instance with sagaInstanceFactory
        [ ] Store future in map with saga ID as key
        [ ] Return future
    [ ] Run test - verify it passes
    
[ ] TDD: Future completion mechanism
    [ ] Write test for completeSecuritySystemCreation method
    [ ] Run test - verify it fails
    [ ] Implement completeSecuritySystemCreation method
        [ ] Remove future from map by saga ID
        [ ] Complete future with securitySystemId if found
    [ ] Run test - verify it passes
    [ ] Add logging for debugging
    
[ ] TDD: Saga integration with future completion
    [ ] Write test that saga calls completeSecuritySystemCreation
    [ ] Run test - verify it fails
    [ ] Update CreateSecuritySystemSaga
        [ ] Inject SecuritySystemSagaService
        [ ] Modify handleSecuritySystemCreated to call completeSecuritySystemCreation
        [ ] Add method to access current saga instance ID
    [ ] Run test - verify future completes when saga receives reply
    [ ] Extract saga ID handling to utility method
```

## Thread 6: REST Controller Implementation

**Goal**: Implement the REST endpoint that accepts security system creation requests using TDD.

```text
Implement the SecuritySystemController that exposes the POST /securitysystems endpoint. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: Request/Response DTOs as records
    [ ] Write test for CreateSecuritySystemRequest serialization/validation
    [ ] Run test - verify it fails
    [ ] Create record CreateSecuritySystemRequest(@NotNull Long customerId, @NotBlank String locationName)
    [ ] Run test - verify it passes
    
    [ ] Write test for CreateSecuritySystemResponse serialization
    [ ] Run test - verify it fails
    [ ] Create record CreateSecuritySystemResponse(Long securitySystemId)
    [ ] Run test - verify it passes
    
[ ] TDD: SecuritySystemController - Happy path
    [ ] Write MockMvc test for POST /securitysystems returning 201
    [ ] Run test - verify it fails (controller doesn't exist)
    [ ] Create SecuritySystemController with @RestController
        [ ] Inject SecuritySystemSagaService (mock in test)
        [ ] Implement POST /securitysystems endpoint
        [ ] Accept @Valid @RequestBody CreateSecuritySystemRequest
        [ ] Call saga service createSecuritySystem
        [ ] Return CompletableFuture<ResponseEntity<CreateSecuritySystemResponse>>
        [ ] Map to 201 Created status
    [ ] Run test - verify it passes
    [ ] Extract response mapping logic if needed
    
[ ] TDD: Validation error handling
    [ ] Write test for invalid request (null customerId)
    [ ] Run test - verify it fails (no validation handling)
    [ ] Create GlobalExceptionHandler with @ControllerAdvice
        [ ] Add @ExceptionHandler for MethodArgumentNotValidException
        [ ] Return 400 Bad Request with error details
    [ ] Run test - verify it passes
    
[ ] TDD: Timeout error handling
    [ ] Write test for CompletableFuture timeout
    [ ] Run test - verify it fails
    [ ] Add timeout handling to controller
        [ ] Configure timeout on CompletableFuture
        [ ] Add @ExceptionHandler for TimeoutException
        [ ] Return 503 Service Unavailable
    [ ] Run test - verify it passes
    [ ] Consolidate error response format
```

## Thread 7: Security System Service Command Handlers

**Goal**: Implement command handlers in the security-system-service to process saga commands using TDD.

```text
Add command handlers to the existing security-system-service. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: Update SecuritySystem entity
    [ ] Write test for SecuritySystem with new fields
    [ ] Run test - verify it fails (fields don't exist)
    [ ] Update SecuritySystem entity
        [ ] Add customerId field (Long)
        [ ] Add locationId field (Long)
        [ ] Add rejectionReason field (String)
        [ ] Add CREATION_PENDING and CREATION_FAILED to SecuritySystemState enum
    [ ] Run test - verify it passes
    [ ] Create database migration script for new fields
    
[ ] TDD: CreateSecuritySystemCommand handler
    [ ] Write test for handling CreateSecuritySystemCommand
    [ ] Run test - verify it fails (handler doesn't exist)
    [ ] Create SecuritySystemCommandHandler class with @EventuateCommandHandler
        [ ] Inject SecuritySystemRepository
        [ ] Implement handle(CreateSecuritySystemCommand) method
        [ ] Create SecuritySystem with CREATION_PENDING state
        [ ] Save to repository
        [ ] Return SecuritySystemCreated reply
    [ ] Run test - verify it passes
    
[ ] TDD: UpdateCreationFailedCommand handler
    [ ] Write test for handling UpdateCreationFailedCommand
    [ ] Run test - verify it fails
    [ ] Implement handle(UpdateCreationFailedCommand)
        [ ] Find SecuritySystem by ID (throw if not found)
        [ ] Update state to CREATION_FAILED
        [ ] Set rejection reason
        [ ] Save to repository
    [ ] Run test - verify it passes
    
[ ] TDD: NoteLocationCreatedCommand handler
    [ ] Write test for handling NoteLocationCreatedCommand
    [ ] Run test - verify it fails
    [ ] Implement handle(NoteLocationCreatedCommand)
        [ ] Find SecuritySystem by ID (throw if not found)
        [ ] Update locationId
        [ ] Update state to DISARMED
        [ ] Save to repository
        [ ] Return LocationNoted reply
    [ ] Run test - verify it passes
    [ ] Extract common repository operations
    
[ ] TDD: Command handler configuration
    [ ] Write integration test for command handler wiring
    [ ] Run test - verify it fails
    [ ] Create CommandHandlerConfiguration class
        [ ] Define SecuritySystemCommandHandler bean
        [ ] Configure command dispatcher with Eventuate
    [ ] Run test - verify commands are routed correctly
```

## Thread 8: Application Configuration for Component Testing

**Goal**: Set up application configuration, Docker support, and database setup needed for component tests.

```text
Create the necessary configuration files and Docker setup to support component testing with Testcontainers.

Tasks:
[ ] Database setup and migrations
    [ ] Create Flyway migration for saga instance tables (saga_instance, saga_state_instance)
    [ ] Create migration for SecuritySystem new fields (customerId, locationId, rejectionReason)
    [ ] Create migration for SecuritySystemState enum values (CREATION_PENDING, CREATION_FAILED)
    [ ] Test migrations with clean database
    
[ ] Application configuration files
    [ ] Create application.yml for orchestration-main:
        [ ] Configure Spring datasource properties
        [ ] Configure Eventuate Tram saga tables
        [ ] Configure Kafka bootstrap servers
        [ ] Configure service channels
    [ ] Create application.yml for security-system-service:
        [ ] Configure datasource and Kafka
        [ ] Configure command dispatcher channel
    [ ] Create application.yml for customer-service:
        [ ] Configure datasource and Kafka
        [ ] Configure command dispatcher channel
    
[ ] Docker configuration for component tests
    [ ] Create Dockerfile for orchestration-main:
        [ ] Based on appropriate Java image
        [ ] Copy application JAR
        [ ] Set entrypoint for Spring Boot
    [ ] Create Dockerfile for security-system-service
    [ ] Create Dockerfile for customer-service
    [ ] Verify each Dockerfile builds successfully
    
[ ] Gradle configuration for component tests
    [ ] Apply ComponentTestsPlugin in each service's build.gradle
    [ ] Add Testcontainers dependencies to each service
    [ ] Configure ServiceContainer to use Dockerfile
    [ ] Verify gradle componentTest task runs successfully
```

## Thread 9: Customer Service Command Handlers

**Goal**: Implement command handlers in the customer-service to process location creation with security system using TDD.

```text
Add command handlers to the existing customer-service. Follow strict TDD: write failing test -> make it pass -> refactor.

Tasks:
[ ] TDD: Update Location entity
    [ ] Write test for Location with securitySystemId field
    [ ] Run test - verify it fails (field doesn't exist)
    [ ] Add securitySystemId field to Location entity
    [ ] Run test - verify it passes
    [ ] Create database migration script
    
[ ] TDD: Customer exists validation
    [ ] Write test for CreateLocationWithSecuritySystemCommand when customer not found
    [ ] Run test - verify it fails (handler doesn't exist)
    [ ] Create CustomerCommandHandler class with @EventuateCommandHandler
        [ ] Inject CustomerRepository and LocationRepository
        [ ] Implement handle(CreateLocationWithSecuritySystemCommand) - customer check only
        [ ] Return CustomerNotFound if customer doesn't exist
    [ ] Run test - verify it passes
    
[ ] TDD: New location creation
    [ ] Write test for creating new location with security system
    [ ] Run test - verify it fails
    [ ] Extend handler to create new location
        [ ] Find location by customerId and name
        [ ] If not found, create new location
        [ ] Set securitySystemId
        [ ] Save location
        [ ] Return LocationCreatedWithSecuritySystem(locationId)
    [ ] Run test - verify it passes
    
[ ] TDD: Existing location without security system
    [ ] Write test for updating existing location
    [ ] Run test - verify it fails
    [ ] Extend handler to update existing location
        [ ] If location exists but has no securitySystemId, update it
        [ ] Save location
        [ ] Return LocationCreatedWithSecuritySystem(locationId)
    [ ] Run test - verify it passes
    
[ ] TDD: Location already has security system
    [ ] Write test for location with existing security system
    [ ] Run test - verify it fails
    [ ] Add validation for existing securitySystemId
        [ ] If location has securitySystemId, return LocationAlreadyHasSecuritySystem
    [ ] Run test - verify it passes
    [ ] Extract location finding/creation logic
    
[ ] TDD: Command handler configuration
    [ ] Write integration test for command routing
    [ ] Run test - verify it fails
    [ ] Create or update CommandHandlerConfiguration
        [ ] Define CustomerCommandHandler bean
        [ ] Configure command dispatcher
    [ ] Run test - verify commands are handled correctly
```

## Thread 10: Component Testing - Happy Path

**Goal**: Create component tests for each service using Testcontainers for real Kafka and database infrastructure.

```text
Write component tests for each service in isolation using Testcontainers, following the pattern from CustomerServiceComponentTest.

Tasks:
[ ] Component Test Setup: Common Test Infrastructure
    [ ] Apply ComponentTestsPlugin in each service's build.gradle
    [ ] Add Testcontainers dependencies (testcontainers, kafka, postgresql/mysql)
    [ ] Add Eventuate test container dependencies
    [ ] Verify gradle componentTest task works
    
[ ] Component Test: Orchestration Service
    [ ] Create OrchestrationServiceComponentTest class
    [ ] Set up Testcontainers:
        [ ] EventuateKafkaNativeCluster with network
        [ ] EventuateDatabaseContainer for PostgreSQL/MySQL
        [ ] ServiceContainer for orchestration service
    [ ] Implement @BeforeAll to start containers
    [ ] Write test methods:
        [ ] shouldStart() - verify service starts successfully
        [ ] shouldExposeSwaggerUI() - verify REST endpoints available
        [ ] shouldInitiateSaga() - POST /securitysystems returns 201
        [ ] shouldCompleteFirstStep() - verify CompletableFuture completion
    [ ] Verify message published to Kafka topics
    [ ] Run component tests - verify they pass
    
[ ] Component Test: Security System Service
    [ ] Create SecuritySystemServiceComponentTest class
    [ ] Set up Testcontainers:
        [ ] EventuateKafkaNativeCluster with network
        [ ] EventuateDatabaseContainer for database
        [ ] ServiceContainer for security-system-service
    [ ] Implement @BeforeAll to start containers
    [ ] Write test methods:
        [ ] shouldHandleCreateSecuritySystemCommand()
            [ ] Send command via Kafka
            [ ] Verify SecuritySystem created in database
            [ ] Verify reply message sent
        [ ] shouldHandleUpdateCreationFailedCommand()
            [ ] Create SecuritySystem first
            [ ] Send failure command
            [ ] Verify state updated to CREATION_FAILED
        [ ] shouldHandleNoteLocationCreatedCommand()
            [ ] Create SecuritySystem first
            [ ] Send note location command
            [ ] Verify state updated to DISARMED
    [ ] Run component tests - verify they pass
    
[ ] Component Test: Customer Service
    [ ] Create CustomerServiceComponentTest class
    [ ] Set up Testcontainers:
        [ ] EventuateKafkaNativeCluster with network
        [ ] EventuateDatabaseContainer for database
        [ ] ServiceContainer for customer-service
    [ ] Implement @BeforeAll to start containers
    [ ] Write test methods:
        [ ] shouldCreateNewLocationWithSecuritySystem()
            [ ] Create customer in database
            [ ] Send CreateLocationWithSecuritySystemCommand
            [ ] Verify location created with securitySystemId
            [ ] Verify reply message sent
        [ ] shouldUpdateExistingLocation()
            [ ] Create customer and location
            [ ] Send command to update location
            [ ] Verify location updated
        [ ] shouldRejectDuplicateSecuritySystem()
            [ ] Create location with existing securitySystemId
            [ ] Send command for same location
            [ ] Verify LocationAlreadyHasSecuritySystem reply
    [ ] Run component tests - verify they pass
    
```

## Thread 11: End-to-End Testing - Happy Path

**Goal**: Create end-to-end tests for the happy path scenario with all services running.

```text
Create end-to-end tests following the pattern from CustomersAndOrdersEndToEndTest using ApplicationUnderTest.

Tasks:
[ ] Set up EndToEndTestsPlugin
    [ ] Copy EndToEndTestsPlugin.groovy from https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders/blob/master/buildSrc/src/main/groovy/EndToEndTestsPlugin.groovy to realguardio buildSrc
    [ ] Ensure buildSrc project is configured correctly
    [ ] Verify plugin is available for use
    
[ ] Create end-to-end-tests module
    [ ] Create new gradle module for E2E tests
    [ ] Add dependencies on all service APIs
    [ ] Apply EndToEndTestsPlugin in build.gradle
    [ ] Add Spring Boot test dependencies
    
[ ] Create ApplicationUnderTest class
    [ ] Define container setup for all services
    [ ] Configure shared Kafka cluster
    [ ] Configure shared database
    [ ] Implement start() method to launch all containers
    [ ] Add methods to get service URLs
    
[ ] TDD: SecuritySystemSagaEndToEndTest - Happy Path
    [ ] Write shouldApproveSecuritySystemCreation() test
    [ ] Implement test:
        [ ] Create customer via REST
        [ ] Create security system via POST /securitysystems
        [ ] Poll for SecuritySystem in DISARMED state
        [ ] Verify location has securitySystemId
    [ ] Run test - verify it passes
    
[ ] Test Swagger UI endpoints
    [ ] Write test to verify Swagger UI available
    [ ] Test each service's Swagger endpoint
    [ ] Verify API documentation accessible
```

## Thread 12: Component Testing - Failure Scenarios

**Goal**: Create component tests for failure scenarios using Testcontainers.

```text
Write component tests for failure scenarios using Testcontainers, testing each service's error handling in isolation.

Tasks:
[ ] Component Test: Orchestration Service - Failure Handling
    [ ] Extend OrchestrationServiceComponentTest with failure tests
    [ ] Write shouldHandleCustomerNotFound() test
    [ ] Implement test:
        [ ] Start saga with valid request
        [ ] Simulate CustomerNotFound reply via Kafka
        [ ] Verify compensation command sent
        [ ] Query database to verify saga state
    [ ] Write shouldHandleLocationConflict() test:
        [ ] Start saga with valid request
        [ ] Simulate LocationAlreadyHasSecuritySystem reply
        [ ] Verify compensation triggered
        [ ] Verify saga marked as failed
    [ ] Run component tests - verify they pass
    
[ ] Component Test: Security System Service - Compensation
    [ ] Extend SecuritySystemServiceComponentTest with compensation tests
    [ ] Write shouldHandleCompensation() test
    [ ] Implement test:
        [ ] Create SecuritySystem in CREATION_PENDING state
        [ ] Send UpdateCreationFailedCommand via Kafka
        [ ] Query database to verify state = CREATION_FAILED
        [ ] Verify rejection reason stored correctly
    [ ] Write shouldBeIdempotent() test:
        [ ] Send same command twice
        [ ] Verify no errors on duplicate
        [ ] Verify state remains consistent
    [ ] Run component tests - verify they pass
    
[ ] Component Test: Customer Service - Error Responses
    [ ] Extend CustomerServiceComponentTest with error tests
    [ ] Write shouldReturnCustomerNotFound() test
    [ ] Implement test:
        [ ] Send command with non-existent customerId
        [ ] Verify CustomerNotFound reply sent via Kafka
        [ ] Query database to verify no location created
    [ ] Write shouldRejectDuplicateSecuritySystem() test:
        [ ] Create location with securitySystemId
        [ ] Send command for same location
        [ ] Verify LocationAlreadyHasSecuritySystem reply
        [ ] Verify original securitySystemId unchanged
    [ ] Run component tests - verify they pass
```

## Thread 13: End-to-End Testing - Failure Scenarios

**Goal**: Create end-to-end tests for failure scenarios with all services running.

```text
Create E2E tests for failure scenarios following the CustomersAndOrdersEndToEndTest pattern.

Tasks:
[ ] TDD: SecuritySystemSagaEndToEndTest - Failure Scenarios
    [ ] Write shouldRejectUnknownCustomer() test
    [ ] Implement test:
        [ ] POST with non-existent customerId
        [ ] Poll for SecuritySystem in CREATION_FAILED state
        [ ] Verify rejection reason = "Customer not found"
    [ ] Run test - verify it passes
    
[ ] TDD: Location conflict scenario
    [ ] Write shouldRejectDuplicateSecuritySystem() test
    [ ] Implement test:
        [ ] Create customer via REST
        [ ] Create security system for a location
        [ ] POST for same location again
        [ ] Poll for new system in CREATION_FAILED state
        [ ] Verify existing assignment unchanged
    [ ] Run test - verify it passes
    
[ ] TDD: Concurrent request handling
    [ ] Write shouldHandleConcurrentRequests() test
    [ ] Implement test:
        [ ] Create customer
        [ ] Send multiple concurrent POST requests
        [ ] Verify only one succeeds
        [ ] Verify others fail appropriately
    [ ] Run all E2E tests - verify they pass
```

## Thread 14: Production Deployment Configuration

**Goal**: Update the existing docker-compose.yml to include the new orchestration service.

```text
Add the orchestration service to the existing docker-compose.yml configuration.

Tasks:
[ ] Update existing docker-compose.yml
    [ ] Add orchestration-service definition
    [ ] Configure environment variables for database connection
    [ ] Configure environment variables for Kafka connection
    [ ] Set appropriate service dependencies
    [ ] Configure network settings to match existing services
    [ ] Test full system startup with docker-compose up
    [ ] Verify orchestration service connects to Kafka and database
    [ ] Verify service endpoints are accessible
```

## Thread 15: Final Refinements and Documentation

**Goal**: Apply final polish, refactoring, and complete documentation.

```text
Refactor code for clarity, add any missing error handling, and ensure comprehensive documentation.

Tasks:
[ ] Code refactoring
    [ ] Extract magic strings to constants
    [ ] Improve error messages
    [ ] Add proper logging statements
    [ ] Remove any code duplication
    [ ] Ensure consistent code style
    
[ ] Complete JavaDoc documentation
    [ ] Document all public methods
    [ ] Add class-level documentation
    [ ] Document saga flow in CreateSecuritySystemSaga
    [ ] Add package-info.java where appropriate
    
[ ] Update README files
    [ ] Create README for orchestration service
    [ ] Document API endpoints with examples
    [ ] Add architecture diagram
    [ ] Include deployment instructions
    
[ ] Final testing pass
    [ ] Run all unit tests
    [ ] Run all integration tests
    [ ] Execute E2E test suite
    [ ] Verify no test failures
    [ ] Check code coverage metrics
```

## Change History

- **2025-08-28**: Thread 1 - Replaced manual application start test with integration test that verifies application context starts using @SpringBootTest
- **2025-08-28**: Thread 2 - Updated to use Java records instead of classes for all commands/replies and follow strict TDD approach
- **2025-08-28**: Thread 3 - Restructured to follow TDD practices for service proxy interfaces
- **2025-08-28**: Thread 4 - Restructured to follow TDD practices for saga definition with step-by-step test-driven development
- **2025-08-28**: Thread 5 - Restructured to follow TDD practices for saga service with CompletableFuture management
- **2025-08-28**: Thread 6 - Updated to use records for DTOs and follow strict TDD for controller implementation
- **2025-08-28**: Thread 7 - Restructured to follow TDD practices for Security System command handlers
- **2025-08-28**: Thread 8 - Created new thread for application configuration and Docker setup needed for component tests (moved from original Thread 11)
- **2025-08-28**: Thread 9 - Customer Service command handlers with TDD approach
- **2025-08-28**: Thread 10 - Component tests using Testcontainers pattern for happy path scenarios
- **2025-08-28**: Thread 11 - End-to-End Testing for happy path (split from original combined E2E thread)
- **2025-08-28**: Thread 12 - Component Testing for failure scenarios (moved after E2E happy path as requested)
- **2025-08-28**: Thread 13 - End-to-End Testing for failure scenarios (split from original combined E2E thread)
- **2025-08-28**: Thread 14 - Production deployment configuration - updating existing docker-compose.yml (simplified)
- **2025-08-28**: Thread 15 - Final refinements and documentation
- **2025-08-28**: Removed integration tests involving multi-service setup, removed timing verification tests, removed retry/idempotency component tests, removed invented requirements, removed deployment documentation creation
- **2025-08-28**: Removed RED:, GREEN:, and REFACTOR: labels from all tasks as they're not needed - TDD approach is implied