# Migration to New Eventuate Messaging APIs

## Overview

This document outlines the required changes to migrate from the legacy Eventuate messaging APIs to the newer annotation-based APIs. The new APIs simplify message handler configuration by using annotations instead of programmatic builders.

## Current State Analysis

### Components Using Legacy APIs

#### 1. Event Consumers (2 instances)

**CustomerEmployeeLocationEventConsumer**
- Location: `realguardio-security-system-service/location-roles-replica/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/locationroles/messaging/CustomerEmployeeLocationEventConsumer.java`
- Handles: `CustomerEmployeeAssignedLocationRole` events
- Configuration: `LocationRolesReplicaMessagingConfiguration.java`

**CustomerEventConsumer**
- Location: `realguardio-oso-integration-service/oso-event-subscribers/src/main/java/io/realguardio/osointegration/eventsubscribers/CustomerEventConsumer.java`
- Handles 4 event types:
  - `CustomerEmployeeAssignedCustomerRole`
  - `LocationCreatedForCustomer`
  - `SecuritySystemAssignedToLocation`
  - `CustomerEmployeeAssignedLocationRole`
- Configuration: `OsoEventSubscribersConfiguration.java`

#### 2. Command Handlers (2 instances)

**SecuritySystemCommandHandler**
- Location: `realguardio-security-system-service/security-system-api-messaging/src/main/java/io/eventuate/examples/realguardio/securitysystemservice/api/messaging/SecuritySystemCommandHandler.java`
- Handles 2 command types:
  - `CreateSecuritySystemCommand`
  - `NoteLocationCreatedCommand`
- Configuration: `SecuritySystemCommandHandlerConfiguration.java`

**CustomerCommandHandler**
- Location: `realguardio-customer-service/customer-service-api-messaging/src/main/java/io/eventuate/examples/realguardio/customerservice/api/messaging/CustomerCommandHandler.java`
- Handles: `CreateLocationWithSecuritySystemCommand`
- Configuration: `CustomerCommandHandlerConfiguration.java`

## API Comparison

### Event Consumers

#### Legacy API Pattern
```java
public class CustomerEventConsumer {
    private final RealGuardOsoFactManager osoFactManager;

    public CustomerEventConsumer(RealGuardOsoFactManager osoFactManager) {
        this.osoFactManager = osoFactManager;
    }

    public DomainEventHandlers domainEventHandlers() {
        return DomainEventHandlersBuilder
            .forAggregateType("Customer")
            .onEvent(CustomerEmployeeAssignedCustomerRole.class, this::handleCustomerEmployeeAssignedCustomerRole)
            .onEvent(LocationCreatedForCustomer.class, this::handleLocationCreatedForCustomer)
            .build();
    }

    private void handleCustomerEmployeeAssignedCustomerRole(DomainEventEnvelope<CustomerEmployeeAssignedCustomerRole> envelope) {
        // Handler logic
    }
}
```

**Configuration:**
```java
@Configuration
public class OsoEventSubscribersConfiguration {
    @Bean
    public DomainEventDispatcher domainEventDispatcher(
            DomainEventDispatcherFactory domainEventDispatcherFactory,
            CustomerEventConsumer eventConsumer) {
        return domainEventDispatcherFactory.make(
            "osoEventSubscribersDispatcher",
            eventConsumer.domainEventHandlers()
        );
    }
}
```

#### New API Pattern
```java
public class CustomerEventConsumer {
    private final RealGuardOsoFactManager osoFactManager;

    public CustomerEventConsumer(RealGuardOsoFactManager osoFactManager) {
        this.osoFactManager = osoFactManager;
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "Customer")
    public void handleCustomerEmployeeAssignedCustomerRole(DomainEventEnvelope<CustomerEmployeeAssignedCustomerRole> envelope) {
        // Handler logic
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "Customer")
    public void handleLocationCreatedForCustomer(DomainEventEnvelope<LocationCreatedForCustomer> envelope) {
        // Handler logic
    }
}
```

**Configuration:**
```java
@Configuration
public class OsoEventSubscribersConfiguration {
    @Bean
    public CustomerEventConsumer customerEventConsumer(RealGuardOsoFactManager osoFactManager) {
        return new CustomerEventConsumer(osoFactManager);
    }
    // No DomainEventDispatcher bean needed!
}
```

### Command Handlers

#### Legacy API Pattern
```java
public class CustomerCommandHandler {
    private final CustomerService customerService;

    public CustomerCommandHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CommandHandlers commandHandlers() {
        return SagaCommandHandlersBuilder
            .fromChannel("customer-service")
            .onMessage(CreateLocationWithSecuritySystemCommand.class, this::handleCreateLocationWithSecuritySystem)
            .build();
    }

    public Message handleCreateLocationWithSecuritySystem(CommandMessage<CreateLocationWithSecuritySystemCommand> cm) {
        // Handler logic
        return withSuccess(new LocationCreatedWithSecuritySystem(locationId));
    }
}
```

**Configuration:**
```java
@Configuration
public class CustomerCommandHandlerConfiguration {
    @Bean
    public CustomerCommandHandler customerCommandHandler(CustomerService customerService) {
        return new CustomerCommandHandler(customerService);
    }

    @Bean
    public CommandDispatcher customerCommandDispatcher(CustomerCommandHandler customerCommandHandler,
                                                        SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
        return sagaCommandDispatcherFactory.make("customerCommandDispatcher",
                customerCommandHandler.commandHandlers());
    }
}
```

#### New API Pattern
```java
public class CustomerCommandHandler {
    private final CustomerService customerService;

    public CustomerCommandHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @EventuateCommandHandler(subscriberId = "customerCommandDispatcher", channel = "customer-service")
    public LocationCreatedWithSecuritySystem handleCreateLocationWithSecuritySystem(
            CommandMessage<CreateLocationWithSecuritySystemCommand> cm) {
        // Handler logic
        // Return the reply object directly instead of wrapping with withSuccess()
        return new LocationCreatedWithSecuritySystem(locationId);
    }
}
```

**Configuration:**
```java
@Configuration
public class CustomerCommandHandlerConfiguration {
    @Bean
    public CustomerCommandHandler customerCommandHandler(CustomerService customerService) {
        return new CustomerCommandHandler(customerService);
    }
    // No CommandDispatcher bean needed!
}
```

## Key Differences

### Event Consumers

1. **Handler Methods**
   - Legacy: Private methods referenced in `DomainEventHandlersBuilder`
   - New: Public methods annotated with `@EventuateDomainEventHandler`

2. **Handler Registration**
   - Legacy: `domainEventHandlers()` method returns `DomainEventHandlers` built with `DomainEventHandlersBuilder`
   - New: No registration method needed - annotations handle registration

3. **Configuration**
   - Legacy: Must create `DomainEventDispatcher` bean using `DomainEventDispatcherFactory`
   - New: Only need to create consumer bean - framework handles dispatcher creation

4. **Annotation Parameters**
   - `subscriberId`: Identifies the subscriber (equivalent to the dispatcher name in legacy API)
   - `channel`: The aggregate type or channel name (equivalent to `forAggregateType()` in legacy API)

### Command Handlers

1. **Handler Methods**
   - Legacy: Public/private methods returning `Message`, using `withSuccess()`/`withFailure()` builders
   - New: Public methods annotated with `@EventuateCommandHandler`, returning reply objects directly

2. **Handler Registration**
   - Legacy: `commandHandlers()` method returns `CommandHandlers` built with `SagaCommandHandlersBuilder`
   - New: No registration method needed - annotations handle registration

3. **Configuration**
   - Legacy: Must create `CommandDispatcher` bean using `SagaCommandDispatcherFactory`
   - New: Only need to create handler bean - framework handles dispatcher creation

4. **Reply Handling**
   - Legacy: Wrap replies with `CommandHandlerReplyBuilder.withSuccess()` or `withFailure()`
   - New: Return reply objects directly; throw exceptions for failures

5. **Annotation Parameters**
   - `subscriberId`: Identifies the command dispatcher
   - `channel`: The command channel name (equivalent to `fromChannel()` in legacy API)

## Migration Steps

### For Event Consumers

1. **Update Handler Methods**
   - Change handler methods from private to public
   - Add `@EventuateDomainEventHandler` annotation to each handler method
   - Set `subscriberId` to the dispatcher name from the legacy configuration
   - Set `channel` to the aggregate type from `forAggregateType()`

2. **Remove Handler Registration**
   - Delete the `domainEventHandlers()` method
   - Remove imports: `DomainEventHandlers`, `DomainEventHandlersBuilder`

3. **Update Configuration**
   - Remove the `DomainEventDispatcher` bean creation
   - Remove imports: `DomainEventDispatcher`, `DomainEventDispatcherFactory`
   - Ensure consumer bean is still created (if not using `@Component`)

### For Command Handlers

1. **Update Handler Methods**
   - Add `@EventuateCommandHandler` annotation to each handler method
   - Set `subscriberId` to the dispatcher name from the legacy configuration
   - Set `channel` to the channel name from `fromChannel()`
   - Change return type from `Message` to the reply object type
   - Remove `CommandHandlerReplyBuilder.withSuccess()` wrapping - return reply directly
   - For failure cases, throw exceptions or return failure reply objects

2. **Remove Handler Registration**
   - Delete the `commandHandlers()` method
   - Remove imports: `CommandHandlers`, `SagaCommandHandlersBuilder`, `CommandHandlerReplyBuilder`

3. **Update Configuration**
   - Remove the `CommandDispatcher` bean creation
   - Remove imports: `CommandDispatcher`, `SagaCommandDispatcherFactory`
   - Ensure handler bean is still created

## Benefits of New API

1. **Simplified Configuration**: No need to manually create dispatcher beans
2. **Less Boilerplate**: Eliminates builder pattern and registration methods
3. **More Declarative**: Annotations clearly indicate message handling at the method level
4. **Easier to Understand**: Handler methods are self-contained with clear annotations
5. **Better Integration**: Works seamlessly with Spring's component scanning and dependency injection
6. **Reduced Configuration Errors**: Framework handles dispatcher creation automatically

## Testing Considerations

When migrating, ensure that:

1. **Integration Tests**: Update any integration tests that may depend on the dispatcher beans
2. **Unit Tests**: Handler method signatures may change (especially for command handlers returning reply objects directly)
3. **Channel Names**: Verify that channel names and subscriber IDs match between old and new implementations
4. **Exception Handling**: For command handlers, review error handling patterns (exceptions vs. failure replies)

## Migration Order

Recommended migration order to minimize risk:

1. Start with event consumers (simpler migration)
   - `CustomerEmployeeLocationEventConsumer` (1 event handler - simplest)
   - `CustomerEventConsumer` (4 event handlers)

2. Then migrate command handlers
   - `CustomerCommandHandler` (1 command handler)
   - `SecuritySystemCommandHandler` (2 command handlers)

3. Run full test suite after each migration
4. Commit after each successful migration

## Event Publishing API Enhancements

### Current Event Publishing Pattern

Currently, the application uses `DomainEventPublisher` directly with string-based channel names:

**CustomerService.java (lines 148-154, 176-182, 232-238, 295-301):**
```java
public class CustomerService {
    private final DomainEventPublisher domainEventPublisher;

    public MemberRole assignRoleInternal(Long customerId, Long customerEmployeeId, String roleName) {
        // ... business logic ...

        domainEventPublisher.publish(
            "Customer",  // Channel as string
            customerId.toString(),  // Aggregate ID as string
            Collections.singletonList(
                new CustomerEmployeeAssignedCustomerRole(customerEmployeeId, roleName)
            )
        );

        return organizationService.assignRole(customer.getOrganizationId(), customerEmployee.getMemberId(), roleName);
    }
}
```

### Type-Safe Event Publishing Pattern

The new API provides type-safe event publishers through `DomainEventPublisherForAggregate`:

**Interface Definition:**
```java
public interface CustomerEventPublisher extends DomainEventPublisherForAggregate<Customer, Long, CustomerEvent> {
    // Marker interface - all methods inherited from parent
}
```

**Implementation:**
```java
public class CustomerEventPublisherImpl
    extends AbstractDomainEventPublisherForAggregateImpl<Customer, Long, CustomerEvent>
    implements CustomerEventPublisher {

    public CustomerEventPublisherImpl(DomainEventPublisher domainEventPublisher) {
        super(Customer.class, Customer::getId, domainEventPublisher, CustomerEvent.class);
    }
}
```

**Configuration:**
```java
@Configuration
public class CustomerEventPublisherConfiguration {
    @Bean
    public CustomerEventPublisher customerEventPublisher(DomainEventPublisher domainEventPublisher) {
        return new CustomerEventPublisherImpl(domainEventPublisher);
    }
}
```

**Usage:**
```java
public class CustomerService {
    private final CustomerEventPublisher customerEventPublisher;

    public MemberRole assignRoleInternal(Long customerId, Long customerEmployeeId, String roleName) {
        Customer customer = customerRepository.findRequiredById(customerId);
        // ... business logic ...

        customerEventPublisher.publish(
            customer,  // Type-safe aggregate object
            Collections.singletonList(
                new CustomerEmployeeAssignedCustomerRole(customerEmployeeId, roleName)
            )
        );

        return organizationService.assignRole(customer.getOrganizationId(), customerEmployee.getMemberId(), roleName);
    }
}
```

### Benefits of Type-Safe Event Publishing

1. **Compile-Time Safety**: Ensures aggregate type, ID type, and event types are correct
2. **Less Error-Prone**: No manual string conversion for aggregate IDs
3. **Better Refactoring**: IDE can track usage and assist with refactoring
4. **Clear Intent**: Generic parameters document expected types
5. **Consistency**: Enforces uniform event publishing pattern across the codebase

### Event Publishing Migration Steps

1. **Define Event Base Class/Interface** (if not exists)
   ```java
   public interface CustomerEvent {
       // Marker interface for all customer events
   }
   ```

2. **Have Event Classes Implement Interface**
   ```java
   public record CustomerEmployeeAssignedCustomerRole(Long customerEmployeeId, String roleName)
       implements CustomerEvent {
   }
   ```

3. **Create Publisher Interface**
   ```java
   public interface CustomerEventPublisher
       extends DomainEventPublisherForAggregate<Customer, Long, CustomerEvent> {
   }
   ```

4. **Create Publisher Implementation**
   ```java
   public class CustomerEventPublisherImpl
       extends AbstractDomainEventPublisherForAggregateImpl<Customer, Long, CustomerEvent>
       implements CustomerEventPublisher {

       public CustomerEventPublisherImpl(DomainEventPublisher domainEventPublisher) {
           super(Customer.class, Customer::getId, domainEventPublisher, CustomerEvent.class);
       }
   }
   ```

5. **Configure Publisher Bean**
   ```java
   @Bean
   public CustomerEventPublisher customerEventPublisher(DomainEventPublisher domainEventPublisher) {
       return new CustomerEventPublisherImpl(domainEventPublisher);
   }
   ```

6. **Update Service to Use Type-Safe Publisher**
   - Replace `DomainEventPublisher` injection with `CustomerEventPublisher`
   - Update `publish()` calls to pass aggregate object instead of string channel/ID

## Saga API Enhancements

### Current Saga Implementation

The application already uses `@SagaParticipantProxy` annotation, but methods lack `@SagaParticipantOperation` annotations.

**CustomerServiceProxy.java (current):**
```java
@SagaParticipantProxy(channel = CustomerServiceProxy.CHANNEL)
public class CustomerServiceProxy {
    public static final String CHANNEL = "customer-service";

    public static final Class<CustomerNotFound> customerNotFoundReply = CustomerNotFound.class;
    public static final Class<LocationAlreadyHasSecuritySystem> locationAlreadyHasSecuritySystemReply =
        LocationAlreadyHasSecuritySystem.class;

    public CommandWithDestination createLocationWithSecuritySystem(Long customerId, String locationName, Long securitySystemId) {
        return CommandWithDestinationBuilder.send(
                new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
                .to(CHANNEL)
                .build();
    }
}
```

### Enhanced Saga Proxy Pattern

The new API adds `@SagaParticipantOperation` annotations to document operations:

**CustomerServiceProxy.java (enhanced):**
```java
@Component
@SagaParticipantProxy(channel = CustomerServiceProxy.CHANNEL)
public class CustomerServiceProxy {
    public static final String CHANNEL = "customerService";

    public static final Class<CustomerNotFound> customerNotFoundReply = CustomerNotFound.class;
    public static final Class<LocationAlreadyHasSecuritySystem> locationAlreadyHasSecuritySystemReply =
        LocationAlreadyHasSecuritySystem.class;

    @SagaParticipantOperation(
        commandClass = CreateLocationWithSecuritySystemCommand.class,
        replyClasses = LocationCreatedWithSecuritySystem.class
    )
    public CommandWithDestination createLocationWithSecuritySystem(Long customerId, String locationName, Long securitySystemId) {
        return CommandWithDestinationBuilder.send(
                new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
                .to(CHANNEL)
                .build();
    }
}
```

### Reply Class Annotations

Reply classes can be annotated to indicate success or failure outcomes using annotations from the Eventuate framework:

**Success Reply:**
```java
import io.eventuate.tram.commands.consumer.annotations.SuccessReply;

@SuccessReply
public record LocationCreatedWithSecuritySystem(Long locationId)
    implements CreateLocationResult {
}
```

**Failure Replies:**
```java
import io.eventuate.tram.commands.consumer.annotations.FailureReply;

@FailureReply
public record CustomerNotFound() implements CreateLocationResult {
}

@FailureReply
public record LocationAlreadyHasSecuritySystem() implements CreateLocationResult {
}
```

**Note**: The `@SuccessReply` and `@FailureReply` annotations are provided by the Eventuate framework in the package `io.eventuate.tram.commands.consumer.annotations.*`

**Reply Interface:**
```java
public interface CreateLocationResult {
    // Marker interface for all possible replies
}
```

### Benefits of Saga Annotations

1. **Better Documentation**: `@SagaParticipantOperation` clearly documents command and reply types
2. **AsyncAPI Generation**: Enables automatic generation of AsyncAPI documentation
3. **Clear Success/Failure Semantics**: `@SuccessReply` and `@FailureReply` make outcomes explicit
4. **Type Safety**: Reply classes can implement common interface for type safety
5. **Discovery**: Tools can discover all saga operations and their contracts

### Saga Migration Steps

1. **Add @SagaParticipantOperation to Proxy Methods**
   ```java
   @SagaParticipantOperation(
       commandClass = CreateSecuritySystemCommand.class,
       replyClasses = SecuritySystemCreated.class
   )
   public CommandWithDestination createSecuritySystem(String locationName) {
       // ...
   }
   ```

2. **Create Reply Interface (Optional but Recommended)**
   ```java
   public interface CreateLocationResult {
   }
   ```

3. **Annotate Success Reply Classes**
   ```java
   import io.eventuate.tram.commands.consumer.annotations.SuccessReply;

   @SuccessReply
   public record LocationCreatedWithSecuritySystem(Long locationId)
       implements CreateLocationResult {
   }
   ```

4. **Annotate Failure Reply Classes**
   ```java
   import io.eventuate.tram.commands.consumer.annotations.FailureReply;

   @FailureReply
   public record CustomerNotFound() implements CreateLocationResult {
   }
   ```

5. **Update Saga to Use Reply Interface** (Optional)
   ```java
   public void handleLocationCreated(CreateSecuritySystemSagaData data, CreateLocationResult reply) {
       if (reply instanceof LocationCreatedWithSecuritySystem success) {
           data.setLocationId(success.locationId());
       } else if (reply instanceof CustomerNotFound) {
           data.setRejectionReason("Customer not found");
       }
   }
   ```

## Components Requiring Migration

### Event Publishing

**CustomerService.java**
- Location: `realguardio-customer-service/customer-service-domain/src/main/java/io/eventuate/examples/realguardio/customerservice/customermanagement/domain/CustomerService.java`
- Current: Uses `DomainEventPublisher` directly with string channels
- Events Published:
  - `CustomerEmployeeAssignedCustomerRole` (line 148-154)
  - `LocationCreatedForCustomer` (line 176-182)
  - `SecuritySystemAssignedToLocation` (lines 193-199, 232-238)
  - `CustomerEmployeeAssignedLocationRole` (line 295-301)
- Needs: Create `CustomerEventPublisher` with type-safe interface

### Saga Proxies

**CustomerServiceProxy.java**
- Location: `realguardio-orchestration-service/orchestration-sagas/src/main/java/io/realguardio/orchestration/sagas/proxies/CustomerServiceProxy.java`
- Current: Has `@SagaParticipantProxy` but no method annotations
- Needs: Add `@SagaParticipantOperation` to `createLocationWithSecuritySystem()` method

**SecuritySystemServiceProxy.java**
- Location: `realguardio-orchestration-service/orchestration-sagas/src/main/java/io/realguardio/orchestration/sagas/proxies/SecuritySystemServiceProxy.java`
- Current: Has `@SagaParticipantProxy` but no method annotations
- Needs: Add `@SagaParticipantOperation` to all 3 methods:
  - `createSecuritySystem()`
  - `updateCreationFailed()`
  - `noteLocationCreated()`

### Reply Classes

The following reply classes should be annotated:

**Success Replies:**
- `SecuritySystemCreated`
- `LocationCreatedWithSecuritySystem`
- `LocationNoted`

**Failure Replies:**
- `CustomerNotFound`
- `LocationAlreadyHasSecuritySystem`

## References

- Example project: `/Users/cer/src/eventuate/eventuate-tram-spring-wolf-support`
- New API examples:
  - Event Consumers: `eventuate-tram-springwolf-support-events/src/test/java/io/eventuate/exampleapp/events/consumer/CustomerEventConsumer.java`
  - Event Publishers: `eventuate-tram-springwolf-support-events/src/test/java/io/eventuate/exampleapp/events/publisher/CustomerEventPublisher.java`
  - Saga Proxies: `eventuate-tram-springwolf-support-sagas/src/test/java/io/eventuate/tram/spring/springwolf/sagas/application/CustomerServiceProxy.java`
- Legacy API examples:
  - Event Consumers: `eventuate-tram-springwolf-support-events/src/test/java/io/eventuate/exampleapp/events/legacyconsumer/OrderEventConsumer.java`
