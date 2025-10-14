import { enc, fetchJson } from "@/api/utils.ts";
import * as z from "zod";

const JsonMeasurement = z.object({
  metric: z.string(),
  first: z
    .number()
    .nullish()
    .transform((x) => x ?? undefined),
  second: z
    .number()
    .nullish()
    .transform((x) => x ?? undefined),
  firstSource: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  secondSource: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  unit: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  direction: z.union([z.literal(-1), z.literal(0), z.literal(1)]),
});

const JsonGet = z.object({
  chashFirst: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  chashSecond: z
    .string()
    .nullish()
    .transform((x) => x ?? undefined),
  measurements: z.array(JsonMeasurement),
});

export async function getCompare(repo: string, first: string, second: string) {
  return await fetchJson(JsonGet, `/compare/${enc(repo)}/${enc(first)}/${enc(second)}/`);
}
