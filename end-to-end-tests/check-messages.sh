#!/bin/bash

# Find postgres containers by their network aliases
CUSTOMER_DB_CONTAINER=$(docker ps --format "{{.Names}}" --filter "ancestor=eventuateio/eventuate-vanilla-postgres:0.21.0.BUILD-SNAPSHOT" | xargs -I {} sh -c 'docker inspect {} | grep -q "customer-service-db" && echo {}' | head -1)
ORCHESTRATION_DB_CONTAINER=$(docker ps --format "{{.Names}}" --filter "ancestor=eventuateio/eventuate-vanilla-postgres:0.21.0.BUILD-SNAPSHOT" | xargs -I {} sh -c 'docker inspect {} | grep -q "orchestration-service-db" && echo {}' | head -1)
SECURITY_DB_CONTAINER=$(docker ps --format "{{.Names}}" --filter "ancestor=eventuateio/eventuate-vanilla-postgres:0.21.0.BUILD-SNAPSHOT" | xargs -I {} sh -c 'docker inspect {} | grep -q "security-system-service-db" && echo {}' | head -1)

if [ -z "$CUSTOMER_DB_CONTAINER" ] || [ -z "$ORCHESTRATION_DB_CONTAINER" ] || [ -z "$SECURITY_DB_CONTAINER" ]; then
    echo "Error: Could not find all required postgres containers"
    echo "Customer DB: $CUSTOMER_DB_CONTAINER"
    echo "Orchestration DB: $ORCHESTRATION_DB_CONTAINER"
    echo "Security DB: $SECURITY_DB_CONTAINER"
    exit 1
fi

echo "Using postgres containers:"
echo "  Customer DB: $CUSTOMER_DB_CONTAINER"
echo "  Orchestration DB: $ORCHESTRATION_DB_CONTAINER"
echo "  Security DB: $SECURITY_DB_CONTAINER"
echo "================================================"
echo

# Function to run psql command
run_query() {
    local container=$1
    local user=$2
    local db=$3
    local query=$4
    docker exec -i "$container" psql -U "$user" -d "$db" -c "$query"
}

# Function to format and display results for a database
show_database_messages() {
    local container=$1
    local db_name=$2
    local user=$3
    local db_label=$4
    
    echo "=== $db_label DATABASE ==="
    echo "-----------------------------------------"
    
    echo "Messages:"
    run_query "$container" "$user" "$db_name" "SELECT id, destination, 
           left(payload, 100) as payload_preview,
           published, creation_time 
           FROM public.message 
           ORDER BY creation_time DESC 
           LIMIT 10;" 2>/dev/null || \
    run_query "$container" "$user" "$db_name" "SELECT * FROM public.message ORDER BY id DESC LIMIT 10;"
    
    echo
}

# Check messages in each database
show_database_messages "$CUSTOMER_DB_CONTAINER" "eventuate" "postgresuser" "CUSTOMER"
show_database_messages "$ORCHESTRATION_DB_CONTAINER" "eventuate" "postgresuser" "ORCHESTRATION"
show_database_messages "$SECURITY_DB_CONTAINER" "eventuate" "postgresuser" "SECURITY SYSTEM"

# Show saga instances if they exist
echo "=== SAGA INSTANCES ==="
echo "-----------------------------------------"
run_query "$ORCHESTRATION_DB_CONTAINER" "postgresuser" "eventuate" "SELECT 
    saga_type,
    saga_id,
    state_name::json->>'currentlyExecuting' as currently_executing,
    state_name::json->>'compensating' as compensating,
    state_name::json->>'endState' as end_state,
    state_name::json->>'failed' as failed,
    saga_data_json
    FROM public.saga_instance;"

echo
echo "=== CUSTOMERS AND LOCATIONS ==="
echo "-----------------------------------------"
run_query "$CUSTOMER_DB_CONTAINER" "postgresuser" "eventuate" "SELECT 
    c.id as customer_id,
    c.name as customer_name,
    c.organization_id,
    l.id as location_id,
    l.name as location_name,
    l.security_system_id
    FROM public.customers c
    LEFT JOIN public.locations l ON c.id = l.customer_id
    ORDER BY c.id, l.id;"

echo
echo "=== SECURITY SYSTEMS ==="
echo "-----------------------------------------"
run_query "$SECURITY_DB_CONTAINER" "postgresuser" "eventuate" "SELECT 
    id as security_system_id,
    location_id,
    location_name,
    state,
    rejection_reason,
    version
    FROM public.security_system
    ORDER BY id DESC;"

echo
echo "=== MESSAGE COUNTS ==="
echo "-----------------------------------------"
echo -n "Customer messages: "
run_query "$CUSTOMER_DB_CONTAINER" "postgresuser" "eventuate" "SELECT COUNT(*) FROM public.message;" 2>/dev/null | grep -E '[0-9]+' | head -1 || echo "0"

echo -n "Orchestration messages: "
run_query "$ORCHESTRATION_DB_CONTAINER" "postgresuser" "eventuate" "SELECT COUNT(*) FROM public.message;" 2>/dev/null | grep -E '[0-9]+' | head -1 || echo "0"

echo -n "Security System messages: "
run_query "$SECURITY_DB_CONTAINER" "postgresuser" "eventuate" "SELECT COUNT(*) FROM public.message;" 2>/dev/null | grep -E '[0-9]+' | head -1 || echo "0"