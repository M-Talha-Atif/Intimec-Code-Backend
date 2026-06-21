#!/bin/bash
set -e
echo 'Waiting for OpenSearch...';
# Wait for OpenSearch to be ready
until curl -s -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" "$OPENSEARCH_URL/_cluster/health" | grep -q '"status":"green"\|"status":"yellow"'; do
  echo "Waiting for OpenSearch to be ready..."
  sleep 1
done;
echo 'OpenSearch is ready!';
echo 'Pushing template...'; 
curl -X PUT $OPENSEARCH_URL/_index_template/log-template -H 'Content-Type: application/json' -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD" -d @/index-template.json


# Explicitly create the main-logs index
echo 'Creating main-logs index...';
curl -X PUT "$OPENSEARCH_URL/main-logs" \
  -H 'Content-Type: application/json' \
  -u "$OPENSEARCH_USERNAME:$OPENSEARCH_PASSWORD"

touch /tmp/ready;