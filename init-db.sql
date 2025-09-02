-- Create databases for each service
CREATE DATABASE securitysystem;
CREATE DATABASE customer;
CREATE DATABASE orchestration;

-- Create users for each service with replication permissions for Eventuate CDC
CREATE USER securitysystemuser WITH PASSWORD 'securitysystempass' REPLICATION;
CREATE USER customeruser WITH PASSWORD 'customerpass' REPLICATION;
CREATE USER orchestrationuser WITH PASSWORD 'orchestrationpass' REPLICATION;

-- Grant database permissions
GRANT ALL PRIVILEGES ON DATABASE securitysystem TO securitysystemuser;
GRANT ALL PRIVILEGES ON DATABASE customer TO customeruser;
GRANT ALL PRIVILEGES ON DATABASE orchestration TO orchestrationuser;

-- Create and configure schemas for securitysystem database
\c securitysystem
CREATE SCHEMA IF NOT EXISTS securitysystem_schema AUTHORIZATION securitysystemuser;
GRANT ALL ON SCHEMA securitysystem_schema TO securitysystemuser;
ALTER USER securitysystemuser SET search_path TO securitysystem_schema;

-- Create and configure schemas for customer database
\c customer
CREATE SCHEMA IF NOT EXISTS customer_schema AUTHORIZATION customeruser;
GRANT ALL ON SCHEMA customer_schema TO customeruser;
ALTER USER customeruser SET search_path TO customer_schema;

-- Create and configure schemas for orchestration database
\c orchestration
CREATE SCHEMA IF NOT EXISTS orchestration_schema AUTHORIZATION orchestrationuser;
GRANT ALL ON SCHEMA orchestration_schema TO orchestrationuser;
ALTER USER orchestrationuser SET search_path TO orchestration_schema;

