#!/usr/bin/env bash
set -eux

systemd-run --user --scope -- echo hi

## Compute memory limit in bytes
#mem_total="$(awk '/MemTotal/ { print $2 * 1024 }' /proc/meminfo)"
#mem_limit="$((mem_total * 90 / 100))"
#
## Create cgroup
#cg_main="/sys/fs/cgroup$(awk -F '::' '{ print $2 }' /proc/self/cgroup)"
#cg_bench="$(dirname "$cg_main")/bench"
#mkdir "$cg_bench"
#echo "$mem_limit" > "$cg_bench/memory.max"
#
#(
#  # Run something in the newly created cgroup
#  echo $$ > "$cg_bench/cgroup.procs"
#  echo "subshell cgroup is:"
#  cat /proc/self/cgroup
#)
