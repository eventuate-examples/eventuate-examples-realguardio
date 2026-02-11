#!/bin/bash

# Get the Kafka container name - look for apache/kafka-native image or containers with kafka in the name
KAFKA_CONTAINER=$(docker ps --format "{{.Names}}" --filter "ancestor=apache/kafka-native:3.8.0" | head -1)

# If not found by image, try by name pattern
if [ -z "$KAFKA_CONTAINER" ]; then
    KAFKA_CONTAINER=$(docker ps --format "{{.Names}}" | grep -E "kafka|broker" | head -1)
fi

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "Error: No Kafka container found running"
    exit 1
fi

echo "Using Kafka container: $KAFKA_CONTAINER"

# Get the network that Kafka is connected to
KAFKA_NETWORK=$(docker inspect $KAFKA_CONTAINER --format '{{range $net, $conf := .NetworkSettings.Networks}}{{$net}}{{end}}' | head -1)

if [ -z "$KAFKA_NETWORK" ]; then
    echo "Error: Could not determine Kafka container network"
    exit 1
fi

echo "Kafka is on network: $KAFKA_NETWORK"

# Get Kafka hostname/alias from the network
KAFKA_HOSTNAME=$(docker inspect $KAFKA_CONTAINER --format "{{range .NetworkSettings.Networks}}{{index .Aliases 1}}{{end}}")
if [ -z "$KAFKA_HOSTNAME" ]; then
    KAFKA_HOSTNAME=$(docker inspect $KAFKA_CONTAINER --format "{{range .NetworkSettings.Networks}}{{index .Aliases 0}}{{end}}")
fi

if [ -z "$KAFKA_HOSTNAME" ]; then
    # Fallback to container name
    KAFKA_HOSTNAME=$KAFKA_CONTAINER
fi

echo "Kafka hostname in network: $KAFKA_HOSTNAME"
echo "=================================="
echo

# Use a Kafka image with admin tools to inspect the broker
KAFKA_TOOLS_IMAGE="confluentinc/cp-kafka:latest"

echo "KAFKA TOPICS:"
echo "-------------"
docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
    kafka-topics --bootstrap-server $KAFKA_HOSTNAME:9093 --list

echo
echo "TOPIC DETAILS:"
echo "--------------"
TOPICS=$(docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
    kafka-topics --bootstrap-server $KAFKA_HOSTNAME:9093 --list 2>/dev/null)

if [ ! -z "$TOPICS" ]; then
    for topic in $TOPICS; do
        # Remove any carriage returns from topic names
        topic=$(echo $topic | tr -d '\r')
        if [ ! -z "$topic" ]; then
            echo "Topic: $topic"
            docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
                kafka-topics --bootstrap-server $KAFKA_HOSTNAME:9093 --describe --topic $topic
            echo
        fi
    done
else
    echo "No topics to describe"
fi

echo
echo "CONSUMER GROUPS:"
echo "----------------"
docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
    kafka-consumer-groups --bootstrap-server $KAFKA_HOSTNAME:9093 --list

echo
echo "CONSUMER GROUP DETAILS:"
echo "-----------------------"
GROUPS=$(docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
    kafka-consumer-groups --bootstrap-server $KAFKA_HOSTNAME:9093 --list 2>/dev/null | grep -v '^[[:space:]]*$')

if [ ! -z "$GROUPS" ]; then
    echo "$GROUPS" | while IFS= read -r group; do
        # Remove any carriage returns and whitespace from group names
        group=$(echo "$group" | tr -d '\r' | xargs)
        if [ ! -z "$group" ] && [ "$group" != "" ]; then
            echo "Consumer Group: $group"
            docker run --rm --network $KAFKA_NETWORK $KAFKA_TOOLS_IMAGE \
                kafka-consumer-groups --bootstrap-server $KAFKA_HOSTNAME:9093 --describe --group "$group"
            echo
        fi
    done
else
    echo "No consumer groups to describe"
fi