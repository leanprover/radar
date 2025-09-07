import { useQuery } from "@tanstack/vue-query";
import * as api from "@/api.ts";

// See also https://github.com/TanStack/query/issues/5418
export function useRepos() {
  return useQuery({ queryKey: ["repos"], queryFn: api.getRepos });
}
