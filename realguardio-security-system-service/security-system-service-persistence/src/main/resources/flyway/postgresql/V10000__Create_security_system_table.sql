CREATE TABLE security_system (
    id BIGSERIAL PRIMARY KEY,
    location_name VARCHAR(255),
    state VARCHAR(50),
    location_id BIGINT,
    rejection_reason VARCHAR(255),
    version BIGINT
);

