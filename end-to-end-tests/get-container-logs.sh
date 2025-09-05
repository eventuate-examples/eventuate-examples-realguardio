#!/bin/bash

TEST_RESULTS_FILE="build/test-results/endToEndTest/TEST-com.realguardio.endtoendtests.RealGuardioEndToEndTest.xml"
OUTPUT_DIR="build/container-logs"

if [ ! -f "$TEST_RESULTS_FILE" ]; then
    echo "Test results file not found: $TEST_RESULTS_FILE"
    exit 1
fi

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

echo "Extracting container logs from test results..."

# Extract unique service names
SERVICES=$(grep -o "\[SVC [^:]*:\]" "$TEST_RESULTS_FILE" | sed 's/\[SVC //g' | sed 's/:\]//g' | sort -u)

if [ -z "$SERVICES" ]; then
    echo "No service logs found in the test results"
    exit 0
fi

# Extract logs for each service
for SERVICE in $SERVICES; do
    OUTPUT_FILE="$OUTPUT_DIR/$SERVICE.log"
    echo "Extracting logs for $SERVICE to $OUTPUT_FILE"
    
    # Extract lines containing this service's logs and remove all prefixes
    grep "\[SVC $SERVICE:\]" "$TEST_RESULTS_FILE" | \
        sed 's/^.*\[SVC [^:]*:\] STDOUT: //' | \
        sed 's/^.*\[SVC [^:]*:\] STDERR: //' > "$OUTPUT_FILE"
    
done

# Also extract non-service container logs (kafka, postgres, etc.)
echo ""
echo "Extracting other container logs..."

# Extract kafka logs
if grep -q "\[kafka:\]" "$TEST_RESULTS_FILE"; then
    echo "Extracting logs for kafka to $OUTPUT_DIR/kafka.log"
    grep "\[kafka:\]" "$TEST_RESULTS_FILE" | \
        sed 's/^.*\[kafka:\] STDOUT: //' | \
        sed 's/^.*\[kafka:\] STDERR: //' > "$OUTPUT_DIR/kafka.log"
fi

# Extract postgres logs
if grep -q "\[postgres:\]" "$TEST_RESULTS_FILE"; then
    echo "Extracting logs for postgres to $OUTPUT_DIR/postgres.log"
    grep "\[postgres:\]" "$TEST_RESULTS_FILE" | \
        sed 's/^.*\[postgres:\] STDOUT: //' | \
        sed 's/^.*\[postgres:\] STDERR: //' > "$OUTPUT_DIR/postgres.log"
fi

# Extract authorization-server logs
if grep -q "\[authorization-server:\]" "$TEST_RESULTS_FILE"; then
    echo "Extracting logs for authorization-server to $OUTPUT_DIR/authorization-server.log"
    grep "\[authorization-server:\]" "$TEST_RESULTS_FILE" | \
        sed 's/^.*\[authorization-server:\] STDOUT: //' | \
        sed 's/^.*\[authorization-server:\] STDERR: //' > "$OUTPUT_DIR/authorization-server.log"
fi

echo ""
echo "Log extraction complete!"
echo "Service logs have been saved to $OUTPUT_DIR/"