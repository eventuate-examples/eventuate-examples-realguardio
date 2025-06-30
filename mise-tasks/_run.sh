#! /bin/bash -e

DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ROOT_DIR="${DIR}/.."

NAME=${1?}
shift

mkdir -p ${ROOT_DIR}/logs ${ROOT_DIR}/pids

mise ${NAME}-stop

set -m
"$@" > ${ROOT_DIR}/logs/${NAME}.log 2>&1 &

echo $! > ${ROOT_DIR}/pids/${NAME}.pid
set +m

echo new PID $(cat ${ROOT_DIR}/pids/${NAME}.pid)

mise run ${NAME}-wait
