#!/usr/bin/env bash
set -eux

# Limit memory (all values are KiB)
mem_total="$(awk '/MemTotal/ { print $2 }' /proc/meminfo)"
mem_limit="$((mem_total * 90 / 100))"
ulimit -v "$mem_limit"

mvn --version

timeout -s kill 1h bash -c '
scripts/fmt
scripts/build
'