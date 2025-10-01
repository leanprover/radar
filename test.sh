#!/usr/bin/env bash
set -eux

cat /proc/self/cgroup | grep -Po '(?<=^0::).*$'