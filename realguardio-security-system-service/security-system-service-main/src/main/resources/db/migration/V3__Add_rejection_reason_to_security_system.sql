-- Add rejection_reason field for failed saga handling
ALTER TABLE security_system ADD COLUMN rejection_reason VARCHAR(255);