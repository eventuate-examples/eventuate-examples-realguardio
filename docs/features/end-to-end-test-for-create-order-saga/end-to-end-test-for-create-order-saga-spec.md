# End-to-End Test for Create Order Saga - Technical Specification

## Executive Summary

This specification defines the implementation of end-to-end tests for the RealGuardio Create Order Saga, validating the complete workflow from customer creation through security system provisioning. The tests will ensure proper integration between the customer-service, orchestration-service, and security-service components using Docker Compose and Testcontainers.

## Project Overview

### Purpose
Create comprehensive end-to-end tests that validate the Create Order Saga workflow, ensuring that:
- Customers can be successfully created
- Security systems can be ordered through the orchestration service
- Security systems are properly assigned location IDs through the saga process

### Scope
The initial implementation will focus on the happy path scenario with the foundation to add error cases and additional saga tests in the future.

## Architecture

### Project Structure

```
end-to-end-tests/
├── build.gradle
├── settings.gradle
├── buildSrc/
│   └── src/main/groovy/
│       └── EndToEndTestsPlugin.groovy
└── src/
    └── endToEndTest/
        ├── java/
        │   └── com/realguardio/endtoendtests/
        │       ├── clients/
        │       │   ├── CustomerServiceClient.java
        │       │   ├── OrchestrationServiceClient.java
        │       │   └── SecurityServiceClient.java
        │       ├── dto/
        │       │   ├── CreateCustomerRequest.java
        │       │   ├── CreateCustomerResponse.java
        │       │   ├── CreateSecuritySystemRequest.java
        │       │   ├── CreateSecuritySystemResponse.java
        │       │   └── SecuritySystemDetails.java
        │       ├── utils/
        │       │   ├── JwtTokenHelper.java
        │       │   └── Eventually.java
        │       └── tests/
        │           └── CreateOrderSagaE2ETest.java
        └── resources/
            └── logback-test.xml
```

### Technology Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Test Framework | JUnit 5 | Consistency with component tests |
| HTTP Client | RestAssured | Proven in component tests, simple API |
| Container Management | Testcontainers ComposeContainer | Simulates production environment |
| Authentication | JWT via JwtTokenHelper | Reuses existing patterns |
| Logging | SLF4J with Logback | Standard logging framework |
| Build Tool | Gradle with custom plugin | Follows project conventions |
| Retry Logic | Eventually pattern | Consistent with component tests |

## Implementation Details

### 1. Gradle Configuration

#### settings.gradle
```groovy
rootProject.name = 'end-to-end-tests'
```

#### build.gradle
```groovy
plugins {
    id 'java'
    id 'groovy'
}

apply plugin: EndToEndTestsPlugin

group = 'com.realguardio'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Test Framework
    endToEndTestImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    endToEndTestRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    
    // RestAssured for API Testing
    endToEndTestImplementation 'io.rest-assured:rest-assured:5.3.2'
    
    // Testcontainers
    endToEndTestImplementation 'org.testcontainers:testcontainers:1.19.1'
    endToEndTestImplementation 'org.testcontainers:junit-jupiter:1.19.1'
    
    // Logging
    endToEndTestImplementation 'org.slf4j:slf4j-api:2.0.9'
    endToEndTestImplementation 'ch.qos.logback:logback-classic:1.4.11'
    
    // Assertions
    endToEndTestImplementation 'org.assertj:assertj-core:3.24.2'
    
    // JSON Processing
    endToEndTestImplementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
}

endToEndTest {
    useJUnitPlatform()
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

### 2. Service Client Implementation

#### CustomerServiceClient.java
```java
package com.realguardio.endtoendtests.clients;

import com.realguardio.endtoendtests.dto.CreateCustomerRequest;
import com.realguardio.endtoendtests.dto.CreateCustomerResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClient.class);
    private final String baseUrl;
    private final String authToken;
    
    public CustomerServiceClient(String host, int port, String authToken) {
        this.baseUrl = String.format("http://%s:%d", host, port);
        this.authToken = authToken;
    }
    
    public CreateCustomerResponse createCustomer(CreateCustomerRequest request) {
        logger.info("Creating customer: {}", request);
        
        return RestAssured.given()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/customers")
            .then()
            .statusCode(200)
            .extract()
            .as(CreateCustomerResponse.class);
    }
}
```

### 3. Test Implementation

#### CreateOrderSagaE2ETest.java
```java
package com.realguardio.endtoendtests.tests;

import com.realguardio.endtoendtests.clients.*;
import com.realguardio.endtoendtests.dto.*;
import com.realguardio.endtoendtests.utils.JwtTokenHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.realguardio.endtoendtests.utils.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CreateOrderSagaE2ETest {
    private static final Logger logger = LoggerFactory.getLogger(CreateOrderSagaE2ETest.class);
    
    @Container
    private static ComposeContainer environment = new ComposeContainer(
            new File("../docker-compose.yml"))
            .withExposedService("customer-service", 8080, 
                Wait.forHttp("/actuator/health").forStatusCode(200))
            .withExposedService("orchestration-service", 8081, 
                Wait.forHttp("/actuator/health").forStatusCode(200))
            .withExposedService("security-service", 8082, 
                Wait.forHttp("/actuator/health").forStatusCode(200))
            .withExposedService("iam-service", 9000, 
                Wait.forHttp("/actuator/health").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(5));
    
    private CustomerServiceClient customerClient;
    private OrchestrationServiceClient orchestrationClient;
    private SecurityServiceClient securityClient;
    private String authToken;
    
    @BeforeEach
    void setup() {
        // Get JWT token
        authToken = JwtTokenHelper.getJwtToken("localhost", 9000);
        
        // Initialize service clients with fixed ports from docker-compose.yml
        customerClient = new CustomerServiceClient("localhost", 8080, authToken);
        orchestrationClient = new OrchestrationServiceClient("localhost", 8081, authToken);
        securityClient = new SecurityServiceClient("localhost", 8082, authToken);
    }
    
    @Test
    void shouldCreateCustomerAndSecuritySystemWithLocationId() throws Exception {
        // Arrange - Create unique test data
        String uniqueEmail = "admin-" + UUID.randomUUID() + "@example.com";
        String locationName = "Office Front Door " + System.currentTimeMillis();
        
        // Act - Step 1: Create customer
        logger.info("Creating customer with email: {}", uniqueEmail);
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
            "Test Customer",
            new Administrator("Admin", "User", uniqueEmail)
        );
        CreateCustomerResponse customerResponse = customerClient.createCustomer(customerRequest);
        long customerId = customerResponse.getCustomerId();
        
        assertThat(customerId).isGreaterThan(0);
        logger.info("Customer created with ID: {}", customerId);
        
        // Act - Step 2: Create security system via orchestration
        logger.info("Creating security system for customer {} at location {}", 
            customerId, locationName);
        CreateSecuritySystemRequest securityRequest = new CreateSecuritySystemRequest(
            customerId, 
            locationName
        );
        CreateSecuritySystemResponse securityResponse = 
            orchestrationClient.createSecuritySystem(securityRequest);
        long securitySystemId = securityResponse.getSecuritySystemId();
        
        assertThat(securitySystemId).isGreaterThan(0);
        logger.info("Security system creation initiated with ID: {}", securitySystemId);
        
        // Assert - Step 3: Verify security system has locationId
        logger.info("Verifying security system {} has been assigned a locationId", 
            securitySystemId);
        
        eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
            SecuritySystemDetails details = 
                securityClient.getSecuritySystem(securitySystemId);
            
            assertThat(details).isNotNull();
            assertThat(details.getId()).isEqualTo(securitySystemId);
            assertThat(details.getLocationId()).isNotNull();
            assertThat(details.getLocationId()).isGreaterThan(0);
            
            logger.info("Security system {} successfully assigned locationId: {}", 
                securitySystemId, details.getLocationId());
        });
    }
}
```

### 4. DTO Definitions

#### CreateCustomerRequest.java
```java
package com.realguardio.endtoendtests.dto;

public class CreateCustomerRequest {
    private String name;
    private Administrator initialAdministrator;
    
    // Constructors, getters, setters
}
```

#### SecuritySystemDetails.java
```java
package com.realguardio.endtoendtests.dto;

public class SecuritySystemDetails {
    private Long id;
    private Long customerId;
    private Long locationId;
    private String status;
    
    // Constructors, getters, setters
}
```

### 5. Utility Classes

#### Eventually.java
```java
package com.realguardio.endtoendtests.utils;

import java.util.concurrent.TimeUnit;

public class Eventually {
    public static void eventually(int iterations, int delay, TimeUnit timeUnit, 
                                 Runnable runnable) throws InterruptedException {
        AssertionError lastError = null;
        
        for (int i = 0; i < iterations; i++) {
            try {
                runnable.run();
                return;
            } catch (AssertionError e) {
                lastError = e;
                if (i < iterations - 1) {
                    timeUnit.sleep(delay);
                }
            }
        }
        
        throw new AssertionError("Assertion failed after " + iterations + 
            " attempts", lastError);
    }
}
```

## Test Execution

### Running Tests Locally
```bash
# Run all end-to-end tests
./gradlew endToEndTest

# Run with detailed output
./gradlew endToEndTest --info

# Run specific test class
./gradlew endToEndTest --tests CreateOrderSagaE2ETest
```

### CI/CD Integration
The tests are automatically included in the `check` task:
```bash
./gradlew check  # Runs all tests including end-to-end
```

## Error Handling

### Retry Strategy
- Uses the Eventually pattern with 30-second timeout
- Polls every 500ms for saga completion
- Provides clear failure messages on timeout

### Assertion Strategy
- Simple assertions following component test patterns
- Clear error messages indicating which step failed
- Logs key events for debugging

## Data Management

### Test Isolation
- Uses UUID and timestamps for unique identifiers
- No cleanup required between tests
- Tests can run in parallel without conflicts

### Test Data Examples
- Customer email: `admin-{UUID}@example.com`
- Location name: `Office Front Door {timestamp}`
- Security system ID: Generated by service

## Future Enhancements

### Additional Test Scenarios
1. **Error Cases**
   - Invalid customer data
   - Network failures during saga
   - Service timeouts
   - Compensation scenarios

2. **Performance Tests**
   - Concurrent saga execution
   - Load testing with multiple customers
   - Saga completion time validation

3. **Additional Sagas**
   - Update security system saga
   - Delete customer saga
   - Location transfer saga

### Technical Improvements
1. **Test Reporting**
   - Allure reporting integration
   - Performance metrics collection
   - Failure screenshot capture

2. **Test Data Management**
   - Test data factories
   - Database seeding utilities
   - Test data cleanup jobs

## Dependencies and Prerequisites

### System Requirements
- Docker and Docker Compose installed
- Java 17 or higher
- Gradle 7.x or higher
- Minimum 8GB RAM for running all containers

### Service Dependencies
All services must be buildable as Docker images:
- customer-service
- orchestration-service
- security-service
- iam-service (authorization server)

### Network Configuration
Fixed ports as defined in docker-compose.yml:
- Customer Service: 8080
- Orchestration Service: 8081
- Security Service: 8082
- IAM Service: 9000
- Kafka: 9092
- PostgreSQL: 5432

## Maintenance Guidelines

### Adding New Tests
1. Create new test methods in existing test class for related scenarios
2. Create new test classes for different sagas
3. Follow naming convention: `{SagaName}E2ETest`

### Updating Service Clients
1. Keep client methods focused and single-purpose
2. Add new DTOs as needed for new endpoints
3. Maintain backwards compatibility

### Debugging Failed Tests
1. Check service logs via Docker Compose
2. Verify service health endpoints
3. Review timing/retry configurations
4. Validate test data uniqueness

## Conclusion

This specification provides a complete blueprint for implementing robust end-to-end tests for the Create Order Saga. The design emphasizes:
- Consistency with existing testing patterns
- Clear separation of concerns
- Maintainability and extensibility
- Reliable test execution

The implementation follows established patterns from component tests while providing true end-to-end validation of the complete saga workflow.