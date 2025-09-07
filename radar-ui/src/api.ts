import * as z from "zod";

async function fetchJson<S extends z.Schema>(url: string, schema: S): Promise<z.infer<S>> {
  if (!url.startsWith("/")) url = "/" + url;

  const result = await fetch(`/api${url}`);
  if (!result.ok) {
    throw new Error(`Failed to fetch ${url}: ${result.statusText}`);
  }

  const json = await result.json();
  const parsed = schema.safeParse(json);
  console.log(json);
  if (parsed.error) throw new Error(`Failed to fetch ${url}: ${parsed.error.message}`);
  return parsed.data;
}

export type JsonRepo = z.infer<typeof JsonRepo>;
export const JsonRepo = z.object({
  name: z.string(),
  url: z.url(),
  description: z.string(),
});

export async function getRepos(): Promise<JsonRepo[]> {
  return await fetchJson("/repos", z.array(JsonRepo));
}
