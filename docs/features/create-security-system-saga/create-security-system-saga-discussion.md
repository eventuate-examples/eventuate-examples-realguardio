# Security System Creation Saga - Specification Discussion

## Initial Idea

Creating a security system service endpoint that:
- Accepts POST requests to `/securitysystems`
- Creates security systems linked to customers and locations
- Uses Eventuate Tram Saga for orchestration
- Ensures consistency across SecuritySystemService and CustomerService

## Questions and Answers

### Q1: Service Architecture and Dependencies

**Q: What should happen if the CustomerService is temporarily unavailable during saga execution?**

**A:** Since Eventuate Tram Saga uses Kafka, messages will be delivered eventually. The framework handles retries and eventual consistency automatically through persistent messaging.

### Q2: Location Validation and Creation

**Q: When the CustomerService validates the location, what should be the behavior for location creation/updates?**

**A:** Create if not exists, update if exists - If the location doesn't exist, create it with the securitySystemId. If it exists without a securitySystemId, update it to add the securitySystemId.

### Q3: Error Handling and Compensation

**Q: What specific validation should the CustomerService perform when it receives the request to validate and update the location?**

**A:** Basic validation only - Verify the customerId exists, check if the location already has a different securitySystemId assigned.

### Q4: Saga Compensation Strategy

**Q: If the saga fails at step 3 (updating SecuritySystem with locationId), what compensation should occur?**

**A:** Mark as failed - Keep everything as-is but change SecuritySystem state to FAILED for later retry.

### Q5: API Response Behavior

**Q: What should the POST /securitysystems endpoint return immediately after initiating the saga?**

**A:** Following the pattern in the example application - Return the securitySystemId immediately after the first local step creates the SecuritySystem in PENDING state. The controller will return this ID synchronously while the saga continues asynchronously.

### Q6: SecuritySystem Data Model

**Q: The existing SecuritySystem entity has id, locationName, state, actions, and version. What additional fields should be added to support the saga?**

**A:** Add customerId (Long) and locationId (Long) fields to link the SecuritySystem to the Customer Service's customer and location entities. These are essential for the saga coordination.

### Q7: SecuritySystem Actions

**Q: How should the SecuritySystemAction set be initialized when creating a new SecuritySystem in the saga?**

**A:** Ignore actions for now - they're not part of the core saga flow.

### Q8: State Transitions

**Q: Should we add a new SecuritySystemState value specifically for the saga failure scenario you chose earlier (option C - "Mark as failed")?**

**A:** Add CREATION_PENDING and CREATION_FAILED states to clearly distinguish the saga creation process from operational states.

### Q9: Command and Reply Messages

**Q: For the CustomerService command to validate and update the location, what should be included in the command message?**

**A:** Minimal data - customerId, locationName, securitySystemId.

### Q10: Error Reply Messages

**Q: What specific error reply types should the CustomerService return that the saga needs to handle?**

**A:** Following the example application pattern - Create a common interface (e.g., `ValidateLocationResult`) with specific reply classes: `LocationValidated` (success), `CustomerNotFound` (failure), and `LocationAlreadyHasSecuritySystem` (failure). Each failure reply is annotated with `@FailureReply`.

### Q11: Saga Data Class

**Q: What fields should the CreateSecuritySystemSagaData class contain to orchestrate the saga?**

**A:** Essential fields only - securitySystemId, customerId, locationName, locationId (set after validation), rejectionReason

**Key Decision:** The saga will be implemented in a separate `realguardio-orchestration-service` rather than within the security-system-service itself.

### Q12: Service Communication

**Q: How should the orchestration-service initiate the saga when it receives the POST /securitysystems request?**

**A:** Command to SecuritySystemService - Send a CreateSecuritySystemCommand to SecuritySystemService as the first saga step, since services must not access each other's databases.

### Q13: Success Reply from CustomerService

**Q: When CustomerService successfully validates and updates the location, what data should the LocationValidated reply contain?**

**A:** Just the locationId - Return only the locationId that was created or updated.

### Q14: Final Step Implementation

**Q: In step 3, when updating the SecuritySystem with locationId and changing state to DISARMED, should this be a compensatable action?**

**A:** No compensation needed - Only steps that are followed by a step that can fail need compensation. This is the final step.

### Q15: Compensation for Step 1

**Q: What should the compensation action for Step 1 (CreateSecuritySystem) do if Step 2 (CustomerService validation) fails?**

**A:** Mark as CREATION_FAILED - Keep the SecuritySystem but change its state to CREATION_FAILED.

## Summary of Decisions

Based on our discussion, here's the complete specification for the Security System Creation Saga:

### Architecture
- Implement saga in a separate `realguardio-orchestration-service`
- Services communicate via commands/replies (no direct database access)
- Use Eventuate Tram Saga framework with Kafka for message delivery

### Saga Steps
1. **Create SecuritySystem (compensatable)**
   - Command: `CreateSecuritySystemCommand(customerId, locationName)`
   - Success: Returns `securitySystemId`, state set to `CREATION_PENDING`
   - Compensation: `UpdateSecuritySystemStateCommand` to set state to `CREATION_FAILED`

2. **Validate and Update Location (participates in saga)**
   - Command: `ValidateAndUpdateLocationCommand(customerId, locationName, securitySystemId)`
   - Success Reply: `LocationValidated(locationId)`
   - Failure Replies: `CustomerNotFound`, `LocationAlreadyHasSecuritySystem`

3. **Finalize SecuritySystem (non-compensatable)**
   - Command: `FinalizeSecuritySystemCommand(securitySystemId, locationId)`
   - Updates SecuritySystem with locationId and changes state to `DISARMED`

### Data Model Updates
- Add to SecuritySystem entity: `customerId`, `locationId`
- Add SecuritySystemState values: `CREATION_PENDING`, `CREATION_FAILED`

### API Behavior
- POST `/securitysystems` returns `securitySystemId` immediately after Step 1
- Failed sagas leave SecuritySystem in `CREATION_FAILED` state for potential retry

### Error Handling
- Kafka ensures eventual message delivery (no timeout handling needed)
- Basic validation: customer exists, location not already assigned
- Failed systems kept for audit/retry (not deleted)
