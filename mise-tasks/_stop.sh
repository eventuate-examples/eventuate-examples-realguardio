#! /bin/bash -e

DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ROOT_DIR="${DIR}/.."

NAME=${1?}

if [ -f ${ROOT_DIR}/pids/${NAME}.pid ]; then
  echo killing $(cat ${ROOT_DIR}/pids/${NAME}.pid)
  GID="$(ps -o pgid= -p $(cat ${ROOT_DIR}/pids/${NAME}.pid) | tr -d ' ')"
  
  if [ -n "$GID" ]; then
    echo pkilling GID="$GID"
    pkill -g "$GID"
  fi
fi

