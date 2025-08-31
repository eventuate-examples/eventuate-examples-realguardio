#!/bin/bash

# Extract stack traces from JUnit XML test results
# Usage: ./extract-traces.sh

XML_FILE="customer-service-main/build/test-results/componentTest/TEST-io.eventuate.examples.realguardio.customerservice.CustomerServiceComponentTest.xml"

if [ ! -f "$XML_FILE" ]; then
    echo "Error: Test results file not found: $XML_FILE"
    exit 1
fi

echo "Extracting information from: $XML_FILE"
echo "================================================"
echo

# Extract system-out content with three criteria:
# (a) Stack traces from [SVC customer-service:] STDOUT
# (b) Any log entries containing customerCommandDispatcher  
# (c) Any log entries containing CustomerServiceComponentTest but NOT [SVC customer-service:]
xmllint --xpath "//system-out/text()" "$XML_FILE" 2>/dev/null | \
{
    in_trace=false
    prev_was_trace=false
    while IFS= read -r line; do
        include_line=false
        
        # Check criteria (a): Stack traces from [SVC customer-service:] STDOUT
        if [[ "$line" =~ \[SVC\ customer-service:\]\ STDOUT: ]] && [[ "$line" =~ (Exception|Error|at\ |Caused\ by:|Suppressed:) ]]; then
            include_line=true
            # Check if this starts a new exception
            if [[ "$line" =~ (Exception|Error): ]] && [[ ! "$line" =~ at\  ]]; then
                # If we had a previous trace, add separator
                if [ "$prev_was_trace" = true ]; then
                    echo "========================="
                    echo
                fi
                in_trace=true
                prev_was_trace=true
            fi
            # Clean up and output the stack trace line
            cleaned_line=$(echo "$line" | sed -E 's/^.*\[SVC customer-service:\] STDOUT: //')
            echo "$cleaned_line"
            
            # Check if this might be the end of a trace
            if [[ "$line" =~ \.\.\.\ [0-9]+\ (more|common\ frames\ omitted) ]]; then
                in_trace=false
            fi
        # Check criteria (b): Any log with customerCommandDispatcher
        elif [[ "$line" =~ (CustomerCommandHandler|customerCommandDispatcher) ]] && [ "$in_trace" = false ]; then
            # Add separator if we just finished a stack trace
            if [ "$prev_was_trace" = true ]; then
                echo "========================="
                echo
                prev_was_trace=false
            fi
            # Clean and output the message
            if [[ "$line" =~ \[customer-service\] ]]; then
                cleaned_line=$(echo "$line" | sed -E 's/^.*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+Z)/\1/')
            else
                cleaned_line=$(echo "$line" | sed -E 's/^.*\[SVC customer-service:\] STDOUT: //')
            fi
            echo "$cleaned_line"
        # Check criteria (c): CustomerServiceComponentTest but NOT [SVC customer-service:]
        elif [[ "$line" =~ CustomerServiceComponentTest ]] && [[ ! "$line" =~ \[SVC\ customer-service:\] ]] && [ "$in_trace" = false ]; then
            # Add separator if we just finished a stack trace
            if [ "$prev_was_trace" = true ]; then
                echo "========================="
                echo
                prev_was_trace=false
            fi
            # Clean and output the message - extract timestamp and message
            cleaned_line=$(echo "$line" | sed -E 's/^.*([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]+Z)/\1/')
            echo "$cleaned_line"
        fi
    done
    # Final separator if we ended with a trace
    if [ "$prev_was_trace" = true ]; then
        echo "========================="
    fi
}

echo
echo "================================================"
echo "Stack trace extraction complete"