import { postAdminJson } from "@/api/utils.ts";

export async function postAdminResetRssBotState(adminToken: string, repo: string, chash: string) {
  await postAdminJson("/admin/reset-rss-bot-state/", adminToken, { repo, chash });
}
