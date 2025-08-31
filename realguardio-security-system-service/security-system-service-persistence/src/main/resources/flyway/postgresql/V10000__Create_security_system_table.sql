CREATE TABLE security_system (
    id BIGSERIAL PRIMARY KEY,
    location_name VARCHAR(255),
    state VARCHAR(50),
    location_id BIGINT,
    rejection_reason VARCHAR(255),
    version BIGINT
);

CREATE TABLE security_system_actions (
    security_system_id BIGINT NOT NULL,
    actions VARCHAR(50),
    FOREIGN KEY (security_system_id) REFERENCES security_system(id)
);