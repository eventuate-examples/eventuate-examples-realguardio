#! /bin/bash -e

if [ -z "$(docker ps -q -f ancestor=public.ecr.aws/osohq/dev-server)" ]; then
  docker run -d --rm -p :8080 -v "$PWD/policies:/policies" \
      public.ecr.aws/osohq/dev-server:latest --watch-for-changes /policies/main.polar
else
  echo OSO dev server is already running
fi