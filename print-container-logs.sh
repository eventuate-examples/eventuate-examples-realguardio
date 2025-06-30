#! /bin/bash -e

mkdir -p logs

docker ps -a > logs/containers.txt

for name in $(docker ps -a --format "{{.Names}}") ; do
  docker logs "$name" > "logs/${name}.log"
done
