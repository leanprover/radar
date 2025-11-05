import { JsonCommit } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

const JsonEntry = z.object({
  commit: JsonCommit,
  hasRuns: z.boolean(),
  significant: z
    .boolean()
    .nullish()
    .transform((it) => it ?? undefined),
});

const JsonGet = z.object({
  total: z.number(),
  entries: JsonEntry.array(),
});

export async function getRepoHistory(repo: string, params?: { n?: number; skip?: number; s?: string }) {
  const queryParams = new URLSearchParams();
  if (params?.n !== undefined) queryParams.set("n", params.n.toFixed());
  if (params?.skip !== undefined) queryParams.set("skip", params.skip.toFixed());
  if (params?.s !== undefined) queryParams.set("s", params.s);
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/history/`, queryParams);
}

export function useRepoHistory(
  repo: MaybeRefOrGetter<string>,
  params?: {
    n?: MaybeRefOrGetter<number> | MaybeRefOrGetter<number | undefined>;
    skip?: MaybeRefOrGetter<number> | MaybeRefOrGetter<number | undefined>;
    s?: MaybeRefOrGetter<string> | MaybeRefOrGetter<string | undefined>;
  },
) {
  return useQuery({
    queryKey: ["repoHistory", { repo, n: params?.n, skip: params?.skip, s: params?.s }],
    queryFn: () =>
      getRepoHistory(toValue(repo), {
        n: toValue(params?.n),
        skip: toValue(params?.skip),
        s: toValue(params?.s),
      }),
  });
}
