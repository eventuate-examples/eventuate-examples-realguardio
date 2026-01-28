CREATE TABLE customer_employee_location_role (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    location_id BIGINT NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_employee_location_role_user_name ON customer_employee_location_role(user_name);
CREATE INDEX idx_customer_employee_location_role_location_id ON customer_employee_location_role(location_id);