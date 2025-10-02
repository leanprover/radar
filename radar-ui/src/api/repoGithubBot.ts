import * as z from "zod";
import { enc, fetchJson } from "@/api/utils.ts";

const JsonCommand = z.object({
  pr: z.string(),
  url: z.string(),
  replyUrl: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
  active: z.boolean(),
});

const JsonGet = z.object({
  commands: JsonCommand.array(),
});

export async function getRepoGithubBot(repo: string) {
  const queryParams = new URLSearchParams();
  return await fetchJson(JsonGet, `/repos/${enc(repo)}/github-bot/`, queryParams);
}
