import { Temporal } from "temporal-polyfill";
import * as z from "zod";

export class NotFoundError extends Error {}

function buildUrl(path: string, queryParams?: URLSearchParams): string {
  if (!path.startsWith("/")) throw new Error("path must start with /");
  if (queryParams === undefined || queryParams.size === 0) return `/api${path}`;
  return `/api${path}?${queryParams}`;
}

function checkResponse(url: string, response: Response): void {
  if (response.status === 404) throw new NotFoundError(`${url} not found:\n${response.statusText}`);
  else if (!response.ok) throw new Error(`Failed to fetch ${url}:\n${response.statusText}`);
}

export async function fetchJson<S extends z.ZodType>(
  schema: S,
  path: string,
  queryParams?: URLSearchParams,
): Promise<z.infer<S>> {
  const url = buildUrl(path, queryParams);

  const response = await fetch(url);
  checkResponse(url, response);

  const parsed = schema.safeParse(await response.json());
  if (parsed.error) throw new Error(`Failed to fetch ${url}:\n${z.prettifyError(parsed.error)}`);
  return parsed.data;
}

export async function postAdminJson(path: string, adminToken: string, data: unknown): Promise<void> {
  const url = buildUrl(path);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Basic " + btoa(`admin:${adminToken}`),
    },
    body: JSON.stringify(data),
  });
  checkResponse(url, response);
}

export const enc = encodeURIComponent;

// TODO Move types into separate file

export const Timestamp = z.number().transform((it) => Temporal.Instant.fromEpochMilliseconds(Math.round(it * 1000)));

export const JsonRun = z.object({
  name: z.string(),
  script: z.string(),
  runner: z.string(),
  active: z
    .object({ startTime: Timestamp })
    .nullish()
    .transform((it) => it ?? undefined),
  finished: z
    .object({ startTime: Timestamp, endTime: Timestamp, exitCode: z.int() })
    .nullish()
    .transform((it) => it ?? undefined),
});

export const JsonOutputLine = z
  .tuple([Timestamp, z.int(), z.string()])
  .transform(([time, source, line]) => ({ time, source, line }));

export const JsonCommitIdent = z.object({
  name: z.string(),
  email: z.string(),
  time: Timestamp,
  offset: z.int(),
});

export const JsonCommit = z.object({
  chash: z.string(),
  author: JsonCommitIdent,
  committer: JsonCommitIdent,
  title: z.string(),
  body: z
    .string()
    .nullish()
    .transform((it) => it ?? undefined),
});
