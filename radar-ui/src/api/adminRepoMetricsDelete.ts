import { enc, postAdminJson } from "./utils";

export async function postAdminRepoMetricsDelete(adminToken: string, repo: string, metrics: Set<string>) {
  await postAdminJson(`/admin/repos/${enc(repo)}/metrics/delete/`, adminToken, {
    metrics: Array.from(metrics),
  });
}
