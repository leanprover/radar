import { getQueue } from "@/api/queue.ts";
import { useQuery } from "@tanstack/vue-query";

export function useQueue() {
  return useQuery({
    queryKey: ["queue"],
    queryFn: getQueue,
    refetchInterval: 5000,
  });
}
