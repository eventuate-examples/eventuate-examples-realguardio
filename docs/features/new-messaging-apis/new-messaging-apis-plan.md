# Migration Plan: New Eventuate Messaging APIs

## Overview

This plan outlines the incremental migration to the new Eventuate messaging APIs. Each step is designed to be as small as possible while leaving the system in a working, testable state.

## Migration Strategy

- **One component at a time**: Migrate one consumer/handler/publisher at a time
- **Verify after each step**: Run `./gradlew check` after each change
- **Commit frequently**: Commit after each successful step
- **Start simple**: Begin with simplest components (fewest handlers)

## Phase 1: Event Consumers (Message Receiving)

### [x] Step 1: Migrate CustomerEmployeeLocationEventConsumer (Simplest - 1 Handler)

**Location**: `realguardio-security-system-service/location-roles-replica`

**Changes**:
1. Update `CustomerEmployeeLocationEventConsumer.java`:
   - Change `handleCustomerEmployeeAssignedLocationRole()` from private to public
   - Add `@EventuateDomainEventHandler(subscriberId = "locationRolesReplicaDispatcher", channel = "Customer")` annotation
   - Remove `domainEventHandlers()` method
   - Remove imports: `DomainEventHandlers`, `DomainEventHandlersBuilder`

2. Update `LocationRolesReplicaMessagingConfiguration.java`:
   - Remove `domainEventDispatcher()` bean method
   - Remove imports: `DomainEventDispatcher`, `DomainEventDispatcherFactory`
   - Keep `customerEmployeeLocationEventConsumer()` bean

**Verification**:
```bash
./gradlew :location-roles-replica:check
./gradlew check
```

**Commit**: "Migrate CustomerEmployeeLocationEventConsumer to annotation-based API"

### [ ] Step 2: Migrate CustomerEventConsumer (4 Handlers)

**Location**: `realguardio-oso-integration-service/oso-event-subscribers`

**Changes**:
1. Update `CustomerEventConsumer.java`:
   - Change all handler methods from private to public
   - Add `@EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "Customer")` to each handler:
     - `handleCustomerEmployeeAssignedCustomerRole()`
     - `handleLocationCreatedForCustomer()`
     - `handleSecuritySystemAssignedToLocation()`
     - `handleCustomerEmployeeAssignedLocationRole()`
   - Remove `domainEventHandlers()` method
   - Remove imports: `DomainEventHandlers`, `DomainEventHandlersBuilder`

2. Update `OsoEventSubscribersConfiguration.java`:
   - Remove `domainEventDispatcher()` bean method
   - Remove imports: `DomainEventDispatcher`, `DomainEventDispatcherFactory`

**Verification**:
```bash
./gradlew :oso-event-subscribers:check
./gradlew check
```

**Commit**: "Migrate CustomerEventConsumer to annotation-based API"

## Phase 2: Command Handlers (Command Processing)

### [ ] Step 3: Migrate CustomerCommandHandler (Simplest - 1 Handler)

**Location**: `realguardio-customer-service/customer-service-api-messaging`

**Changes**:
1. Update `CustomerCommandHandler.java`:
   - Add `@EventuateCommandHandler(subscriberId = "customerCommandDispatcher", channel = "customer-service")` to `handleCreateLocationWithSecuritySystem()`
   - Change return type from `Message` to `LocationCreatedWithSecuritySystem`
   - Update method body:
     - For success: Return `new LocationCreatedWithSecuritySystem(locationId)` directly
     - For failure (`CustomerNotFoundException`): Return `new CustomerNotFound()` instead of `withFailure()`
   - Remove `commandHandlers()` method
   - Remove imports: `CommandHandlers`, `SagaCommandHandlersBuilder`, `CommandHandlerReplyBuilder`, `Message`

2. Update `CustomerCommandHandlerConfiguration.java`:
   - Remove `customerCommandDispatcher()` bean method
   - Remove imports: `CommandDispatcher`, `SagaCommandDispatcherFactory`
   - Keep `customerCommandHandler()` bean

**Verification**:
```bash
./gradlew :customer-service-api-messaging:check
./gradlew check
```

**Commit**: "Migrate CustomerCommandHandler to annotation-based API"

### [ ] Step 4: Migrate SecuritySystemCommandHandler (2 Handlers)

**Location**: `realguardio-security-system-service/security-system-api-messaging`

**Changes**:
1. Update `SecuritySystemCommandHandler.java`:
   - Add `@EventuateCommandHandler(subscriberId = "securitySystemCommandDispatcher", channel = "security-system-service")` to both handlers:
     - `handleCreateSecuritySystem()`
     - `handleNoteLocationCreated()`
   - Change return types from `Message` to reply objects:
     - `handleCreateSecuritySystem()`: Return `SecuritySystemCreated`
     - `handleNoteLocationCreated()`: Return `LocationNoted`
   - Update method bodies to return reply objects directly instead of `withSuccess()`
   - Remove `commandHandlers()` method
   - Remove imports: `CommandHandlers`, `SagaCommandHandlersBuilder`, `Message`
   - Keep `CommandHandlerReplyBuilder` import (still used for `withSuccess()` until we update)

2. Update `SecuritySystemCommandHandlerConfiguration.java`:
   - Remove `securitySystemCommandDispatcher()` bean method
   - Remove imports: `CommandDispatcher`, `SagaCommandDispatcherFactory`
   - Keep `securitySystemCommandHandler()` bean

**Verification**:
```bash
./gradlew :security-system-api-messaging:check
./gradlew check
```

**Commit**: "Migrate SecuritySystemCommandHandler to annotation-based API"

## Phase 3: Event Publishing (Type-Safe Publishing)

### [ ] Step 5: Create CustomerEvent Interface and Update Event Classes

**Location**: `realguardio-customer-service/customer-service-domain`

**Changes**:
1. Create new file `CustomerEvent.java`:
   ```java
   package io.eventuate.examples.realguardio.customerservice.domain;

   public interface CustomerEvent {
       // Marker interface for all customer events
   }
   ```

2. Update event classes to implement `CustomerEvent`:
   - `CustomerEmployeeAssignedCustomerRole.java`
   - `LocationCreatedForCustomer.java`
   - `SecuritySystemAssignedToLocation.java`
   - `CustomerEmployeeAssignedLocationRole.java`

**Verification**:
```bash
./gradlew :customer-service-domain:check
./gradlew check
```

**Commit**: "Add CustomerEvent interface and update event classes"

### [ ] Step 6: Create CustomerEventPublisher Interface and Implementation

**Location**: `realguardio-customer-service/customer-service-domain`

**Changes**:
1. Create new file `CustomerEventPublisher.java`:
   ```java
   package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

   import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;
   import io.eventuate.tram.events.publisher.DomainEventPublisherForAggregate;

   public interface CustomerEventPublisher
       extends DomainEventPublisherForAggregate<Customer, Long, CustomerEvent> {
   }
   ```

2. Create new file `CustomerEventPublisherImpl.java`:
   ```java
   package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

   import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;
   import io.eventuate.tram.events.publisher.AbstractDomainEventPublisherForAggregateImpl;
   import io.eventuate.tram.events.publisher.DomainEventPublisher;

   public class CustomerEventPublisherImpl
       extends AbstractDomainEventPublisherForAggregateImpl<Customer, Long, CustomerEvent>
       implements CustomerEventPublisher {

       public CustomerEventPublisherImpl(DomainEventPublisher domainEventPublisher) {
           super(Customer.class, Customer::getId, domainEventPublisher, CustomerEvent.class);
       }
   }
   ```

3. Update `CustomerManagementConfiguration.java`:
   - Add `customerEventPublisher()` bean:
     ```java
     @Bean
     public CustomerEventPublisher customerEventPublisher(DomainEventPublisher domainEventPublisher) {
         return new CustomerEventPublisherImpl(domainEventPublisher);
     }
     ```

**Verification**:
```bash
./gradlew :customer-service-domain:check
./gradlew check
```

**Commit**: "Add CustomerEventPublisher with type-safe interface"

### [ ] Step 7: Update CustomerService to Use CustomerEventPublisher

**Location**: `realguardio-customer-service/customer-service-domain`

**Changes**:
1. Update `CustomerService.java`:
   - Change constructor parameter from `DomainEventPublisher` to `CustomerEventPublisher`
   - Update field: `private final CustomerEventPublisher customerEventPublisher;`
   - Update all `publish()` calls (4 locations at lines ~148, 176, 193/232, 295):
     - Before: `domainEventPublisher.publish("Customer", customerId.toString(), Collections.singletonList(event))`
     - After: `customerEventPublisher.publish(customer, Collections.singletonList(event))`
   - Note: For lines 193 and 232 where we already have `location`, we need to fetch the customer first
   - Import: Add `CustomerEventPublisher` import

2. Update `CustomerManagementConfiguration.java`:
   - Change `customerService()` bean parameter from `DomainEventPublisher` to `CustomerEventPublisher`

**Verification**:
```bash
./gradlew :customer-service-domain:check
./gradlew check
```

**Commit**: "Migrate CustomerService to use type-safe CustomerEventPublisher"

## Phase 4: Saga Enhancements (Optional but Valuable)

### [ ] Step 8: Create Reply Interfaces for Commands

**Location**: Various API modules

**Changes**:
1. Create `CreateSecuritySystemResult.java` in `security-system-api-messaging`:
   ```java
   package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies;

   public interface CreateSecuritySystemResult {
   }
   ```

2. Create `CreateLocationResult.java` in `customer-service-api-messaging`:
   ```java
   package io.eventuate.examples.realguardio.customerservice.api.messaging.replies;

   public interface CreateLocationResult {
   }
   ```

3. Create `NoteLocationResult.java` in `security-system-api-messaging`:
   ```java
   package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies;

   public interface NoteLocationResult {
   }
   ```

**Verification**:
```bash
./gradlew check
```

**Commit**: "Add reply interfaces for command results"

### [ ] Step 9: Annotate Success Reply Classes

**Location**: Various API modules

**Changes**:
1. Update `SecuritySystemCreated.java`:
   - Add `@SuccessReply` annotation
   - Add `implements CreateSecuritySystemResult`
   - Add import:
     ```java
     import io.eventuate.tram.commands.consumer.annotations.SuccessReply;
     ```

2. Update `LocationCreatedWithSecuritySystem.java`:
   - Add `@SuccessReply` annotation
   - Add `implements CreateLocationResult`
   - Add import:
     ```java
     import io.eventuate.tram.commands.consumer.annotations.SuccessReply;
     ```

3. Update `LocationNoted.java`:
   - Add `@SuccessReply` annotation
   - Add `implements NoteLocationResult`
   - Add import:
     ```java
     import io.eventuate.tram.commands.consumer.annotations.SuccessReply;
     ```

**Verification**:
```bash
./gradlew check
```

**Commit**: "Annotate success reply classes with @SuccessReply"

### [ ] Step 10: Annotate Failure Reply Classes

**Location**: Various API modules

**Changes**:
1. Update `CustomerNotFound.java`:
   - Add `@FailureReply` annotation
   - Add `implements CreateLocationResult`
   - Add import:
     ```java
     import io.eventuate.tram.commands.consumer.annotations.FailureReply;
     ```

2. Update `LocationAlreadyHasSecuritySystem.java`:
   - Add `@FailureReply` annotation
   - Add `implements CreateLocationResult`
   - Add import:
     ```java
     import io.eventuate.tram.commands.consumer.annotations.FailureReply;
     ```

**Verification**:
```bash
./gradlew check
```

**Commit**: "Annotate failure reply classes with @FailureReply"

### [ ] Step 11: Add @SagaParticipantOperation to CustomerServiceProxy

**Location**: `realguardio-orchestration-service/orchestration-sagas`

**Changes**:
1. Update `CustomerServiceProxy.java`:
   - Add `@SagaParticipantOperation` annotation to `createLocationWithSecuritySystem()`:
     ```java
     @SagaParticipantOperation(
         commandClass = CreateLocationWithSecuritySystemCommand.class,
         replyClasses = LocationCreatedWithSecuritySystem.class
     )
     ```
   - Add import:
     ```java
     import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantOperation;
     ```

**Verification**:
```bash
./gradlew :orchestration-sagas:check
./gradlew check
```

**Commit**: "Add @SagaParticipantOperation to CustomerServiceProxy"

### [ ] Step 12: Add @SagaParticipantOperation to SecuritySystemServiceProxy

**Location**: `realguardio-orchestration-service/orchestration-sagas`

**Changes**:
1. Update `SecuritySystemServiceProxy.java`:
   - Add `@SagaParticipantOperation` to `createSecuritySystem()`:
     ```java
     @SagaParticipantOperation(
         commandClass = CreateSecuritySystemCommand.class,
         replyClasses = SecuritySystemCreated.class
     )
     ```
   - Add `@SagaParticipantOperation` to `updateCreationFailed()`:
     ```java
     @SagaParticipantOperation(
         commandClass = UpdateCreationFailedCommand.class,
         replyClasses = Void.class  // Or appropriate reply class
     )
     ```
   - Add `@SagaParticipantOperation` to `noteLocationCreated()`:
     ```java
     @SagaParticipantOperation(
         commandClass = NoteLocationCreatedCommand.class,
         replyClasses = LocationNoted.class
     )
     ```
   - Add import:
     ```java
     import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantOperation;
     ```

**Verification**:
```bash
./gradlew :orchestration-sagas:check
./gradlew check
```

**Commit**: "Add @SagaParticipantOperation to SecuritySystemServiceProxy"

## Summary

**Total Steps**: 12
**Commits**: 12
**Phases**: 4

**Estimated Time**:
- Phase 1 (Event Consumers): 30-45 minutes
- Phase 2 (Command Handlers): 30-45 minutes
- Phase 3 (Event Publishing): 45-60 minutes
- Phase 4 (Saga Enhancements): 30-45 minutes
- **Total**: 2.5-3.5 hours

## Dependencies Between Steps

- Steps 1-2 are independent (can be done in any order)
- Steps 3-4 are independent (can be done in any order)
- Step 5 must precede Step 6
- Step 6 must precede Step 7
- Step 8 must precede Steps 9-10
- Steps 9-10 are independent (but both depend on Step 8)
- Steps 11-12 are independent (but may benefit from Steps 9-10 for documentation)

## Rollback Strategy

Each step is a separate commit. If any step fails:
1. Review the test failures
2. Fix the issue, or
3. Revert the commit: `git revert HEAD`
4. Analyze the problem before retrying

## Testing Strategy

After each step:
1. Run module-specific tests: `./gradlew :module-name:check`
2. Run full test suite: `./gradlew check`
3. If tests pass, commit
4. If tests fail, investigate and fix before committing

## Notes

- Keep the spec document (`new-messaging-apis-spec.md`) open for reference
- Each step should take 10-20 minutes to implement
- Don't skip the verification step - it's critical for catching issues early
- If a step seems too large, break it down further
