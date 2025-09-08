import * as z from "zod";

export async function fetchJson<S extends z.Schema>(url: string, schema: S): Promise<z.infer<S>> {
  if (!url.startsWith("/")) url = "/" + url;

  const result = await fetch(`/api${url}`);
  if (!result.ok) {
    throw new Error(`Failed to fetch ${url}: ${result.statusText}`);
  }

  const json = await result.json();
  const parsed = schema.safeParse(json);
  if (parsed.error) throw new Error(`Failed to fetch ${url}: ${parsed.error.message}`);
  return parsed.data;
}

export const Timestamp = z.number().transform((it) => new Date(it * 1000));
