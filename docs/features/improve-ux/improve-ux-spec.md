# Improve UX - Product Requirements Specification

## Purpose and Background

RealGuardIO is a security system management platform built with a microservices architecture. The current UI is a minimal placeholder that demonstrates basic OAuth2 authentication and displays a list of security systems. This feature modernizes the UI into a fully functional admin and security system management interface.

The platform already has a rich backend with:
- Customer Service managing customers, locations, teams, and employees
- Security System Service managing security systems with CQRS location-roles replica
- Oso-based authorization with role-based access control
- OAuth2/OIDC authentication via Spring Authorization Server

This feature bridges the gap between the capable backend and the placeholder UI.

---

## Target Users and Personas

### Persona 1: Company Administrator
- **Role**: `COMPANY_ROLE_ADMIN`
- **Responsibilities**: Manages the company's employees, teams, and locations
- **Goals**: Efficiently onboard employees, organize them into teams, and assign appropriate access to locations
- **Technical level**: Business user, not technical

### Persona 2: Security Operator
- **Roles**: `SECURITY_SYSTEM_ARMER`, `SECURITY_SYSTEM_DISARMER`, `SECURITY_SYSTEM_VIEWER`
- **Responsibilities**: Monitor and control security systems at assigned locations
- **Goals**: Quickly view status and arm/disarm security systems as needed
- **Technical level**: Frontline worker, needs simple and fast interface

---

## Problem Statement

The current UI is a basic placeholder that:
- Only displays a list of security systems
- Has non-functional action buttons (arm/disarm only log to console)
- Lacks any admin management capabilities
- Uses inconsistent styled-jsx styling
- Provides no CRUD functionality for employees, teams, or locations

Users cannot perform essential tasks through the UI, making the platform unusable for real-world demonstrations or usage.

---

## Goals

1. **Enable Admin Management**: Allow company admins to create, view, update, and delete employees, teams, and locations
2. **Enable Security Operations**: Allow security operators to arm, disarm, and acknowledge alarms on security systems
3. **Modernize UI**: Adopt shadcn/ui component library for consistent, accessible, professional appearance
4. **Role-Based Experience**: Show appropriate features based on user's roles
5. **Demo-Ready**: Provide seed data for immediate demonstration of all features

---

## In-Scope

- Full CRUD UI for employees, teams, and locations
- Functional arm/disarm/acknowledge actions for security systems
- Top-tab navigation with role-based visibility
- shadcn/ui component library adoption
- Migration of existing styled-jsx components
- Init-DB Docker service for seeding demo data
- Comprehensive mock server for testing
- E2E tests for critical paths against real backend

## Out-of-Scope

- Multi-customer support (UI assumes single customer context)
- User self-registration or password management
- Real-time updates via WebSocket
- Mobile-specific responsive design (basic responsiveness only)
- Audit logging UI
- Dashboard analytics or reporting
- Customer CRUD (only one customer in demo)

---

## High-Level Functional Requirements

### FR-1: Navigation and Layout

| ID | Requirement |
|----|-------------|
| FR-1.1 | Display horizontal tab navigation below the header |
| FR-1.2 | Tabs: Employees, Teams, Locations, Security Systems |
| FR-1.3 | Filter visible tabs based on user roles (see Security Requirements) |
| FR-1.4 | Maintain header with logo, user name, and sign-out button |

### FR-2: Employee Management

| ID | Requirement |
|----|-------------|
| FR-2.1 | List all employees in a data table with columns: Name, Email, Roles |
| FR-2.2 | Create employee form: first name, last name, email |
| FR-2.3 | View employee details including assigned location roles |
| FR-2.4 | Edit employee details (name, email) |
| FR-2.5 | Delete employee with confirmation dialog |
| FR-2.6 | Assign/remove location roles to individual employees |

### FR-3: Team Management

| ID | Requirement |
|----|-------------|
| FR-3.1 | List all teams in a data table with columns: Name, Member Count |
| FR-3.2 | Create team form: team name |
| FR-3.3 | View team details including members and location roles |
| FR-3.4 | Edit team name |
| FR-3.5 | Delete team with confirmation dialog |
| FR-3.6 | Add/remove team members |
| FR-3.7 | Assign location roles to team (from team detail page) |

### FR-4: Location Management

| ID | Requirement |
|----|-------------|
| FR-4.1 | List all locations in a data table with columns: Name, Security System Status |
| FR-4.2 | Create location form: location name |
| FR-4.3 | View location details including assigned security system |
| FR-4.4 | Edit location name |
| FR-4.5 | Delete location with confirmation dialog |

### FR-5: Security System Control

| ID | Requirement |
|----|-------------|
| FR-5.1 | List security systems in a data table with columns: Location, State, Actions |
| FR-5.2 | Display state with visual indicators: DISARMED, ARMED, ALARMED, FAULT |
| FR-5.3 | ARM action button (visible when state is DISARMED) |
| FR-5.4 | DISARM action button (visible when state is ARMED or ALARMED) |
| FR-5.5 | ACKNOWLEDGE action button (visible when state is ALARMED) |
| FR-5.6 | Filter displayed systems to only those user has access to |
| FR-5.7 | Filter available actions based on user's location-specific permissions |

### FR-6: Init-DB Service

| ID | Requirement |
|----|-------------|
| FR-6.1 | Docker service that seeds database via REST API calls |
| FR-6.2 | Create Customer: Acme Corporation |
| FR-6.3 | Create Admin: admin@acme.com with COMPANY_ROLE_ADMIN |
| FR-6.4 | Create Employee: john.doe@acme.com |
| FR-6.5 | Create 3 Locations: Oakland office, Berkeley office, Hayward office |
| FR-6.6 | Create Team: Security Team with john.doe@acme.com as member |
| FR-6.7 | Assign SECURITY_SYSTEM_DISARMER role to Security Team at Oakland |
| FR-6.8 | Create 3 Security Systems (one per location) |
| FR-6.9 | Register users in IAM service via existing UserService |
| FR-6.10 | Disable existing DBInitializers via Spring profile |

### FR-7: User Feedback

| ID | Requirement |
|----|-------------|
| FR-7.1 | Toast notifications for successful actions |
| FR-7.2 | Toast notifications for errors |
| FR-7.3 | Confirmation dialogs for delete operations only |
| FR-7.4 | Loading states during API calls |

---

## Security Requirements

### Authorization Model

The system uses Oso-based authorization with the following roles:

| Role | Scope | Permissions |
|------|-------|-------------|
| `COMPANY_ROLE_ADMIN` | Customer-level | `createCustomerEmployee`, `createLocation`, view/manage all employees, teams, locations |
| `SECURITY_SYSTEM_ARMER` | Location-level | `arm`, `view` security systems at assigned locations |
| `SECURITY_SYSTEM_DISARMER` | Location-level | `disarm`, `view` security systems at assigned locations |
| `SECURITY_SYSTEM_VIEWER` | Location-level | `view` security systems at assigned locations |

### Endpoint Authorization Matrix

#### Navigation & UI

| Operation | Required Role | Authorization Check |
|-----------|---------------|---------------------|
| View Employees tab | `COMPANY_ROLE_ADMIN` | User has role at customer level |
| View Teams tab | `COMPANY_ROLE_ADMIN` | User has role at customer level |
| View Locations tab | `COMPANY_ROLE_ADMIN` | User has role at customer level |
| View Security Systems tab | Any `SECURITY_SYSTEM_*` | User has any security system role at any location |

#### Employee Operations

| Operation | Required Role | Authorization Check |
|-----------|---------------|---------------------|
| List employees | `COMPANY_ROLE_ADMIN` | User has role; returns only employees from user's customer |
| Create employee | `COMPANY_ROLE_ADMIN` | User has role; creates within user's customer |
| View employee | `COMPANY_ROLE_ADMIN` | User has role; employee belongs to user's customer |
| Update employee | `COMPANY_ROLE_ADMIN` | User has role; employee belongs to user's customer |
| Delete employee | `COMPANY_ROLE_ADMIN` | User has role; employee belongs to user's customer |
| Assign location role | `COMPANY_ROLE_ADMIN` | User has role; employee and location belong to user's customer |

#### Team Operations

| Operation | Required Role | Authorization Check |
|-----------|---------------|---------------------|
| List teams | `COMPANY_ROLE_ADMIN` | User has role; returns only teams from user's customer |
| Create team | `COMPANY_ROLE_ADMIN` | User has role; creates within user's customer |
| View team | `COMPANY_ROLE_ADMIN` | User has role; team belongs to user's customer |
| Update team | `COMPANY_ROLE_ADMIN` | User has role; team belongs to user's customer |
| Delete team | `COMPANY_ROLE_ADMIN` | User has role; team belongs to user's customer |
| Add team member | `COMPANY_ROLE_ADMIN` | User has role; team and employee belong to user's customer |
| Remove team member | `COMPANY_ROLE_ADMIN` | User has role; team and employee belong to user's customer |
| Assign location role to team | `COMPANY_ROLE_ADMIN` | User has role; team and location belong to user's customer |

#### Location Operations

| Operation | Required Role | Authorization Check |
|-----------|---------------|---------------------|
| List locations | `COMPANY_ROLE_ADMIN` | User has role; returns only locations from user's customer |
| Create location | `COMPANY_ROLE_ADMIN` | User has role; creates within user's customer |
| View location | `COMPANY_ROLE_ADMIN` | User has role; location belongs to user's customer |
| Update location | `COMPANY_ROLE_ADMIN` | User has role; location belongs to user's customer |
| Delete location | `COMPANY_ROLE_ADMIN` | User has role; location belongs to user's customer |

#### Security System Operations

| Operation | Required Role | Authorization Check |
|-----------|---------------|---------------------|
| List security systems | Any `SECURITY_SYSTEM_*` | Returns only systems at locations where user has a security system role |
| View security system | `SECURITY_SYSTEM_VIEWER` (or higher) | User has view permission at system's location |
| ARM security system | `SECURITY_SYSTEM_ARMER` | User has arm permission at system's location |
| DISARM security system | `SECURITY_SYSTEM_DISARMER` | User has disarm permission at system's location |
| ACKNOWLEDGE alarm | `SECURITY_SYSTEM_DISARMER` | User has disarm permission at system's location |

### Authorization Implementation

- Backend determines user's Customer from JWT token claims
- Backend filters all data to user's authorized resources
- Oso Integration Service maintains authorization facts from domain events
- Security System Service maintains CQRS replica of location roles for fast authorization queries

### Customer Roles in UI Session

The UI needs customer roles (distinct from IAM roles) to determine tab visibility:

**Two Types of Roles:**
| Type | Source | Examples |
|------|--------|----------|
| IAM Roles | JWT from IAM Service | `REALGUARDIO_ADMIN`, `REALGUARDIO_CUSTOMER_EMPLOYEE` |
| Customer Roles | Customer Service DB | `COMPANY_ROLE_ADMIN`, `SECURITY_SYSTEM_ARMER`, `SECURITY_SYSTEM_DISARMER` |

**Solution: Fetch customer roles during OAuth callback (Option A2)**
- BFF calls `GET /me/roles` during `jwtCallback` when user logs in
- Customer roles stored in session alongside IAM roles
- Roles refreshed on token refresh (~2 min intervals)
- UI accesses via `session.customerRoles` and `session.hasSecuritySystemAccess`

**Required endpoint:** `GET /me/roles` in Customer Service returns:
```json
{
  "customerRoles": ["COMPANY_ROLE_ADMIN"],
  "hasSecuritySystemAccess": true
}
```

**Tab visibility logic:**
- Employees, Teams, Locations tabs: `session.customerRoles.includes('COMPANY_ROLE_ADMIN')`
- Security Systems tab: `session.hasSecuritySystemAccess`

---

## Non-Functional Requirements

### UX Requirements

| ID | Requirement |
|----|-------------|
| NFR-UX-1 | Consistent visual design using shadcn/ui components |
| NFR-UX-2 | Responsive layout for desktop and tablet viewports |
| NFR-UX-3 | Clear visual feedback for all user actions (loading, success, error) |
| NFR-UX-4 | Intuitive navigation with tab-based structure |
| NFR-UX-5 | Accessible components (ARIA compliance via shadcn/ui) |

### Performance Requirements

| ID | Requirement |
|----|-------------|
| NFR-PERF-1 | Page load time < 3 seconds on initial load |
| NFR-PERF-2 | API response rendering < 500ms after data received |
| NFR-PERF-3 | Action feedback (arm/disarm) visible within 1 second |

### Reliability Requirements

| ID | Requirement |
|----|-------------|
| NFR-REL-1 | Graceful error handling for API failures |
| NFR-REL-2 | Clear error messages displayed to users |
| NFR-REL-3 | No data loss on network interruption (show error, don't clear forms) |

### Testability Requirements

| ID | Requirement |
|----|-------------|
| NFR-TEST-1 | Comprehensive mock server supporting all endpoints |
| NFR-TEST-2 | E2E tests for critical paths against real backend |
| NFR-TEST-3 | Page Object pattern for E2E tests |
| NFR-TEST-4 | Unit tests for React components |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| All CRUD operations functional | 100% of create, read, update, delete for employees, teams, locations |
| Security system actions working | arm, disarm, acknowledge all functional |
| Role-based access enforced | Admin-only tabs hidden from non-admins |
| Test coverage | E2E tests pass for all critical paths |
| Demo readiness | Init-DB service successfully seeds all demo data |

---

## Epics and User Stories

### Epic 1: UI Foundation

**E1-S1**: As a user, I want to see a modern, consistent UI so that the application feels professional and trustworthy.
- Acceptance: shadcn/ui components replace all styled-jsx components

**E1-S2**: As a user, I want to navigate between sections using tabs so that I can quickly access different features.
- Acceptance: Horizontal tabs below header; clicking tab shows corresponding content

**E1-S3**: As a non-admin user, I want to only see tabs relevant to my role so that I'm not confused by inaccessible features.
- Acceptance: Admin tabs hidden for users without COMPANY_ROLE_ADMIN

### Epic 2: Employee Management

**E2-S1**: As an admin, I want to view a list of all employees so that I can see who works for the company.
- Acceptance: Data table showing name, email, roles

**E2-S2**: As an admin, I want to create a new employee so that I can onboard new team members.
- Acceptance: Form with first name, last name, email; employee appears in list after creation

**E2-S3**: As an admin, I want to view employee details so that I can see their assigned roles.
- Acceptance: Detail view shows employee info and location role assignments

**E2-S4**: As an admin, I want to edit employee information so that I can correct mistakes or update details.
- Acceptance: Edit form pre-populated; changes reflected after save

**E2-S5**: As an admin, I want to delete an employee so that I can remove people who have left.
- Acceptance: Confirmation dialog; employee removed from list after confirmation

**E2-S6**: As an admin, I want to assign location roles to an employee so that they can access security systems.
- Acceptance: Select location and role; assignment visible in employee details

### Epic 3: Team Management

**E3-S1**: As an admin, I want to view a list of all teams so that I can see organizational structure.
- Acceptance: Data table showing team name and member count

**E3-S2**: As an admin, I want to create a new team so that I can organize employees.
- Acceptance: Form with team name; team appears in list after creation

**E3-S3**: As an admin, I want to view team details so that I can see members and role assignments.
- Acceptance: Detail view shows team members and location role assignments

**E3-S4**: As an admin, I want to add employees to a team so that they inherit team permissions.
- Acceptance: Select employee from list; employee appears in team members

**E3-S5**: As an admin, I want to remove employees from a team so that I can reorganize.
- Acceptance: Remove button on member; member disappears from team

**E3-S6**: As an admin, I want to assign location roles to a team so that all members get access.
- Acceptance: Select location and role from team detail page; assignment visible

**E3-S7**: As an admin, I want to delete a team so that I can remove obsolete organizational units.
- Acceptance: Confirmation dialog; team removed from list after confirmation

### Epic 4: Location Management

**E4-S1**: As an admin, I want to view a list of all locations so that I can see company facilities.
- Acceptance: Data table showing location name and security system status

**E4-S2**: As an admin, I want to create a new location so that I can add new facilities.
- Acceptance: Form with location name; location appears in list after creation

**E4-S3**: As an admin, I want to view location details so that I can see associated security system.
- Acceptance: Detail view shows location info and security system status

**E4-S4**: As an admin, I want to edit location information so that I can update facility names.
- Acceptance: Edit form pre-populated; changes reflected after save

**E4-S5**: As an admin, I want to delete a location so that I can remove closed facilities.
- Acceptance: Confirmation dialog; location removed from list after confirmation

### Epic 5: Security System Control

**E5-S1**: As a security operator, I want to view security systems I have access to so that I can monitor their status.
- Acceptance: Table shows only systems at my assigned locations

**E5-S2**: As a security operator with ARMER role, I want to arm a security system so that the facility is protected.
- Acceptance: ARM button visible for DISARMED systems; state changes to ARMED after click

**E5-S3**: As a security operator with DISARMER role, I want to disarm a security system so that people can enter.
- Acceptance: DISARM button visible for ARMED/ALARMED systems; state changes to DISARMED after click

**E5-S4**: As a security operator with DISARMER role, I want to acknowledge an alarm so that I can indicate I'm aware of it.
- Acceptance: ACKNOWLEDGE button visible for ALARMED systems; action recorded

**E5-S5**: As a security operator, I want to see only actions I'm permitted to perform so that I don't get errors.
- Acceptance: Buttons filtered based on my location-specific permissions

### Epic 6: Init-DB Service

**E6-S1**: As a developer, I want an init-db-service that seeds demo data so that I can demonstrate all features.
- Acceptance: Docker service creates customer, admin, employee, locations, team, security systems

**E6-S2**: As a developer, I want existing DBInitializers disabled when using init-db-service so that data isn't duplicated.
- Acceptance: Spring profile disables DBInitializers; profile activated in docker-compose

### Epic 7: Testing Infrastructure

**E7-S1**: As a developer, I want a comprehensive mock server so that I can test UI in isolation.
- Acceptance: Mock server implements all endpoints used by UI

**E7-S2**: As a developer, I want E2E tests for critical paths so that I can verify real integration.
- Acceptance: E2E tests run against docker-compose stack and pass

---

## User-Facing Scenarios

These scenarios define the main end-to-end user journeys that a steel-thread implementation plan should address.

### Scenario 1: Admin Onboards New Employee (Primary Scenario)

**Actors**: Company Administrator (admin@acme.com)

**Preconditions**:
- User is logged in with COMPANY_ROLE_ADMIN
- Demo data is seeded

**Flow**:
1. Admin views Employees tab
2. Admin clicks "Add Employee"
3. Admin fills form: Jane Smith, jane.smith@acme.com
4. Admin submits form
5. Employee appears in list
6. Admin clicks employee to view details
7. Admin assigns SECURITY_SYSTEM_ARMER role at Berkeley office
8. Admin saves assignment

**Postconditions**:
- Employee exists in system
- Employee can log in (IAM user created)
- Employee has arm permission at Berkeley office

---

### Scenario 2: Admin Manages Team Permissions

**Actors**: Company Administrator

**Preconditions**:
- User is logged in with COMPANY_ROLE_ADMIN
- Security Team exists with john.doe@acme.com

**Flow**:
1. Admin views Teams tab
2. Admin clicks "Security Team"
3. Admin views current location roles (Oakland - DISARMER)
4. Admin adds new location role: Berkeley - DISARMER
5. Admin saves assignment

**Postconditions**:
- Team has DISARMER role at both Oakland and Berkeley
- john.doe@acme.com inherits both location permissions

---

### Scenario 3: Security Operator Arms System

**Actors**: Security Operator with ARMER role (e.g., jane.smith@acme.com from Scenario 1)

**Preconditions**:
- User is logged in with SECURITY_SYSTEM_ARMER at Berkeley
- Berkeley security system is DISARMED

**Flow**:
1. Operator views Security Systems tab
2. Operator sees Berkeley system (DISARMED) with ARM button
3. Operator clicks ARM
4. System state changes to ARMED
5. Toast notification confirms success

**Postconditions**:
- Berkeley security system is ARMED

---

### Scenario 4: Security Operator Responds to Alarm

**Actors**: Security Operator with DISARMER role (john.doe@acme.com)

**Preconditions**:
- User is logged in with SECURITY_SYSTEM_DISARMER at Oakland
- Oakland security system is ALARMED

**Flow**:
1. Operator views Security Systems tab
2. Operator sees Oakland system (ALARMED) with DISARM and ACKNOWLEDGE buttons
3. Operator clicks ACKNOWLEDGE
4. Operator clicks DISARM
5. System state changes to DISARMED
6. Toast notifications confirm each action

**Postconditions**:
- Oakland security system is DISARMED
- Alarm was acknowledged

---

### Scenario 5: Admin Creates New Location

**Actors**: Company Administrator

**Preconditions**:
- User is logged in with COMPANY_ROLE_ADMIN

**Flow**:
1. Admin views Locations tab
2. Admin clicks "Add Location"
3. Admin fills form: San Francisco office
4. Admin submits form
5. Location appears in list

**Postconditions**:
- New location exists in system
- Location available for role assignments

---

### Scenario 6: Non-Admin User Experience

**Actors**: Security Operator (john.doe@acme.com) without admin role

**Preconditions**:
- User is logged in with only SECURITY_SYSTEM_DISARMER role

**Flow**:
1. User sees only Security Systems tab (Employees, Teams, Locations tabs hidden)
2. User views security systems
3. User sees only Oakland system (only location with permission)
4. User sees only DISARM action (no ARM because not ARMER)

**Postconditions**:
- User cannot access admin features
- User can only operate on authorized resources

---

## Primary End-to-End Scenario for Steel-Thread

**Scenario 1: Admin Onboards New Employee** is the primary scenario because it:
- Exercises the core admin workflow (CRUD operations)
- Requires navigation, form submission, and role assignment
- Triggers IAM user registration
- Sets up subsequent security system access
- Validates both UI and backend API integration
- Demonstrates role-based authorization

This scenario should be the foundation for a steel-thread implementation plan (to be created in a subsequent step).

---

## REST APIs Required

### Existing Backend APIs

**Customer Service:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/customers` | List customers |
| POST | `/customers` | Create customer |
| POST | `/customers/{id}/employees` | Create employee |
| PUT | `/customers/{id}/location-roles` | Assign location role to employee |
| POST | `/customers/{id}/locations` | Create location |
| GET | `/locations/{id}/roles` | Get user's roles at a location |

**Security System Service:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/securitysystems` | List security systems |
| GET | `/securitysystems/{id}` | Get single security system |
| PUT | `/securitysystems/{id}` | Perform action (ARM/DISARM via `action` field in body) |

**Orchestration Service:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/securitysystems` | Create security system (saga-based) |

**BFF Routes:**

| Route | Purpose |
|-------|---------|
| GET `/api/securitysystems` | Proxies to Security System Service |

### New APIs Required

**Customer Service - New endpoints needed:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/me/roles` | Get current user's customer roles and security system access flag |
| GET | `/customers/{id}/employees` | List employees for customer |
| GET | `/customers/{id}/employees/{employeeId}` | Get employee details |
| PUT | `/customers/{id}/employees/{employeeId}` | Update employee |
| DELETE | `/customers/{id}/employees/{employeeId}` | Delete employee |
| GET | `/customers/{id}/locations` | List locations for customer |
| GET | `/customers/{id}/locations/{locationId}` | Get location details |
| PUT | `/customers/{id}/locations/{locationId}` | Update location |
| DELETE | `/customers/{id}/locations/{locationId}` | Delete location |
| GET | `/customers/{id}/teams` | List teams for customer |
| POST | `/customers/{id}/teams` | Create team |
| GET | `/customers/{id}/teams/{teamId}` | Get team details |
| PUT | `/customers/{id}/teams/{teamId}` | Update team |
| DELETE | `/customers/{id}/teams/{teamId}` | Delete team |
| POST | `/customers/{id}/teams/{teamId}/members` | Add team member |
| DELETE | `/customers/{id}/teams/{teamId}/members/{employeeId}` | Remove team member |
| POST | `/customers/{id}/teams/{teamId}/location-roles` | Add team location role |
| DELETE | `/customers/{id}/teams/{teamId}/location-roles/{roleId}` | Remove team location role |

**Security System Service - Enhancements needed:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| PUT | `/securitysystems/{id}` | Add support for ACKNOWLEDGE action (currently only ARM/DISARM) |

**BFF Routes - New routes needed:**

| Route | Purpose |
|-------|---------|
| PUT `/api/securitysystems/[id]` | Proxy arm/disarm/acknowledge actions |
| GET `/api/employees` | Proxy employee list |
| POST `/api/employees` | Proxy create employee |
| GET `/api/employees/[id]` | Proxy employee details |
| PUT `/api/employees/[id]` | Proxy update employee |
| DELETE `/api/employees/[id]` | Proxy delete employee |
| GET `/api/locations` | Proxy location list |
| POST `/api/locations` | Proxy create location |
| PUT `/api/locations/[id]` | Proxy update location |
| DELETE `/api/locations/[id]` | Proxy delete location |
| GET `/api/teams` | Proxy team list |
| POST `/api/teams` | Proxy create team |
| GET `/api/teams/[id]` | Proxy team details |
| PUT `/api/teams/[id]` | Proxy update team |
| DELETE `/api/teams/[id]` | Proxy delete team |
| POST `/api/teams/[id]/members` | Proxy add team member |
| DELETE `/api/teams/[id]/members/[employeeId]` | Proxy remove team member |
| POST `/api/teams/[id]/location-roles` | Proxy add team location role |

---

## Change History

### 2026-01-28: Initial specification created

- Created based on brainstorming discussion
- Classified as user-facing feature
- Defined all functional requirements, security requirements, and user stories
- Identified primary scenario: Admin Onboards New Employee

### 2026-01-29: Corrected REST API documentation

- Updated REST APIs section to reflect actual existing endpoints discovered in codebase
- Security system actions use `PUT /securitysystems/{id}` with action in body (not separate POST endpoints)
- Added existing BFF routes and Orchestration Service endpoint
- Clarified that ACKNOWLEDGE action needs to be added to existing PUT endpoint
- Added detailed BFF route requirements

### 2026-01-29: Added customer roles in UI session

- Documented two types of roles: IAM roles (in JWT) vs Customer roles (in Customer Service DB)
- Added `GET /me/roles` endpoint requirement
- Specified Option A2: fetch roles during OAuth callback, refresh on token refresh (~2 min)
- Added authorization implementation details for tab visibility
