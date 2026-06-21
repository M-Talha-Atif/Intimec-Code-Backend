#!/bin/bash

set -e

echo "Creating backend Docker network (if not exists)..."
docker network create dokploy-network 2>/dev/null || true

echo "Stopping existing Kafka (bootstrap) containers..."
docker-compose -f infrastructure/docker/docker-compose-bootstrap.yml down

echo "Starting Kafka (bootstrap) services..."
docker-compose -f infrastructure/docker/docker-compose-bootstrap.yml up -d

echo "Waiting for Kafka to be ready..."
until docker exec kafka bash -c "echo > /dev/tcp/localhost/9092" 2>/dev/null; do
  sleep 2
  echo "Waiting for Kafka..."
done

echo "Kafka started"