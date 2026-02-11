-- Database schema for Oso local authorization
-- This schema supports the authorization model defined in main.polar

-- Table: customer_employee_customer_roles
-- Stores role assignments for CustomerEmployees at the Customer (organization) level
-- Example: Alice has SECURITY_SYSTEM_DISARMER role for Customer "acme"
CREATE TABLE IF NOT EXISTS customer_employee_customer_roles (
    customer_employee_id VARCHAR(255) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (customer_employee_id, role_name, customer_id)
);


-- Table: team_location_roles
-- Stores role assignments for Teams at the Location level
-- Example: Team "ops-t1" has SECURITY_SYSTEM_DISARMER role for Location "loc1"
CREATE TABLE IF NOT EXISTS team_location_roles (
    team_id VARCHAR(255) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    location_id BIGINT NOT NULL,
    PRIMARY KEY (team_id, role_name, location_id)
);

-- Table: team_members
-- Stores Team membership relationships
-- Example: Team "ops-t1" has member Charlie
CREATE TABLE IF NOT EXISTS team_members (
    team_id VARCHAR(255) NOT NULL,
    customer_employee_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (team_id, customer_employee_id)
);

-- Table: locations
-- Stores Locations and their relationship to Customers
-- Example: Location "loc1" belongs to Customer "acme"
CREATE TABLE IF NOT EXISTS locations (
    id BIGINT PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL
);


-- Sample data matching create-facts.sh examples
-- Uncomment to insert test data:

-- INSERT INTO customer_employee_customer_roles (customer_employee_id, role_name, customer_id) VALUES
--     ('alice', 'SECURITY_SYSTEM_DISARMER', 'acme'),
--     ('bob', 'SECURITY_SYSTEM_DISARMER', 'foo');

-- INSERT INTO customer_employee_location_roles (customer_employee_id, role_name, location_id) VALUES
--     ('mary', 'SECURITY_SYSTEM_DISARMER', 3);

-- INSERT INTO team_location_roles (team_id, role_name, location_id) VALUES
--     ('ops-t1', 'SECURITY_SYSTEM_DISARMER', 1);

-- INSERT INTO team_members (team_id, customer_employee_id) VALUES
--     ('ops-t1', 'charlie');

-- INSERT INTO locations (id, customer_id) VALUES
--     (1, 'acme'),
--     (2, 'foo'),
--     (3, 'acme');

-- INSERT INTO security_systems (id, location_id) VALUES
--     (1, 1),
--     (2, 2),
--     (3, 3);
