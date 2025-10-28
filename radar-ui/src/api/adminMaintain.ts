import { postAdminJson } from "@/api/utils.ts";

export async function postAdminMaintain(adminToken: string, repo: string, aggressive: boolean) {
  await postAdminJson("/admin/maintain/", adminToken, { repo, aggressive });
}
