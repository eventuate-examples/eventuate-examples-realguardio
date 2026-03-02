#! /bin/bash -e

./start-oso-dev-server.sh

# shellcheck source=./set-oso-env.sh
source ./set-oso-env.sh

oso-cloud test policies/main.polar "$@"
