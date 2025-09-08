import { useQuery } from "@tanstack/vue-query";
import { getQueue } from "@/api/queue.ts";

// See also https://github.com/TanStack/query/issues/5418
export function useQueue() {
  return useQuery({
    queryKey: ["queue"],
    queryFn: getQueue,
    refetchInterval: 5000,
  });
}
