import { JsonCommitComparison } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import { QueryClient, useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

const JsonGet = z.object({
  chashFirst: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  chashSecond: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  comparison: JsonCommitComparison,
});

export async function getCompare(repo: string, first: string, second: string) {
  return await fetchJson(JsonGet, `/compare/${enc(repo)}/${enc(first)}/${enc(second)}/`);
}

export function useCompare(
  repo: MaybeRefOrGetter<string>,
  first: MaybeRefOrGetter<string>,
  second: MaybeRefOrGetter<string>,
) {
  return useQuery({
    queryKey: ["compare", { repo, first, second }],
    queryFn: () => getCompare(toValue(repo), toValue(first), toValue(second)),
  });
}

export async function invalidateCompare(queryClient: QueryClient, repo: string, first: string, second: string) {
  await queryClient.invalidateQueries({ queryKey: ["compare", { repo, first, second }] });
}
