import { postAdminJson } from "@/api/utils.ts";

export async function postAdminEnqueue(adminToken: string, repo: string, chash: string, priority?: number) {
  await postAdminJson("/admin/enqueue", adminToken, { repo, chash, priority });
}
