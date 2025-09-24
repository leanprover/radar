import * as z from "zod";
import { Temporal } from "temporal-polyfill";

function buildUrl(path: string, queryParams?: URLSearchParams): string {
  if (!path.startsWith("/")) throw new Error("path must start with /");
  if (queryParams === undefined || queryParams.size === 0) return `/api${path}`;
  return `/api${path}?${queryParams}`;
}

function checkResponse(url: string, response: Response): void {
  if (!response.ok) {
    throw new Error(`Failed to fetch ${url}:\n${response.statusText}`);
  }
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

export const Timestamp = z.number().transform((it) => Temporal.Instant.fromEpochMilliseconds(Math.round(it * 1000)));
