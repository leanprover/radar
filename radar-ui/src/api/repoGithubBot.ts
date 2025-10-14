import { enc, fetchJson, Timestamp } from "@/api/utils.ts";
import * as z from "zod";

const JsonCommand = z.object({
  pr: z.int(),
  chash: z.string(),
  againstChash: z.string(),
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
