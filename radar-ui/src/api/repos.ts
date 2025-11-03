import { fetchJson } from "@/api/utils.ts";
import { useQuery } from "@tanstack/vue-query";
import type { MaybeRefOrGetter } from "@vueuse/core";
import { computed, toValue } from "vue";
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

export function useRepos() {
  return useQuery({ queryKey: ["repos"], queryFn: getRepos });
}

export function useRepo(repo: MaybeRefOrGetter<string>) {
  const { data } = useRepos();

  return computed(() => {
    const repoValue = toValue(repo);
    return data.value?.repos.find((it) => it.name === repoValue);
  });
}
