-- Add new fields for saga support
ALTER TABLE security_system ADD COLUMN location_id BIGINT;

-- Add new states to the state enum (if using enum constraint)
-- Note: PostgreSQL doesn't directly support adding values to enum types in older versions
-- You may need to recreate the constraint or use a different approach depending on your DB

-- If state is just a VARCHAR column, no changes needed for new states
-- CREATION_PENDING and CREATION_FAILED will be stored as strings