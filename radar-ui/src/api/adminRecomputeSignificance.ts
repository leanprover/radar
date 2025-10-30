import { postAdminJson } from "@/api/utils.ts";

export async function postAdminRecomputeSignificance(adminToken: string, repo: string) {
  await postAdminJson("/admin/recompute-significance/", adminToken, { repo });
}
