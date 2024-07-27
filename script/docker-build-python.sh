#!/bin/sh

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

docker build "$@" -t 'ldbc/python' script/
