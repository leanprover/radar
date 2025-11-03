import { JsonOutputLine, Timestamp } from "@/api/types.ts";
import { enc, fetchJson, NotFoundError } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { toValue } from "vue";
import * as z from "zod";

const JsonOutputLineBatch = z.object({
  lines: JsonOutputLine.array(),
  start: z.int(),
});

const JsonActiveRun = z.object({
  benchChash: z.string(),
  startTime: Timestamp,
  lines: JsonOutputLineBatch,
});

const JsonGet = z.object({
  runner: z.string(),
  script: z.string(),
  activeRun: JsonActiveRun.nullish().transform((it) => it ?? undefined),
});

export async function getQueueRun(repo: string, chash: string, run: string) {
  try {
    return await fetchJson(JsonGet, `/queue/runs/${enc(repo)}/${enc(chash)}/${enc(run)}/`);
  } catch (e) {
    if (e instanceof NotFoundError) return "not found";
    throw e;
  }
}

export function useQueueRun(
  repo: MaybeRefOrGetter<string>,
  chash: MaybeRefOrGetter<string>,
  run: MaybeRefOrGetter<string>,
) {
  return useQuery({
    queryKey: ["queueRun", { repo, chash, run }],
    queryFn: () => getQueueRun(toValue(repo), toValue(chash), toValue(run)),
    refetchInterval: 1000,
  });
}
