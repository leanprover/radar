import { fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import * as z from "zod";

const JsonLegalLink = z.object({
  name: z.string(),
  url: z.url(),
});

const JsonGet = z.object({
  legalLinks: z.array(JsonLegalLink),
});

export async function getInfo() {
  return await fetchJson(JsonGet, "/info/");
}

export function useInfo() {
  return useQuery({ queryKey: ["info"], queryFn: getInfo });
}
