import { enc, postAdminJson } from "@/api/utils.ts";

export async function postAdminRepoMetricsRename(adminToken: string, repo: string, metrics: Map<string, string>) {
  await postAdminJson(`/admin/repos/${enc(repo)}/metrics/rename/`, adminToken, {
    metrics: Object.fromEntries(metrics),
  });
}
