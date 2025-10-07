import * as z from "zod";
import { enc, fetchJson } from "@/api/utils.ts";

// public record JsonMetric(
//   @JsonProperty(required = true) String metric,
// @JsonProperty(required = true) int direction,
// @JsonProperty(required = true) List<Float> measurements) {}
//
// public record JsonGet(
//   @JsonProperty(required = true) List<String> chashes,
// @JsonProperty(required = true) List<String> titles,
// @JsonProperty(required = true) List<JsonMetric> metrics) {}

export type JsonMetric = z.infer<typeof JsonMetric>;
export const JsonMetric = z.object({
  metric: z.string(),
  direction: z.union([z.literal(-1), z.literal(0), z.literal(1)]),
  measurements: z.number().nullable().array(),
});

export type JsonGet = z.infer<typeof JsonGet>;
export const JsonGet = z.object({
  chashes: z.string().array(),
  titles: z.string().array(),
  metrics: JsonMetric.array(),
});

export async function getRepoGraph(repo: string, m: string[], n: number) {
  const queryParams = new URLSearchParams();

  queryParams.set("n", n.toFixed());
  for (const metric of m) queryParams.append("m", metric);

  return await fetchJson(JsonGet, `/repos/${enc(repo)}/graph/`, queryParams);
}
