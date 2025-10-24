#! /bin/bash -e

set -e
set -o pipefail

OSO_PORT=$(docker port $(docker ps -q -f ancestor=public.ecr.aws/osohq/dev-server) 8080 | cut -d: -f2)
export OSO_URL="http://localhost:${OSO_PORT}"
export OSO_AUTH="e_0123456789_12345_osotesttoken01xiIn"
