#!/bin/bash

# Jaeger Multi-Service Trace Report
# Queries Jaeger API and reports on traces that span multiple services

JAEGER_URL="${JAEGER_URL:-http://localhost:16686}"
LOOKBACK="${LOOKBACK:-1h}"
LIMIT="${LIMIT:-100}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================"
echo "  Jaeger Multi-Service Trace Report"
echo "========================================"
echo ""
echo "Jaeger URL: $JAEGER_URL"
echo "Lookback: $LOOKBACK"
echo "Limit: $LIMIT traces per service"
echo ""

# Get list of services
services=$(curl -s "$JAEGER_URL/api/services" | jq -r '.data[]' | grep -v jaeger | sort)

if [ -z "$services" ]; then
    echo -e "${RED}Error: No services found. Is Jaeger running?${NC}"
    exit 1
fi

echo "Services reporting to Jaeger:"
echo "$services" | while read -r svc; do
    echo "  - $svc"
done
echo ""

# Collect all traces from all services
echo "Fetching traces..."
all_traces=""

for service in $services; do
    traces=$(curl -s "$JAEGER_URL/api/traces?service=$service&limit=$LIMIT&lookback=$LOOKBACK" 2>/dev/null)
    if [ -n "$traces" ]; then
        all_traces="$all_traces $traces"
    fi
done

# Process traces and find multi-service ones
echo ""
echo "========================================"
echo "  Multi-Service Traces"
echo "========================================"
echo ""

# Use a temp file to collect unique trace data
temp_file=$(mktemp)

for service in $services; do
    curl -s "$JAEGER_URL/api/traces?service=$service&limit=$LIMIT&lookback=$LOOKBACK" 2>/dev/null | \
    jq -r '.data[] | {
        traceID: .traceID,
        spanCount: (.spans | length),
        services: ([.processes[].serviceName] | unique | sort),
        serviceCount: ([.processes[].serviceName] | unique | length),
        duration: ((.spans | map(.duration) | max) / 1000),
        startTime: (.spans | map(.startTime) | min),
        httpOperation: ((.spans | map(select(.operationName | test("^http ";"i"))) | sort_by(.startTime) | first | .operationName) // "unknown")
    } | select(.serviceCount > 1) | @json' >> "$temp_file"
done

# Deduplicate and sort by service count
if [ -s "$temp_file" ]; then
    sort -u "$temp_file" | jq -s 'sort_by(-.serviceCount, -.spanCount)' | jq -r '.[] |
        "Trace: \(.traceID)\n  HTTP: \(.httpOperation)\n  Services (\(.serviceCount)): \(.services | join(", "))\n  Spans: \(.spanCount)\n  Duration: \(.duration | floor)ms\n"'

    # Summary statistics
    echo ""
    echo "========================================"
    echo "  Summary"
    echo "========================================"

    total_multi=$(sort -u "$temp_file" | wc -l | tr -d ' ')
    echo "Total multi-service traces: $total_multi"

    # Count by number of services
    echo ""
    echo "Traces by service count:"
    sort -u "$temp_file" | jq -r '.serviceCount' | sort | uniq -c | while read -r count num; do
        echo "  $num services: $count traces"
    done

    # Most common service combinations with HTTP operation and example trace link
    echo ""
    echo "Most common request patterns:"
    echo ""

    # Group by pattern and get example traceID for each
    sort -u "$temp_file" | jq -s '
        group_by(.httpOperation + " => " + (.services | join(" -> "))) |
        map({
            pattern: (.[0].httpOperation + " => " + (.[0].services | join(" -> "))),
            count: length,
            exampleTraceID: .[0].traceID,
            serviceCount: .[0].serviceCount
        }) |
        sort_by(-.count)
    ' | jq -r --arg url "$JAEGER_URL" '.[:10][] |
        "  (\(.count)) \(.pattern)\n       Example: \($url)/trace/\(.exampleTraceID)\n"'

else
    echo -e "${YELLOW}No multi-service traces found in the last $LOOKBACK${NC}"
fi

rm -f "$temp_file"

echo ""
echo "========================================"
echo "  View in Jaeger UI"
echo "========================================"
echo ""
echo "Open: $JAEGER_URL"
echo ""
