import { useQuery } from "@tanstack/vue-query";
import { getQueue } from "@/api/queue.ts";

export function useQueue() {
  return useQuery({
    queryKey: ["queue"],
    queryFn: getQueue,
    refetchInterval: 5000,
  });
}
