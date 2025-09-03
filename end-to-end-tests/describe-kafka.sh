#!/bin/bash

# Get the Kafka container name
KAFKA_CONTAINER=$(docker ps --format "table {{.Names}}" | grep -E "kafka|broker" | head -1)

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "Error: No Kafka container found running"
    exit 1
fi

echo "Using Kafka container: $KAFKA_CONTAINER"
echo "=================================="
echo

# Describe topics
echo "KAFKA TOPICS:"
echo "-------------"
docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:9092 --list

echo
echo "TOPIC DETAILS:"
echo "--------------"
for topic in $(docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:9092 --list); do
    # Remove any carriage returns from topic names
    topic=$(echo $topic | tr -d '\r')
    if [ ! -z "$topic" ]; then
        echo "Topic: $topic"
        docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server localhost:9092 --describe --topic $topic
        echo
    fi
done

echo
echo "CONSUMER GROUPS:"
echo "----------------"
docker exec $KAFKA_CONTAINER kafka-consumer-groups --bootstrap-server localhost:9092 --list

echo
echo "CONSUMER GROUP DETAILS:"
echo "-----------------------"
for group in $(docker exec $KAFKA_CONTAINER kafka-consumer-groups --bootstrap-server localhost:9092 --list); do
    # Remove any carriage returns from group names
    group=$(echo $group | tr -d '\r')
    if [ ! -z "$group" ]; then
        echo "Consumer Group: $group"
        docker exec $KAFKA_CONTAINER kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group $group
        echo
    fi
done