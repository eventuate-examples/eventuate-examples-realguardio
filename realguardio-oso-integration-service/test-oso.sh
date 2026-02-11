#! /bin/bash -e

./start-oso-dev-server.sh

source ./set-oso-env.sh

echo "Waiting for OSO server to be ready at $OSO_URL..."
until curl -sf "$OSO_URL" > /dev/null; do
  echo "OSO server not ready yet, retrying in 2 seconds..."
  sleep 2
done
echo "OSO server is ready!"

curl "$OSO_URL"

oso-cloud test policies/main.polar

./create-facts.sh

./run-queries.sh

