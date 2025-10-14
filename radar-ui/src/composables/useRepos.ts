import { getRepos } from "@/api/repos.ts";
import { useQuery } from "@tanstack/vue-query";

// See also https://github.com/TanStack/query/issues/5418
export function useRepos() {
  return useQuery({ queryKey: ["repos"], queryFn: getRepos });
}
