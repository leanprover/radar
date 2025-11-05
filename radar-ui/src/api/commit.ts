import { JsonCommit, JsonRun } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import { QueryClient, useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

export interface JsonLinkedCommit {
  chash: string;
  title: string;
  tracked: boolean;
}
const JsonLinkedCommit = z.object({
  chash: z.string(),
  title: z.string(),
  tracked: z.boolean(),
});

const JsonGet = z.object({
  commit: JsonCommit,
  parents: z.array(JsonLinkedCommit),
  children: z.array(JsonLinkedCommit),
  runs: z.array(JsonRun),
});

export async function getCommit(repo: string, chash: string) {
  return await fetchJson(JsonGet, `/commits/${enc(repo)}/${enc(chash)}/`);
}

export function useCommit(repo: MaybeRefOrGetter<string>, chash: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["commit", { repo, chash }],
    queryFn: () => getCommit(toValue(repo), toValue(chash)),
  });
}

export async function invalidateCommit(queryClient: QueryClient, repo: string, chash: string) {
  await queryClient.invalidateQueries({ queryKey: ["commit", { repo, chash }] });
}
