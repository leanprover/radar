import { Timestamp } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { Temporal } from "temporal-polyfill";
import { toValue } from "vue";
import * as z from "zod";

export interface JsonCommand {
  pr: number;
  inRepo?: string;
  chashFirst: string;
  chashSecond: string;
  url: string;
  replyUrl?: string;
  created: Temporal.Instant;
  completed?: Temporal.Instant;
}
const JsonCommand = z.object({
  pr: z.number().int(),
  inRepo: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  chashFirst: z.string(),
  chashSecond: z.string(),
  url: z.string(),
  replyUrl: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  created: Timestamp,
  completed: Timestamp.nullish().transform((it) => it ?? undefined),
});

const JsonGet = z.object({
  commands: JsonCommand.array(),
});

export async function getRepoGithubBot(repo: string) {
  const queryParams = new URLSearchParams();
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/github-bot/`, queryParams);
}

export function useRepoGithubBot(repo: MaybeRefOrGetter<string>) {
  return useQuery({
    queryKey: ["repoGithubBot", { repo }],
    queryFn: () => getRepoGithubBot(toValue(repo)),
    refetchInterval: 30 * 1000,
  });
}
