import { postAdminJson } from "@/api/utils.ts";

export async function postAdminVacuum(adminToken: string, repo: string) {
  await postAdminJson("/admin/vacuum/", adminToken, { repo });
}
