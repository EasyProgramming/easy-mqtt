#!/bin/sh

export BASE_DIR
BASE_DIR=$(dirname $0)/..
PID_FILE="${BASE_DIR}/bin/pid"
PID=$(cat $PID_FILE)

if [ -z "${PID}" ] || [ -z "$(ps -ef | grep ${PID} | grep 'java')" ]; then
  echo "no easy mqtt server running."
  exit 1
fi

echo "easy mqtt server(${PID}) is running..."

kill ${PID}

echo "send shutdown request to easy mqtt server(${PID}) OK"
