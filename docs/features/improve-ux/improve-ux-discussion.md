# Improve UX - Discussion

## Summary of Existing Architecture

Based on codebase exploration:

### Current Domain Model
- **Customer** - The organization entity (not "Company" - clarification needed)
- **Location** - Physical location belonging to a Customer
- **Team** - Group of employees with shared roles at locations
- **CustomerEmployee** - Individual user in the system
- **SecuritySystem** - Arm/disarm-able system with states: DISARMED, ARMED, ALARMED, FAULT

### Current Authorization Model (Oso)
- Roles: SECURITY_SYSTEM_ARMER, SECURITY_SYSTEM_DISARMER, COMPANY_ROLE_ADMIN, SECURITY_SYSTEM_VIEWER
- Roles can be assigned at Customer level or Location level
- Team membership grants inherited location roles

### Current UI Stack
- NextJS 14+ / TypeScript / React
- OAuth2 PKCE authentication via next-auth
- Described as "basic placeholder"

### Current Services
- Customer Service (manages customers, locations, teams, employees)
- Security System Service (manages security systems, CQRS location-roles replica)
- Orchestration Service (saga coordination)
- Oso Integration Service (syncs authorization facts)
- IAM Service (Spring Authorization Server)
- BFF (NextJS frontend)

---

## Questions and Answers

### Q1: Terminology - Company vs Customer

**Q:** The idea mentions "Company" but the codebase uses "Customer". Which term should we use?

**A:** Use existing "Customer" terminology - it was a misspeak. The init-db-service creates a Customer with an admin CustomerEmployee.

---

### Q2: Init-DB Service Implementation

**Q:** How should the init-db-service be implemented?

**A:** Separate Docker service - A new Spring Boot service that runs once at startup, calls the existing REST APIs to create entities, then exits.

**Additional note:** Existing `DBInitializer` classes in Customer Service and Security System Service must be disabled when using the init container. These currently run via `CommandLineRunner` without profile guards. Solution: Add `@Profile("!use-init-container")` or similar to the `DbInitializerConfig` and `DBInitializerConfiguration` classes, and activate the profile in docker-compose when using the init container.

---

### Q3: Admin User Credentials

**Q:** What username should the Customer admin use for login?

**A:** `admin@acme.com` - Match the email format the existing DBInitializer uses. Password will be hardcoded to "password".

---

### Q4: IAM Service User Registration

**Q:** How should multiple users be registered with the IAM service?

**A:** Use existing infrastructure. The `UserServiceImpl` in `customer-service-iam-integration` already calls `POST /api/users` on the IAM service to create users. When the init-db-service calls Customer Service REST APIs to create employees, the Customer Service will automatically register them with IAM service. Password is hardcoded to `{noop}password`.

---

### Q5: Admin UI Scope

**Q:** What's the desired scope for the admin management UI?

**A:** Full CRUD UI - Complete pages for managing employees, teams, and locations with create/edit/delete forms, tables, and proper navigation.

---

### Q6: UI Framework/Component Library

**Q:** What styling approach should be used for the new admin UI?

**A:** Use a component library (shadcn/ui). This provides pre-built accessible components (tables, forms, modals) for faster CRUD development. shadcn/ui is built on Tailwind (already configured) and copies components into the codebase (no heavy runtime dependency). Migrate existing styled-jsx components to shadcn/ui + Tailwind for consistency.

---

### Q7: Navigation Structure

**Q:** How should users access the different admin sections?

**A:** Top tabs - Horizontal tabs below the header for switching between sections (Employees, Teams, Locations, Security Systems).

---

### Q8: Role-Based Navigation

**Q:** How should tabs be shown based on user roles?

**A:** Role-filtered tabs based on existing `RolesAndPermissions` model:
- **Employees, Teams, Locations tabs** → visible only to users with `COMPANY_ROLE_ADMIN` role
- **Security Systems tab** → visible to users with any `SECURITY_SYSTEM_*` role (ARMER, DISARMER, VIEWER)
- Actions within Security Systems are filtered by location-specific permissions (arm/disarm/view)

Existing roles from `RolesAndPermissions.java`:
- Customer Service: `COMPANY_ROLE_ADMIN` → `createCustomerEmployee`, `createLocation`
- Security System Service: `SECURITY_SYSTEM_ARMER` (arm, view), `SECURITY_SYSTEM_DISARMER` (disarm, view), `SECURITY_SYSTEM_VIEWER` (view)

---

### Q9: Team-Location Role Assignment UI

**Q:** How should admins assign roles at locations to teams?

**A:** Nested management - Manage team-location-roles from within the Team detail page. Workflow: Team → add location role (select location, select role).

---

### Q10: Seed Data Specifics

**Q:** Is the existing seed data sufficient for demonstrating the features?

**A:** Keep as-is. The existing DBInitializer data is enough to demo admin and non-admin workflows:
- 1 Customer (Acme Corporation)
- 1 Admin (admin@acme.com with COMPANY_ROLE_ADMIN)
- 3 Locations (Oakland, Berkeley, Hayward offices)
- 1 Team (Security Team)
- 1 Non-admin employee (john.doe@acme.com) with DISARMER role at Oakland via team
- 3 Security Systems (one per location, different states)

---

### Q11: REST API Development Approach

**Q:** The backend REST APIs need significant expansion for full CRUD. How should this be handled?

**A:** Incrementally enhance the UI and build supporting REST APIs as needed. Don't build all APIs upfront - add them as each UI feature requires them. This aligns with TDD/incremental development workflow.

Current APIs:
- `GET /customers` - list customers
- `POST /customers` - create customer
- `POST /customers/{id}/employees` - create employee
- `PUT /customers/{id}/location-roles` - assign location role
- `POST /customers/{id}/locations` - create location
- `GET /locations/{id}/roles` - get roles for location

APIs to add incrementally as UI features are built:
- List/Get/Update/Delete for employees
- List/Get/Update/Delete for locations
- Team CRUD + member management + location role management

---

### Q12: Security System Actions

**Q:** The Security Systems table shows action buttons but they only log to console. Should implementing actual arm/disarm/acknowledge be part of this feature?

**A:** Yes, include it. Implement arm/disarm/acknowledge API calls in the UI as part of this feature.

---

### Q13: User Context / Authorization

**Q:** How does the UI know which Customer the logged-in user belongs to?

**A:** The REST API endpoints handle authorization based on the logged-in user's Customer. The backend determines the user's Customer from the JWT token and filters/authorizes data accordingly. The UI simply calls APIs and receives only data the user is authorized to see - no need to explicitly track customerId in the frontend.

---

### Q14: Confirmation Dialogs

**Q:** Should the UI show confirmation dialogs for destructive actions?

**A:** Only for deletes. Confirmation dialogs for delete operations (delete employee, delete team, delete location) since they're irreversible with potential cascading effects. No confirmation for arm/disarm - these are reversible, time-sensitive operational actions that shouldn't have friction. Include toast notifications for success/error feedback.

---

### Q15: E2E Testing Approach

**Q:** How should E2E testing work for new UI features?

**A:** Hybrid approach:
1. **Mock server should be comprehensive** - Expand to support all new endpoints (employees, locations, teams, security system actions) for thorough isolated testing during development
2. **Critical path tests should use real backend** - Key user flows (login → view systems → arm/disarm, admin CRUD operations) should have E2E tests that run against real docker-compose stack

This enables fast feedback during development (mock) while ensuring real integration works (real backend).

---

### Q17: Customer Roles in UI

**Q:** The UI needs customer roles (COMPANY_ROLE_ADMIN, SECURITY_SYSTEM_*) to determine tab visibility, but these are stored in Customer Service, not in the IAM JWT token. When should the UI fetch these roles?

**Options considered:**
- A. During OAuth callback (roles cached in session)
- B. On app mount (extra API call on page refresh)
- C. On tab navigation render (fetched every navigation)
- D. Lazy/on-demand (poor UX)

**A:** Option A with refresh on token refresh (A2):
- BFF calls `GET /me/roles` during `jwtCallback` when user logs in
- Customer roles stored in session alongside IAM roles
- Roles refreshed on token refresh (~2 min intervals) to pick up role changes
- UI accesses via `session.customerRoles` and `session.hasSecuritySystemAccess`

---

### Q16: Classification

**Classification: A. User-facing feature**

**Rationale:** This is a user-facing feature because:
- Primary goal is improving the end-user experience (UI/UX)
- Adds visible functionality for two user personas (admin and non-admin employees)
- Enables real user workflows: managing employees/teams/locations, controlling security systems
- Not an architectural proof-of-concept, infrastructure capability, or educational example
- The backend changes (REST APIs, init-db-service) exist to support the user-facing functionality

---

## Summary of Decisions

| Topic | Decision |
|-------|----------|
| Terminology | Use existing "Customer" (not "Company") |
| Init-DB service | Separate Docker service calling REST APIs; disable existing DBInitializers via Spring profile |
| Admin credentials | `admin@acme.com` / `password` |
| IAM integration | Use existing `UserService` to register users in IAM service |
| UI scope | Full CRUD for employees, teams, locations |
| UI framework | shadcn/ui component library; migrate existing styled-jsx components |
| Navigation | Top tabs (Employees, Teams, Locations, Security Systems) |
| Role-based UI | Tabs filtered by roles (admin sees all; non-admin sees Security Systems only) |
| Team role assignment | From Team detail page (Team → add location role) |
| Seed data | Keep existing DBInitializer data (1 customer, 1 admin, 1 employee, 3 locations, 1 team, 3 security systems) |
| REST API approach | Build incrementally as UI features require |
| Security system actions | Implement arm/disarm/acknowledge in UI |
| Authorization | Backend handles user context from JWT; UI just calls APIs |
| Confirmation dialogs | Only for delete operations; not for arm/disarm |
| Testing | Comprehensive mock server + critical path tests against real backend |
| Customer roles in UI | Fetch via `GET /me/roles` during OAuth callback; refresh on token refresh (~2 min) |

