import { fetchJson } from "@/api/utils.ts";
import * as z from "zod";

const JsonRepo = z.object({
  name: z.string(),
  url: z.url(),
  benchUrl: z.url(),
  description: z.string(),
  lakeprofReportUrl: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
});

const JsonGet = z.object({
  repos: z.array(JsonRepo),
});

export async function getRepos() {
  return await fetchJson(JsonGet, "/repos/");
}
