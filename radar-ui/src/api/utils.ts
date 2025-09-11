import * as z from "zod";

export async function fetchJson<S extends z.Schema>(
  schema: S,
  path: string,
  queryParams?: Record<string, any>,
): Promise<z.infer<S>> {
  if (!path.startsWith("/")) throw new Error("path must start with /");
  const params = new URLSearchParams(queryParams);
  const url = params.size == 0 ? `/api${path}` : `/api${path}?${params}`;

  const result = await fetch(url);
  if (!result.ok) {
    throw new Error(`Failed to fetch ${url}: ${result.statusText}`);
  }

  const json = await result.json();
  const parsed = schema.safeParse(json);
  if (parsed.error) throw new Error(`Failed to fetch ${url}: ${parsed.error.message}`);
  return parsed.data;
}

export const Timestamp = z.number().transform((it) => new Date(it * 1000));
