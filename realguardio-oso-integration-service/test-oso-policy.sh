#! /bin/bash -e

./start-oso-dev-server.sh

source ./set-oso-env.sh

oso-cloud test policies/main.polar $*
