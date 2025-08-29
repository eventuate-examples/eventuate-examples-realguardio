# Security System Creation Saga - Technical Specification

## Overview

Implement a POST `/securitysystems` endpoint that creates security systems using the Eventuate Tram Saga pattern to ensure consistency across the SecuritySystemService and CustomerService microservices.

## Architecture

### Service Topology
- **realguardio-orchestration-service** (NEW): Hosts the saga orchestration logic and REST endpoint
- **security-system-service**: Manages SecuritySystem entities
- **customer-service**: Manages Customer and Location entities
- **Message Broker**: Kafka (via Eventuate Tram)

### Key Principles
- Services communicate exclusively through commands/replies (no direct database access)
- Saga ensures eventual consistency across services
- Failed sagas are tracked for potential retry/remediation

## Saga Definition

### Saga Flow

| Step | Service | Transaction | Compensation |
|------|---------|-------------|--------------|
| 1 | Security System Service | `securitySystemID = CreateSecuritySystem(locationName)`<br/>Sets state to CREATION_PENDING | `updateCreationFailed(securitySystemID)`<br/>Sets state to CREATION_FAILED |
| 2 | Customer Service | `locationID = Create Location with SecuritySystem(customerId, locationName, securitySystemID)` | - |
| 3 | Security System Service | `noteLocationCreated(securitySystemID, locationID)`<br/>Sets state to DISARMED | - |

### Step 1: Create SecuritySystem

**Command**: `CreateSecuritySystemCommand`
```java
public class CreateSecuritySystemCommand {
    private String locationName;
}
```

**Success Reply**: `SecuritySystemCreated`
```java
public class SecuritySystemCreated {
    private Long securitySystemId;
}
```

**Compensation Command**: `UpdateCreationFailedCommand`
```java
public class UpdateCreationFailedCommand {
    private Long securitySystemId;
    private String rejectionReason;
}
```

### Step 2: Create Location with SecuritySystem

**Command**: `CreateLocationWithSecuritySystemCommand`
```java
public class CreateLocationWithSecuritySystemCommand {
    private Long customerId;
    private String locationName;
    private Long securitySystemId;
}
```

**Success Reply**: `LocationCreatedWithSecuritySystem`
```java
public class LocationCreatedWithSecuritySystem {
    private Long locationId;
}
```

**Failure Replies**:
```java
@FailureReply
public class CustomerNotFound {}

@FailureReply
public class LocationAlreadyHasSecuritySystem {}
```

### Step 3: Note Location Created

**Command**: `NoteLocationCreatedCommand`
```java
public class NoteLocationCreatedCommand {
    private Long securitySystemId;
    private Long locationId;
}
```

**Success Reply**: `LocationNoted`
```java
public class LocationNoted {}
```

## Data Models

### SecuritySystem Entity Updates

Add the following fields to the existing SecuritySystem entity:
```java
private Long customerId;
private Long locationId;
private String rejectionReason; // Set when state is CREATION_FAILED
```

### SecuritySystemState Enum Updates

Add the following states:
```java
CREATION_PENDING,  // Initial state during saga execution
CREATION_FAILED,   // Saga failed, system awaits retry/cleanup
DISARMED,         // Successfully created and ready for use
ARMED             // (existing state)
```

### CreateSecuritySystemSagaData

```java
public class CreateSecuritySystemSagaData {
    private Long securitySystemId;
    private Long customerId;
    private String locationName;
    private Long locationId; // Set after location creation
    private String rejectionReason; // Set on failure
    
    // Constructor, getters, setters
}
```

## API Specification

### REST Endpoint

**Request**:
```
POST /securitysystems
Content-Type: application/json

{
    "customerId": 12345,
    "locationName": "Main Office"
}
```

**Response (Success)**:
```
HTTP/1.1 201 Created
Content-Type: application/json

{
    "securitySystemId": 67890
}
```

**Response (Validation Error)**:
```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
    "error": "Missing required field: customerId"
}
```

### Controller Implementation Pattern

```java
@RestController
public class SecuritySystemController {
    private final SecuritySystemSagaService sagaService;
    
    @PostMapping("/securitysystems")
    public CompletableFuture<ResponseEntity<CreateSecuritySystemResponse>> createSecuritySystem(
            @RequestBody CreateSecuritySystemRequest request) {
        // Returns response after first step creates the SecuritySystem
        CompletableFuture<Long> securitySystemIdFuture = sagaService.createSecuritySystem(
            request.getCustomerId(), 
            request.getLocationName()
        );
        
        return securitySystemIdFuture.thenApply(securitySystemId ->
            ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateSecuritySystemResponse(securitySystemId))
        );
    }
}
```

### Sending Back the HTTP Response with the securitySystemID

The orchestration service uses a CompletableFuture-based approach to send the HTTP response after the first saga step:

```java
@Service
public class SecuritySystemSagaService {
    private final SagaInstanceFactory sagaInstanceFactory;
    private final CreateSecuritySystemSaga saga;
    
    // Map to track pending saga responses
    private final ConcurrentHashMap<String, CompletableFuture<Long>> pendingSagaResponses = 
        new ConcurrentHashMap<>();
    
    public CompletableFuture<Long> createSecuritySystem(Long customerId, String locationName) {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData();
        sagaData.setCustomerId(customerId);
        sagaData.setLocationName(locationName);
        
        // Create a CompletableFuture for the response
        CompletableFuture<Long> responseFuture = new CompletableFuture<>();
        
        // Start the saga
        SagaInstance sagaInstance = sagaInstanceFactory.create(saga, sagaData);
        
        // Store the future with the saga ID
        pendingSagaResponses.put(sagaInstance.getId(), responseFuture);
        
        return responseFuture;
    }
    
    // Called by the saga to complete the future
    public void completeSecuritySystemCreation(String sagaId, Long securitySystemId) {
        CompletableFuture<Long> future = pendingSagaResponses.remove(sagaId);
        if (future != null) {
            future.complete(securitySystemId);
        }
    }
}
```

The saga includes an onReply handler for the first step that completes the future:

```java
@Component
public class CreateSecuritySystemSaga implements SimpleSaga<CreateSecuritySystemSagaData> {
    private final SecuritySystemSagaService sagaService;
    
    private SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition =
        step()
            .invokeParticipant(this::createSecuritySystem)
            .onReply(SecuritySystemCreated.class, this::handleSecuritySystemCreated)
            .withCompensation(this::updateCreationFailed)
        .step()
            // ... rest of saga definition
        .build();
    
    private void handleSecuritySystemCreated(CreateSecuritySystemSagaData data, 
                                            SecuritySystemCreated reply) {
        data.setSecuritySystemId(reply.getSecuritySystemId());
        // Complete the HTTP response future
        sagaService.completeSecuritySystemCreation(
            getSagaId(), // Method to get current saga instance ID
            reply.getSecuritySystemId()
        );
    }
}
```

**Key Points**:
- The controller returns a `CompletableFuture<ResponseEntity>`
- A `ConcurrentHashMap` tracks saga IDs to CompletableFutures
- The first step's onReply handler completes the future with the securitySystemID
- The HTTP response is sent as soon as the SecuritySystem is created
- Remaining saga steps execute asynchronously

**Important Note for Production Deployments**:
This CompletableFuture approach works for a single instance deployment. For multiple instances, a per-instance subscriber (to an event or the reply queue) is required since the instance that handles the reply is not guaranteed to be the same as the one that handles the POST request. Alternative solutions for multi-instance deployments include:
- Using a distributed cache (e.g., Redis) to store the pending futures
- Implementing per-instance reply channels
- Using webhooks or polling for async responses

## Service Implementation Details

### Orchestration Service Saga Definition

```java
@Component
public class CreateSecuritySystemSaga implements SimpleSaga<CreateSecuritySystemSagaData> {
    private final SecuritySystemSagaService sagaService;
    private final SecuritySystemServiceProxy securitySystemServiceProxy;
    private final CustomerServiceProxy customerServiceProxy;
    
    private SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition =
        step()
            .invokeParticipant(this::createSecuritySystem)
            .onReply(SecuritySystemCreated.class, this::handleSecuritySystemCreated)
            .withCompensation(this::updateCreationFailed)
        .step()
            .invokeParticipant(this::createLocationWithSecuritySystem)
            .onReply(CustomerNotFound.class, this::handleCustomerNotFound)
            .onReply(LocationAlreadyHasSecuritySystem.class, this::handleLocationAlreadyHasSecuritySystem)
        .step()
            .invokeParticipant(this::noteLocationCreated)
        .build();
    
    private void handleSecuritySystemCreated(CreateSecuritySystemSagaData data, 
                                            SecuritySystemCreated reply) {
        data.setSecuritySystemId(reply.getSecuritySystemId());
        // Complete the HTTP response future
        sagaService.completeSecuritySystemCreation(
            getSagaId(), 
            reply.getSecuritySystemId()
        );
    }
    
    private void handleCustomerNotFound(CreateSecuritySystemSagaData data, CustomerNotFound reply) {
        data.setRejectionReason("Customer not found");
    }
    
    private void handleLocationAlreadyHasSecuritySystem(CreateSecuritySystemSagaData data, 
                                                       LocationAlreadyHasSecuritySystem reply) {
        data.setRejectionReason("Location already has security system");
    }
    
    private CommandWithDestination updateCreationFailed(CreateSecuritySystemSagaData data) {
        return securitySystemServiceProxy.updateCreationFailed(
            data.getSecuritySystemId(), 
            data.getRejectionReason()
        );
    }
}
```

### SecuritySystemService Command Handler

```java
@EventuateCommandHandler
public class SecuritySystemCommandHandler {
    
    @CommandHandler
    public SecuritySystemCreated handle(CreateSecuritySystemCommand cmd) {
        SecuritySystem system = new SecuritySystem();
        system.setLocationName(cmd.getLocationName());
        system.setState(SecuritySystemState.CREATION_PENDING);
        system = repository.save(system);
        return new SecuritySystemCreated(system.getId());
    }
    
    @CommandHandler
    public void handle(UpdateCreationFailedCommand cmd) {
        SecuritySystem system = repository.findById(cmd.getSecuritySystemId())
            .orElseThrow();
        system.setState(SecuritySystemState.CREATION_FAILED);
        system.setRejectionReason(cmd.getRejectionReason());
        repository.save(system);
    }
    
    @CommandHandler
    public LocationNoted handle(NoteLocationCreatedCommand cmd) {
        SecuritySystem system = repository.findById(cmd.getSecuritySystemId())
            .orElseThrow();
        system.setLocationId(cmd.getLocationId());
        system.setState(SecuritySystemState.DISARMED);
        repository.save(system);
        return new LocationNoted();
    }
}
```

### CustomerService Command Handler

```java
@EventuateCommandHandler
public class CustomerCommandHandler {
    
    @CommandHandler
    public Object handle(CreateLocationWithSecuritySystemCommand cmd) {
        // Validate customer exists
        Customer customer = customerRepository.findById(cmd.getCustomerId())
            .orElse(null);
        
        if (customer == null) {
            return new CustomerNotFound();
        }
        
        // Find or create location
        Location location = locationRepository
            .findByCustomerIdAndName(cmd.getCustomerId(), cmd.getLocationName())
            .orElseGet(() -> createNewLocation(customer, cmd.getLocationName()));
        
        // Check if location already has a security system
        if (location.getSecuritySystemId() != null) {
            return new LocationAlreadyHasSecuritySystem();
        }
        
        // Update location with security system
        location.setSecuritySystemId(cmd.getSecuritySystemId());
        location = locationRepository.save(location);
        
        return new LocationCreatedWithSecuritySystem(location.getId());
    }
}
```

## Error Handling

### Saga Failure Scenarios

1. **Customer Not Found**: 
   - Saga fails at Step 2
   - SecuritySystem state changes to CREATION_FAILED
   - API can return appropriate error to client

2. **Location Already Has Security System**:
   - Saga fails at Step 2
   - SecuritySystem state changes to CREATION_FAILED
   - Existing security system assignment preserved

3. **SecuritySystem Finalization Failure**:
   - Retry automatically via Kafka redelivery
   - If persistent failure, manual intervention required

### Compensation Strategy

- Step 1 compensation: `updateCreationFailed()` changes state to CREATION_FAILED
- Step 2 has no compensation (Customer Service is responsible for its own state)
- Step 3 has no compensation (final step)

## Testing Plan

### Unit Tests

1. **Saga Definition Tests**
   - Test saga step sequencing
   - Verify compensation triggers correctly
   - Test reply handler routing

2. **Command Handler Tests**
   - Test each command handler in isolation
   - Verify state transitions
   - Test error scenarios

3. **Service Tests**
   - Mock repository interactions
   - Test business logic validation
   - Verify response formats

### Integration Tests

1. **Happy Path Test**
   ```
   Given: Valid customer and new location
   When: POST /securitysystems
   Then: SecuritySystem created with DISARMED state
   And: Location updated with securitySystemId
   ```

2. **Customer Not Found Test**
   ```
   Given: Invalid customerId
   When: POST /securitysystems
   Then: SecuritySystem in CREATION_FAILED state
   And: No location created
   ```

3. **Location Conflict Test**
   ```
   Given: Location with existing securitySystemId
   When: POST /securitysystems
   Then: New SecuritySystem in CREATION_FAILED state
   And: Existing assignment unchanged
   ```

### End-to-End Tests

1. **Full Saga Execution**
   - Deploy all services with Kafka
   - Submit create request
   - Verify all state changes
   - Check message flow in Kafka

2. **Compensation Test**
   - Force failure at Step 2
   - Verify compensation executes
   - Check final states

3. **Retry Behavior**
   - Simulate transient failures
   - Verify Kafka retry mechanism
   - Ensure idempotency

## Configuration

### Eventuate Tram Configuration

```yaml
eventuate:
  tram:
    sagas:
      orchestration:
        instance-table: saga_instance
        state-table: saga_state
    messaging:
      kafka:
        bootstrap-servers: localhost:9092
```

### Service Channels

- SecuritySystemService: `security-system-service`
- CustomerService: `customer-service`
- Orchestration replies: `orchestration-service-reply`

## Deployment Considerations

1. **Service Startup Order**
   - Kafka must be running first
   - Database migrations before service start
   - Orchestration service can start anytime

2. **Database Schema**
   - Add migration for new SecuritySystem fields
   - Create saga instance tables
   - Index on customerId and locationId

3. **Monitoring**
   - Track saga completion rates
   - Monitor CREATION_FAILED systems
   - Alert on compensation executions

## Future Enhancements

1. **Retry Mechanism**
   - API to retry failed sagas
   - Scheduled cleanup of old failures

2. **Audit Trail**
   - Log all state transitions
   - Track who initiated creation

3. **Batch Operations**
   - Support creating multiple systems
   - Bulk status checking

## References

- [Eventuate Tram Sagas Documentation](https://eventuate.io/docs/manual/eventuate-tram/latest/getting-started-eventuate-tram-sagas.html)
- Example Application: https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders
- Component Test Example: https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders/blob/master/customer-service/customer-service-main/src/componentTest/java/io/eventuate/examples/tram/sagas/customersandorders/customers/CustomerServiceComponentTest.java
- End-to-End Test Example: https://github.com/eventuate-tram/eventuate-tram-sagas-examples-customers-and-orders/blob/master/end-to-end-tests/src/endToEndTest/java/io/eventuate/examples/tram/sagas/customersandorders/endtoendtests/CustomersAndOrdersEndToEndTest.java