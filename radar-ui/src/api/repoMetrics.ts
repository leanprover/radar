import * as z from "zod";
import { enc, fetchJson } from "@/api/utils.ts";

const JsonMetric = z.object({
  metric: z.string(),
  unit: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
});

const JsonGet = z.object({
  metrics: JsonMetric.array(),
});

export async function getRepoMetrics(repo: string) {
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/metrics/`);
}
