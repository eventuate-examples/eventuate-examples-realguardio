# CQRS Replica Location Roles - Discussion

## Initial Idea
Implement the CQRS pattern where:
- Security Service maintains a CQRS replica of CustomerEmployeeLocation roles
- Customer Service publishes domain events when roles are assigned
- Uses Eventuate Tram for event publishing and consuming

## Q&A Development

### Q1: Event Schema and Versioning
**Question:** What should be the complete structure of the `CustomerEmployeeLocationCreated` event, and how should we handle potential future schema changes?

**Answer:** A - Basic fields only (userName, locationId, roleName) with no versioning support

**Decision:** Keep the event structure simple with just the essential fields needed for the CQRS replica.

### Q2: Database Schema for the Replica
**Question:** How should the Security Service store the replicated customer employee location roles in its database?

**Answer:** A - Single denormalized table: `customer_employee_location_role` with columns (id, user_name, location_id, role_name, created_at)

**Decision:** Use a simple denormalized table optimized for read queries, perfect for a CQRS read model.

### Q3: Handling Duplicate Events and Idempotency
**Question:** How should the Security Service handle potential duplicate events (e.g., due to network retries or redelivery)?

**Answer:** A - No special handling - just insert records and let database constraints handle duplicates

**Decision:** Keep it simple - rely on natural behavior without adding idempotency mechanisms.

### Q4: Event Publishing Trigger Point
**Question:** When exactly should the Customer Service publish the `CustomerEmployeeLocationCreated` event?

**Answer:** A - Immediately when `assignLocationRole()` is called, using DomainEventPublisher

**Decision:** The `assignLocationRole()` method will directly call DomainEventPublisher to publish the event as part of its business logic.

### Q5: Event Consumer Implementation Pattern
**Question:** How should the Security Service's event consumer be structured?

**Answer:** B - Event handler delegates to a service layer which handles business logic and database updates

**Decision:** Use a layered approach where the event handler delegates to a service for better separation of concerns and testability.

### Q6: Error Handling Strategy
**Question:** What should happen if the Security Service fails to process a `CustomerEmployeeLocationCreated` event?

**Answer:** The event handler should just throw an exception - the Eventuate framework handles the rest

**Decision:** Rely on Eventuate Tram's built-in error handling and retry mechanisms by throwing exceptions on failure.

### Q7: Package Structure and Naming Conventions
**Question:** How should the new CQRS-related classes be organized in both services?

**Answer:** Customer service - event class is part of the domain, Security service - location-roles-replica subproject

**Decision:** Place the event class in the customer service domain package, and create a new location-roles-replica subproject in the security service.

### Q8: Testing Strategy
**Question:** What types of tests should be implemented to verify the CQRS replica functionality?

**Answer:** Unit test both - CustomerServiceInProcessComponentTest should verify that the event is published to the outbox, SecuritySystemServiceComponentTest should publish an event and verify that DB is updated, RealGuardioEndToEndTest should verify that replica is updated

**Decision:** Implement comprehensive testing:
- Component test in Customer Service to verify event publication to outbox
- Component test in Security Service to verify event consumption and database update
- End-to-end test to verify the complete CQRS flow and replica state

### Q9: Query API for the Replica
**Question:** What REST API endpoints should the Security Service expose to query the replicated location roles?

**Answer:** B - Simple read-only endpoint: GET /location-roles?userName={userName}&locationId={locationId}

**Decision:** Provide a simple query endpoint that allows filtering by user and/or location, suitable for authorization checks.

### Q10: Implementation Order and Dependencies
**Question:** What is the recommended sequence for implementing this CQRS feature?

**Answer:** D - Iterative approach, but first test should be CustomerServiceTest.shouldCreateCustomerEmployeeAndAssignLocationRoles() should mock DomainEventPublisher and verify that event was published

**Decision:** Follow an iterative implementation approach:
1. Start with unit test in CustomerServiceTest - mock DomainEventPublisher and verify event publication
2. Create event class in Customer Service domain
3. Implement event publishing in CustomerService.assignLocationRole()
4. Create Security Service location-roles-replica subproject
5. Implement event consumer and database updates
6. Add query API endpoint
7. Add component and end-to-end tests

## Final Specification Summary

### Feature Overview
Implement CQRS pattern where Security Service maintains a read-only replica of customer employee location roles by consuming events from Customer Service.

### Technical Details
- **Event**: `CustomerEmployeeLocationCreated` with fields (userName, locationId, roleName)
- **Database**: Single denormalized table `customer_employee_location_role` in Security Service
- **Publishing**: CustomerService.assignLocationRole() publishes via DomainEventPublisher
- **Consuming**: Event handler in Security Service delegates to service layer for DB updates
- **Query API**: GET /location-roles?userName={userName}&locationId={locationId}
- **Error Handling**: Throw exceptions, let Eventuate Tram handle retries
- **Testing**: Unit tests, component tests, and end-to-end test for complete flow

### Implementation Checklist
- [ ] Add unit test to CustomerServiceTest with mocked DomainEventPublisher
- [ ] Create CustomerEmployeeLocationCreated event class in customer domain
- [ ] Update CustomerService.assignLocationRole() to publish event
- [ ] Create location-roles-replica subproject in Security Service
- [ ] Implement event consumer and service layer
- [ ] Add database migration for customer_employee_location_role table
- [ ] Implement REST query endpoint
- [ ] Add component test to verify event publication to outbox
- [ ] Add component test to verify event consumption and DB update
- [ ] Add end-to-end test to verify complete CQRS flow
