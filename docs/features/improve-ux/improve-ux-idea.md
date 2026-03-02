# Improve UX - Refined Idea

## Overview

Modernize the RealGuardIO UI from a basic placeholder to a fully functional admin and security system management interface.

## Goals

### 1. Init-DB Service for Demo Data

- Add a new Docker service (`init-db-service`) that seeds the database via REST API calls
- Creates: Customer (Acme Corporation), admin user (`admin@acme.com`/`password`), locations, teams, employees, and security systems
- Disable existing `DBInitializer` classes via Spring profile when using init container
- Users are registered in IAM service via existing `UserService` infrastructure

### 2. Admin Management (COMPANY_ROLE_ADMIN users)

Company admins can:

- **Manage Employees**: Create, view, update, delete customer employees; assign roles at locations
- **Manage Teams**: Create, view, update, delete teams; add/remove team members; assign location roles to teams
- **Manage Locations**: Create, view, update, delete locations; assign security systems to locations

### 3. Security System Control (SECURITY_SYSTEM_* roles)

Non-admin employees can:

- View accessible security systems (filtered by their location-specific roles)
- Perform actions based on permissions:
  - ARM (requires SECURITY_SYSTEM_ARMER role at location)
  - DISARM (requires SECURITY_SYSTEM_DISARMER role at location)
  - ACKNOWLEDGE alarms (requires SECURITY_SYSTEM_DISARMER role at location)
  - VIEW (requires any SECURITY_SYSTEM_* role at location)

### 4. UI Modernization

- Adopt shadcn/ui component library (built on Tailwind CSS)
- Migrate existing styled-jsx components to new framework
- Top tab navigation: Employees | Teams | Locations | Security Systems
- Role-filtered tabs (admins see all; non-admins see Security Systems only)
- Confirmation dialogs for delete operations
- Toast notifications for action feedback

## Technical Approach

- **Incremental development**: Build UI features and supporting REST APIs together, one at a time
- **Testing**: Comprehensive mock server for development; critical path E2E tests against real docker-compose stack
- **Authorization**: Backend REST APIs handle user context from JWT; UI simply calls APIs and receives authorized data

## Seed Data (from existing DBInitializer)

- 1 Customer: Acme Corporation
- 1 Admin: admin@acme.com (COMPANY_ROLE_ADMIN)
- 1 Non-admin: john.doe@acme.com (SECURITY_SYSTEM_DISARMER at Oakland via team)
- 3 Locations: Oakland office, Berkeley office, Hayward office
- 1 Team: Security Team
- 3 Security Systems: One per location (ARMED, DISARMED, ALARMED states)

## Passwords

All user passwords hardcoded to `password` for demo purposes.
