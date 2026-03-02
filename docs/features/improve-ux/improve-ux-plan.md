# Improve UX - Steel Thread Implementation Plan

## Instructions for Coding Agent

**IMPORTANT**: Follow these guidelines when implementing this plan:

1. **Use simple commands** that you have permission to execute. Avoid complex commands that may fail due to permission issues.

2. **Track task completion** using the `idea-to-code:plan-tracking` skill.

3. **Write code using TDD**:
   - Use the `idea-to-code:tdd` skill when implementing code
   - NEVER write production code (`src/main/java/**/*.java`) without first writing a failing test
   - Before using the Write tool on any `.java` file in `src/main/`, ask: "Do I have a failing test for this?" If not, write the test first.
   - When building something that requires scripting, never run scripts or ad-hoc docker/curl commands that modify state directly. Always update the test script first, then run the test script.
   - When task direction changes mid-implementation, return to TDD PLANNING state and write a test first

4. **Commit after each completed task** when tests pass and the task is marked complete.

5. **Incremental development**: Each task should be independently testable and deliverable.

---

## Overview

**Idea Type**: A. User-facing feature

**Primary Scenario**: Admin Onboards New Employee

This plan implements the Improve UX feature through a series of steel threads, each delivering an incremental slice of end-to-end functionality.

---

## Steel Thread 1: UI Foundation and Security System Actions

**Goal**: Set up shadcn/ui, migrate existing components, and implement functional arm/disarm/acknowledge buttons.

This thread delivers immediate value by making the existing security system buttons functional while establishing the UI foundation.

### Task 1.1: Install and configure shadcn/ui

- [ ] Initialize shadcn/ui in realguardio-bff (`npx shadcn@latest init`)
- [ ] Configure Tailwind CSS properly for shadcn/ui
- [ ] Install required shadcn/ui components: button, table, toast, tabs
- [ ] Verify build succeeds with new dependencies

### Task 1.2: Add toast notification infrastructure

- [ ] Install sonner (toast library used by shadcn/ui)
- [ ] Add Toaster component to app layout
- [ ] Create useToast hook wrapper for consistent usage
- [ ] Write unit test for toast display

### Task 1.3: Implement Security System action API endpoints

**Backend (Security System Service)** - The existing `PUT /securitysystems/{id}` endpoint already supports ARM and DISARM actions via request body `{ "action": "ARM" | "DISARM" }`. Authorization is already implemented via `SecuritySystemActionAuthorizer.verifyCanDo()`. Enhancements needed:
- [ ] Add `acknowledge()` method to `SecuritySystemServiceImpl` (following pattern of `arm()`/`disarm()`)
- [ ] Add ACKNOWLEDGE action handling to `SecuritySystemController.updateSecuritySystem()`
- [ ] Use existing `securitySystemActionAuthorizer.verifyCanDo(id, RolesAndPermissions.DISARM)` for ACKNOWLEDGE (same permission as DISARM)

**BFF API route**:
- [ ] Add `PUT /api/securitysystems/[id]` route that proxies to Security System Service
- [ ] Accept `{ action: "ARM" | "DISARM" | "ACKNOWLEDGE" }` in request body
- [ ] Forward JWT token for authorization

### Task 1.4: Wire up Security System action buttons in UI

- [ ] Update SecuritySystemTable to call `PUT /api/securitysystems/[id]` with action in body
- [ ] Pass action type: ARM, DISARM, or ACKNOWLEDGE based on button clicked
- [ ] Show loading state during API call
- [ ] Display success toast on successful action
- [ ] Display error toast on failure
- [ ] Refresh table data after action completes

### Task 1.5: Expand mock server for security system actions

- [ ] Add `PUT /securitysystems/:id` mock endpoint
- [ ] Handle actions in request body: ARM, DISARM, ACKNOWLEDGE
- [ ] Update in-memory state based on action
- [ ] Return updated security system with new state

### Task 1.6: Migrate Header component to Tailwind

- [ ] Replace styled-jsx in Header.tsx with Tailwind classes
- [ ] Use shadcn/ui Button component for auth buttons
- [ ] Verify visual appearance matches original
- [ ] Ensure all existing tests pass

---

## Steel Thread 2: Tab Navigation with Role-Based Visibility

**Goal**: Add horizontal tab navigation that shows/hides tabs based on user roles.

Implements: Scenario 6 (Non-Admin User Experience) - partial

### Task 2.1: Add GET /me/roles endpoint to Customer Service

- [ ] Create `UserRolesController` with `GET /me/roles` endpoint
- [ ] Return `{ customerRoles: string[], hasSecuritySystemAccess: boolean }`
- [ ] Query user's customer-level roles (e.g., `COMPANY_ROLE_ADMIN`)
- [ ] Query whether user has any `SECURITY_SYSTEM_*` role at any location
- [ ] Require `REALGUARDIO_CUSTOMER_EMPLOYEE` role for access

### Task 2.2: Fetch customer roles during OAuth callback

- [ ] Modify `jwtCallback` in authOptions.ts to call `GET /me/roles` on initial login
- [ ] Store `customerRoles` and `hasSecuritySystemAccess` in JWT token
- [ ] Modify `sessionCallback` to copy customer roles to session
- [ ] Refresh customer roles during token refresh (every ~2 min)
- [ ] Add `CUSTOMER_SERVICE_URL` environment variable to BFF

### Task 2.3: Create TabNavigation component

- [ ] Create TabNavigation component using shadcn/ui Tabs
- [ ] Define tabs: Employees, Teams, Locations, Security Systems
- [ ] Add TabNavigation below Header in layout
- [ ] Default to Security Systems tab (if accessible) or first visible tab

### Task 2.4: Implement role-based tab filtering

- [ ] Access customer roles from session via `useSession()`
- [ ] Filter tabs based on roles:
  - Employees, Teams, Locations: require `COMPANY_ROLE_ADMIN` in `session.customerRoles`
  - Security Systems: require `session.hasSecuritySystemAccess === true`
- [ ] Create `useCustomerRoles` hook to centralize role checking
- [ ] Write unit tests for role filtering logic

### Task 2.3: Create placeholder content for admin tabs

- [ ] Create EmployeesTab placeholder component
- [ ] Create TeamsTab placeholder component
- [ ] Create LocationsTab placeholder component
- [ ] Move existing SecuritySystemTable into SecuritySystemsTab
- [ ] Wire tabs to display corresponding content

### Task 2.4: Migrate SecuritySystemTable to shadcn/ui

- [ ] Replace styled-jsx with Tailwind classes
- [ ] Use shadcn/ui Table component
- [ ] Use shadcn/ui Button for action buttons
- [ ] Add visual state indicators (colored badges for ARMED/DISARMED/ALARMED/FAULT)

---

## Steel Thread 3: Employee List

**Goal**: Admin can view a list of all employees.

Implements: Scenario 1 (Admin Onboards New Employee) - Step 1

### Task 3.1: Add GET employees endpoint to Customer Service

- [ ] Add `GET /customers/{customerId}/employees` endpoint to CustomerController
- [ ] Return list of CustomerEmployee with id, name, email
- [ ] Endpoint requires `COMPANY_ROLE_ADMIN` role
- [ ] Filter employees by user's customer (from JWT)

### Task 3.2: Add employees BFF API route

- [ ] Create `/api/employees` route in BFF
- [ ] Forward request to Customer Service with JWT
- [ ] Define TypeScript types for Employee response
- [ ] Handle error responses

### Task 3.3: Implement EmployeesTab with data table

- [ ] Fetch employees from `/api/employees` on tab load
- [ ] Display in shadcn/ui DataTable with columns: Name, Email
- [ ] Show loading state while fetching
- [ ] Show error state if fetch fails
- [ ] Add "Add Employee" button (non-functional placeholder)

### Task 3.4: Add employees endpoint to mock server

- [ ] Add `GET /customers/:customerId/employees` mock endpoint
- [ ] Return sample employee data (admin@acme.com, john.doe@acme.com)

---

## Steel Thread 4: Create Employee

**Goal**: Admin can create a new employee.

Implements: Scenario 1 (Admin Onboards New Employee) - Steps 2-5

### Task 4.1: Add create employee BFF route

- [ ] Create `POST /api/employees` route
- [ ] Forward to existing Customer Service endpoint
- [ ] Return created employee data

### Task 4.2: Create AddEmployeeDialog component

- [ ] Create modal dialog using shadcn/ui Dialog
- [ ] Form fields: First Name, Last Name, Email
- [ ] Form validation (required fields, email format)
- [ ] Submit button with loading state
- [ ] Cancel button to close dialog

### Task 4.3: Wire up employee creation flow

- [ ] Open dialog when "Add Employee" button clicked
- [ ] Submit form calls POST /api/employees
- [ ] On success: close dialog, show success toast, refresh employee list
- [ ] On error: show error toast, keep dialog open

### Task 4.4: Add create employee to mock server

- [ ] Add `POST /customers/:customerId/employees` mock endpoint
- [ ] Add new employee to in-memory list
- [ ] Return created employee with generated ID

---

## Steel Thread 5: Employee Details and Location Role Assignment

**Goal**: Admin can view employee details and assign location roles.

Implements: Scenario 1 (Admin Onboards New Employee) - Steps 6-8

### Task 5.1: Add GET employee details endpoint

- [ ] Add `GET /customers/{customerId}/employees/{employeeId}` endpoint
- [ ] Return employee details including assigned location roles
- [ ] Include list of assignable locations for role assignment UI

### Task 5.2: Add employee details BFF route

- [ ] Create `GET /api/employees/[id]` route
- [ ] Return employee with location role assignments

### Task 5.3: Create EmployeeDetailView component

- [ ] Display employee info (name, email)
- [ ] Display current location role assignments in a table
- [ ] "Back to list" navigation
- [ ] "Assign Role" button

### Task 5.4: Add GET locations endpoint for role assignment

- [ ] Add `GET /customers/{customerId}/locations` endpoint to CustomerController
- [ ] Return list of locations (id, name)
- [ ] Add BFF route `/api/locations`

### Task 5.5: Create AssignLocationRoleDialog component

- [ ] Modal with dropdowns for Location and Role selection
- [ ] Roles: SECURITY_SYSTEM_ARMER, SECURITY_SYSTEM_DISARMER, SECURITY_SYSTEM_VIEWER
- [ ] Submit calls PUT /customers/{id}/location-roles (existing endpoint)
- [ ] On success: close dialog, show toast, refresh employee details

### Task 5.6: Add employee details and locations to mock server

- [ ] Add `GET /customers/:customerId/employees/:employeeId` mock
- [ ] Add `GET /customers/:customerId/locations` mock
- [ ] Add `PUT /customers/:customerId/location-roles` mock

---

## Steel Thread 6: Location Management

**Goal**: Admin can view and create locations.

Implements: Scenario 5 (Admin Creates New Location)

### Task 6.1: Implement LocationsTab with data table

- [ ] Fetch locations from `/api/locations`
- [ ] Display in DataTable with columns: Name, Security System Status
- [ ] Add "Add Location" button

### Task 6.2: Add create location BFF route

- [ ] Create `POST /api/locations` route
- [ ] Forward to existing Customer Service endpoint

### Task 6.3: Create AddLocationDialog component

- [ ] Modal with Location Name field
- [ ] Submit creates location via API
- [ ] On success: close, toast, refresh list

### Task 6.4: Add create location to mock server

- [ ] Add `POST /customers/:customerId/locations` mock
- [ ] Return created location with generated ID

---

## Steel Thread 7: Team List and Details

**Goal**: Admin can view teams and their details.

Implements: Scenario 2 (Admin Manages Team Permissions) - Steps 1-3

### Task 7.1: Add team endpoints to Customer Service

- [ ] Add `GET /customers/{customerId}/teams` endpoint
- [ ] Add `GET /customers/{customerId}/teams/{teamId}` endpoint
- [ ] Team details include: name, members list, location role assignments

### Task 7.2: Add team BFF routes

- [ ] Create `GET /api/teams` route
- [ ] Create `GET /api/teams/[id]` route

### Task 7.3: Implement TeamsTab with data table

- [ ] Fetch teams from `/api/teams`
- [ ] Display in DataTable with columns: Name, Member Count
- [ ] Click row to view team details

### Task 7.4: Create TeamDetailView component

- [ ] Display team name
- [ ] List team members with names
- [ ] List current location role assignments
- [ ] "Add Location Role" button

### Task 7.5: Add team endpoints to mock server

- [ ] Add `GET /customers/:customerId/teams` mock
- [ ] Add `GET /customers/:customerId/teams/:teamId` mock

---

## Steel Thread 8: Team Location Role Assignment

**Goal**: Admin can assign location roles to teams.

Implements: Scenario 2 (Admin Manages Team Permissions) - Steps 4-5

### Task 8.1: Add team location role endpoint

- [ ] Add `POST /customers/{customerId}/teams/{teamId}/location-roles` endpoint
- [ ] Accept: locationId, roleName
- [ ] Publishes TeamAssignedLocationRole event

### Task 8.2: Add team location role BFF route

- [ ] Create `POST /api/teams/[id]/location-roles` route

### Task 8.3: Create AddTeamLocationRoleDialog component

- [ ] Modal with Location and Role dropdowns
- [ ] Submit assigns role to team
- [ ] On success: close, toast, refresh team details

### Task 8.4: Add team location role to mock server

- [ ] Add `POST /customers/:customerId/teams/:teamId/location-roles` mock

---

## Steel Thread 9: Delete Operations with Confirmation

**Goal**: Admin can delete employees, teams, and locations with confirmation dialogs.

### Task 9.1: Create ConfirmDeleteDialog component

- [ ] Reusable confirmation dialog using shadcn/ui AlertDialog
- [ ] Props: title, description, onConfirm, onCancel
- [ ] Destructive styling for confirm button

### Task 9.2: Add delete employee functionality

- [ ] Add `DELETE /customers/{customerId}/employees/{employeeId}` endpoint
- [ ] Add BFF route and mock
- [ ] Add delete button to employee list/detail with confirmation

### Task 9.3: Add delete team functionality

- [ ] Add `DELETE /customers/{customerId}/teams/{teamId}` endpoint
- [ ] Add BFF route and mock
- [ ] Add delete button to team list/detail with confirmation

### Task 9.4: Add delete location functionality

- [ ] Add `DELETE /customers/{customerId}/locations/{locationId}` endpoint
- [ ] Add BFF route and mock
- [ ] Add delete button to location list with confirmation

---

## Steel Thread 10: Init-DB Service

**Goal**: Create Docker service that seeds demo data via REST APIs.

Implements: Epic 6 (Init-DB Service)

### Task 10.1: Add Spring profile to disable existing DBInitializers

- [ ] Add `@Profile("!use-init-container")` to `DbInitializerConfig` in customer-service
- [ ] Add `@Profile("!use-init-container")` to `DBInitializerConfiguration` in security-system-service
- [ ] Verify services start without seeding when profile is active

### Task 10.2: Create init-db-service Spring Boot project

- [ ] Create new directory `realguardio-init-db-service`
- [ ] Set up Gradle build with Spring Boot dependencies
- [ ] Create main application class

### Task 10.3: Implement data seeding logic

- [ ] Create InitDbRunner implementing CommandLineRunner
- [ ] Call Customer Service APIs to create:
  - Customer (Acme Corporation)
  - Admin employee (admin@acme.com)
  - Non-admin employee (john.doe@acme.com)
  - 3 Locations (Oakland, Berkeley, Hayward offices)
  - Team (Security Team)
  - Team member assignment
  - Team location role (DISARMER at Oakland)
- [ ] Call Security System Service APIs to create:
  - 3 Security Systems with different states
- [ ] Handle idempotency (skip if data exists)

### Task 10.4: Add init-db-service to docker-compose

- [ ] Create Dockerfile for init-db-service
- [ ] Add service to docker-compose.yaml with:
  - depends_on: customer-service, security-system-service
  - restart: "no" (run once)
  - SPRING_PROFILES_ACTIVE includes use-init-container
- [ ] Add use-init-container profile to other services

### Task 10.5: Test init-db-service with docker-compose

- [ ] Start full stack with docker-compose up
- [ ] Verify init-db-service seeds data successfully
- [ ] Verify admin@acme.com can log in
- [ ] Verify john.doe@acme.com can log in

---

## Steel Thread 11: E2E Tests Against Real Backend

**Goal**: Critical path E2E tests run against real docker-compose stack.

Implements: NFR-TEST-2

### Task 11.1: Create E2E test configuration for real backend

- [ ] Add npm script for E2E tests with real backend
- [ ] Configure environment variables for real service URLs
- [ ] Document how to run E2E tests against docker-compose

### Task 11.2: Add E2E test for admin login and employee list

- [ ] Create AdminFlowPage page object
- [ ] Test: admin@acme.com logs in, sees Employees tab, views employee list

### Task 11.3: Add E2E test for security system arm/disarm

- [ ] Test: john.doe@acme.com logs in, sees Security Systems tab
- [ ] Test: clicks DISARM on Oakland system, sees state change

### Task 11.4: Add E2E test for create employee flow

- [ ] Test: admin creates new employee via dialog
- [ ] Test: new employee appears in list

---

## Steel Thread 12: Edit Operations

**Goal**: Admin can edit employees, teams, and locations.

### Task 12.1: Add update employee functionality

- [ ] Add `PUT /customers/{customerId}/employees/{employeeId}` endpoint
- [ ] Add BFF route and mock
- [ ] Create EditEmployeeDialog with pre-populated form
- [ ] Add edit button to employee detail view

### Task 12.2: Add update team functionality

- [ ] Add `PUT /customers/{customerId}/teams/{teamId}` endpoint
- [ ] Add BFF route and mock
- [ ] Create EditTeamDialog
- [ ] Add edit button to team detail view

### Task 12.3: Add update location functionality

- [ ] Add `PUT /customers/{customerId}/locations/{locationId}` endpoint
- [ ] Add BFF route and mock
- [ ] Create EditLocationDialog
- [ ] Add edit button to location list

---

## Steel Thread 13: Team Member Management

**Goal**: Admin can add and remove team members.

### Task 13.1: Add team member endpoints

- [ ] Add `POST /customers/{customerId}/teams/{teamId}/members` endpoint
- [ ] Add `DELETE /customers/{customerId}/teams/{teamId}/members/{employeeId}` endpoint

### Task 13.2: Add team member BFF routes

- [ ] Create `POST /api/teams/[id]/members` route
- [ ] Create `DELETE /api/teams/[id]/members/[employeeId]` route

### Task 13.3: Implement add/remove member UI

- [ ] Add "Add Member" button to team detail
- [ ] Create AddTeamMemberDialog with employee dropdown
- [ ] Add remove button next to each team member
- [ ] Confirm before removing

### Task 13.4: Add team member endpoints to mock server

- [ ] Add POST and DELETE mock endpoints for team members

---

## Dependency Graph

```
ST1 (UI Foundation + Actions)
 └── ST2 (Tab Navigation)
      ├── ST3 (Employee List)
      │    └── ST4 (Create Employee)
      │         └── ST5 (Employee Details + Roles)
      │              └── ST9 (Delete Operations)
      │                   └── ST12 (Edit Operations)
      ├── ST6 (Location Management)
      └── ST7 (Team List + Details)
           └── ST8 (Team Location Roles)
                └── ST13 (Team Members)

ST10 (Init-DB Service) - can start after ST5
ST11 (E2E Tests) - requires ST10
```

---

## Change History

### 2026-01-29: Initial plan created

- Created based on specification
- 13 steel threads covering all scenarios
- Primary scenario (Admin Onboards New Employee) spans threads 3-5
- Foundation work in threads 1-2
- Developer tooling in threads 10-11

### 2026-01-29: Corrected security system action endpoints

- Updated Task 1.3 to use existing `PUT /securitysystems/{id}` endpoint pattern
- Security system actions passed in request body as `{ action: "ARM" | "DISARM" | "ACKNOWLEDGE" }`
- Only ACKNOWLEDGE action needs to be added to existing backend endpoint
- Updated mock server task accordingly

### 2026-01-29: Added customer roles fetching in OAuth callback

- Added Task 2.1: `GET /me/roles` endpoint in Customer Service
- Added Task 2.2: Fetch customer roles during OAuth callback, refresh on token refresh
- Updated Task 2.4 to use `session.customerRoles` and `session.hasSecuritySystemAccess`
- This enables tab visibility based on customer roles (not just IAM roles)
