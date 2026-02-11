#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_DIR="$SCRIPT_DIR/realguardio-security-system-service"
DRY_RUN=false
SERVICE_NAME=""

usage() {
    echo "Usage: $0 [--dry-run] <service-name>"
    echo ""
    echo "Creates a new Realguardio service based on the security-system-service template."
    echo ""
    echo "Arguments:"
    echo "  service-name    Name of the service to create (e.g., customer-service)"
    echo ""
    echo "Options:"
    echo "  --dry-run       Show what would be done without creating files"
    echo ""
    echo "Example:"
    echo "  $0 customer-service"
    echo "  $0 --dry-run customer-service"
    exit 1
}

log() {
    echo "[$(date '+%H:%M:%S')] $1"
}

dry_run_log() {
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] $1"
    fi
}

error() {
    echo "ERROR: $1" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        -*)
            error "Unknown option: $1"
            ;;
        *)
            if [ -z "$SERVICE_NAME" ]; then
                SERVICE_NAME="$1"
            else
                error "Too many arguments"
            fi
            shift
            ;;
    esac
done

if [ -z "$SERVICE_NAME" ]; then
    error "Service name is required"
fi

if ! [[ "$SERVICE_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
    error "Service name must start with a lowercase letter and contain only lowercase letters, numbers, and hyphens"
fi

NEW_SERVICE_DIR="$SCRIPT_DIR/realguardio-$SERVICE_NAME"
# Extract entity name from service name (e.g., customer-service -> Customer)
# Use awk for better cross-platform compatibility
ENTITY_BASE=$(echo "$SERVICE_NAME" | sed 's/-service$//')
ENTITY_NAME=$(echo "$ENTITY_BASE" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2)} 1' OFS='')
ENTITY_NAME_LOWERCASE=$(echo "$ENTITY_BASE" | sed 's/-//g')
SERVICE_NAME_CAMELCASE=$(echo "$SERVICE_NAME" | awk -F'-' '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2)} 1' OFS='')
SERVICE_NAME_PACKAGE=$(echo "$SERVICE_NAME" | sed 's/-//g')
SERVICE_PORT=""  # Will be set by update_docker_compose

log "Creating new service: $SERVICE_NAME"
log "Target directory: $NEW_SERVICE_DIR"
log "Package name: $SERVICE_NAME_PACKAGE"
log "Entity name: $ENTITY_NAME"

if [ ! -d "$TEMPLATE_DIR" ]; then
    error "Template directory not found: $TEMPLATE_DIR"
fi

if [ -e "$NEW_SERVICE_DIR" ]; then
    error "Target directory already exists: $NEW_SERVICE_DIR"
fi

if [ "$DRY_RUN" = true ]; then
    log "Running in dry-run mode - no files will be created"
    echo ""
fi

copy_and_transform() {
    local src="$1"
    local dest="$2"
    local relative_path="${src#$TEMPLATE_DIR/}"
    
    dest=$(echo "$dest" | sed "s/security-system-service/$SERVICE_NAME/g")
    dest=$(echo "$dest" | sed "s/securitysystemservice/$SERVICE_NAME_PACKAGE/g")
    
    if [ -d "$src" ]; then
        if [ "$DRY_RUN" = true ]; then
            dry_run_log "Create directory: ${dest#$SCRIPT_DIR/}"
        else
            mkdir -p "$dest"
        fi
        
        for item in "$src"/*; do
            if [ -e "$item" ]; then
                local basename=$(basename "$item")
                if [[ ! "$basename" =~ ^(build|bin|\.gradle|\.idea|out|target|\.git)$ ]]; then
                    copy_and_transform "$item" "$dest/$basename"
                fi
            fi
        done
        
        for item in "$src"/.[^.]*; do
            if [ -e "$item" ]; then
                local basename=$(basename "$item")
                if [[ ! "$basename" =~ ^(\.gradle|\.idea|\.git)$ ]]; then
                    copy_and_transform "$item" "$dest/$basename"
                fi
            fi
        done
    elif [ -f "$src" ]; then
        local filename=$(basename "$src")
        
        case "$filename" in
            gradle-wrapper.jar)
                # Special case: copy gradle-wrapper.jar without transformation
                if [ "$DRY_RUN" = true ]; then
                    dry_run_log "Create file: ${dest#$SCRIPT_DIR/} (binary file)"
                else
                    mkdir -p "$(dirname "$dest")"
                    cp "$src" "$dest"
                fi
                return
                ;;
            *.jar|*.class|*.pyc|*.so|*.dylib|*.dll|*.exe)
                return
                ;;
            repomix-output.xml|CLAUDE.md|*.txt)
                return
                ;;
        esac
        
        # Rename files that contain SecuritySystem in their name
        local new_filename="$filename"
        new_filename=$(echo "$new_filename" | sed "s/SecuritySystemService/${SERVICE_NAME_CAMELCASE}/g")
        new_filename=$(echo "$new_filename" | sed "s/SecuritySystem/${ENTITY_NAME}/g")
        dest="$(dirname "$dest")/$new_filename"
        
        if [ "$DRY_RUN" = true ]; then
            dry_run_log "Create file: ${dest#$SCRIPT_DIR/}"
            
            if [[ "$filename" =~ \.(java|gradle|properties|xml|yaml|yml|json)$ ]] || [ "$filename" = "settings.gradle" ] || [[ "$filename" =~ ^Dockerfile ]]; then
                if grep -q "security-system-service\|securitysystemservice\|SecuritySystem\|security_system" "$src" 2>/dev/null; then
                    dry_run_log "  - Transform package/class/entity names in: ${dest#$SCRIPT_DIR/}"
                fi
            fi
        else
            mkdir -p "$(dirname "$dest")"
            
            if [[ "$filename" =~ \.(java|gradle|properties|xml|yaml|yml|json)$ ]] || [ "$filename" = "settings.gradle" ] || [[ "$filename" =~ ^Dockerfile ]]; then
                sed -e "s/security-system-service/$SERVICE_NAME/g" \
                    -e "s/securitysystemservice/$SERVICE_NAME_PACKAGE/g" \
                    -e "s/SecuritySystemService/${SERVICE_NAME_CAMELCASE}/g" \
                    -e "s/SecuritySystemAction/${ENTITY_NAME}Action/g" \
                    -e "s/SecuritySystemState/${ENTITY_NAME}State/g" \
                    -e "s/SecuritySystemRepository/${ENTITY_NAME}Repository/g" \
                    -e "s/SecuritySystems/${ENTITY_NAME}s/g" \
                    -e "s/SecuritySystem/${ENTITY_NAME}/g" \
                    -e "s/securitySystem/${ENTITY_NAME_LOWERCASE}/g" \
                    -e "s/security_system/${ENTITY_NAME_LOWERCASE}/g" \
                    -e "s/SECURITY_SYSTEM/$(echo $ENTITY_NAME | tr '[:lower:]' '[:upper:]')/g" \
                    "$src" > "$dest"
            else
                cp "$src" "$dest"
            fi
        fi
    fi
}

if [ "$DRY_RUN" = true ]; then
    echo "=== DRY RUN MODE ==="
    echo ""
    echo "The following actions would be performed:"
    echo ""
fi

update_docker_compose() {
    local docker_compose_file="$SCRIPT_DIR/docker-compose.yaml"
    
    if [ ! -f "$docker_compose_file" ]; then
        log "Warning: docker-compose.yaml not found, skipping docker-compose update"
        return
    fi
    
    # Check if yq is installed
    if ! command -v yq &> /dev/null; then
        log "Warning: yq is not installed. Install it with: brew install yq"
        log "Skipping docker-compose.yaml update"
        return
    fi
    
    # Check if service already exists in docker-compose
    if yq eval ".services | has(\"realguardio-$SERVICE_NAME\")" "$docker_compose_file" | grep -q true; then
        log "Service realguardio-$SERVICE_NAME already exists in docker-compose.yaml"
        return
    fi
    
    if [ "$DRY_RUN" = true ]; then
        dry_run_log "Update docker-compose.yaml to add realguardio-$SERVICE_NAME service"
        return
    fi
    
    log "Updating docker-compose.yaml..."
    
    # Find the next available port (starting from 3002)
    SERVICE_PORT=3002
    while yq eval ".services.* | select(.ports) | .ports[]" "$docker_compose_file" | grep -q "\"$SERVICE_PORT:"; do
        ((SERVICE_PORT++))
    done
    
    # Create a temporary file for the new service configuration
    local temp_service_file=$(mktemp)
    
    # Extract and transform the security-system-service configuration
    # Note: Keep the database URL pointing to 'securitysystem' database (shared database)
    yq eval '.services."realguardio-security-system-service"' "$docker_compose_file" | \
        sed "s/security-system-service/$SERVICE_NAME/g" | \
        sed "s/3001:3001/$SERVICE_PORT:3001/g" > "$temp_service_file"
    
    # The database URL should remain: jdbc:postgresql://postgres:5432/securitysystem
    # Don't transform the database name since all services share the same database
    
    # Add the new service to docker-compose.yaml
    yq eval ".services.\"realguardio-$SERVICE_NAME\" = load(\"$temp_service_file\")" -i "$docker_compose_file"
    
    # Clean up
    rm "$temp_service_file"
    
    log "docker-compose.yaml updated successfully with realguardio-$SERVICE_NAME on port $SERVICE_PORT"
}

copy_and_transform "$TEMPLATE_DIR" "$NEW_SERVICE_DIR"
update_docker_compose

if [ "$DRY_RUN" = true ]; then
    echo ""
    echo "=== Summary ==="
    echo "Service name: $SERVICE_NAME"
    echo "Directory: $NEW_SERVICE_DIR"
    echo "Package: $SERVICE_NAME_PACKAGE"
    echo ""
    echo "To actually create the service, run without --dry-run:"
    echo "  $0 $SERVICE_NAME"
else
    log "Service created successfully!"
    log "Running compileAll to verify the service compiles..."
    
    cd "$NEW_SERVICE_DIR"
    if ./gradlew compileAll; then
        log "Compilation successful!"
        
        # Build the bootJar for the main module
        log "Building bootJar for $SERVICE_NAME-main..."
        if ./gradlew :$SERVICE_NAME-main:bootJar; then
            log "BootJar built successfully!"
        else
            error "Failed to build bootJar. Check the build output for errors."
        fi
        
        # Try to start the service with docker compose
        log "Starting the new service with docker compose..."
        cd "$SCRIPT_DIR"
        if mise run compose-up 2>&1 | tee /tmp/compose-up.log; then
            # Check if the new service started successfully
            if docker compose ps | grep -q "realguardio-$SERVICE_NAME.*running"; then
                log "✅ Service realguardio-$SERVICE_NAME is running successfully!"
                echo ""
                echo "✅ Service created, compiled, and started successfully!"
                echo ""
                echo "Service is now running on port $SERVICE_PORT"
                echo ""
                echo "Next steps:"
                echo "1. Check service health: curl http://localhost:$SERVICE_PORT/actuator/health"
                echo "2. View logs: docker compose logs realguardio-$SERVICE_NAME"
                echo "3. Stop service: docker compose stop realguardio-$SERVICE_NAME"
            else
                log "Warning: Service realguardio-$SERVICE_NAME may not have started properly"
                log "Check logs with: docker compose logs realguardio-$SERVICE_NAME"
            fi
        else
            log "Warning: docker compose up failed. Check /tmp/compose-up.log for details"
        fi
    else
        error "Compilation failed. Please check the generated code for errors."
    fi
fi