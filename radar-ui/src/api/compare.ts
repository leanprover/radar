import { JsonMetricComparison } from "@/api/types.ts";
import { enc, fetchJson } from "@/api/utils.ts";
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
  comparisons: JsonMetricComparison.array(),
});

export async function getCompare(repo: string, first: string, second: string) {
  return await fetchJson(JsonGet, `/compare/${enc(repo)}/${enc(first)}/${enc(second)}/`);
}
