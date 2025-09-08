import * as z from "zod";
import { fetchJson } from "@/api/utils.ts";

const JsonRepo = z.object({
  name: z.string(),
  url: z.url(),
  description: z.string(),
});

const JsonGet = z.object({
  repos: z.array(JsonRepo),
});

export async function getRepos() {
  return await fetchJson("/repos", JsonGet);
}
