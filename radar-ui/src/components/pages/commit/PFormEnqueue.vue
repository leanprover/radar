<script setup lang="ts">
import { postAdminEnqueue } from "@/api/adminEnqueue.ts";
import { invalidateCommit } from "@/api/commit.ts";
import { invalidateCompare } from "@/api/compare.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlCol from "@/components/CControlCol.vue";
import { useAdminStore } from "@/stores/useAdminStore.ts";
import { useQueryClient } from "@tanstack/vue-query";
import { ref } from "vue";

const { repo, chash, reference } = defineProps<{ repo: string; chash: string; reference: string }>();

const admin = useAdminStore();
const queryClient = useQueryClient();

const enqueuePriority = ref(-1);

async function onClick() {
  if (admin.token === undefined) return;
  await postAdminEnqueue(admin.token, repo, chash, enqueuePriority.value);
  void invalidateCommit(queryClient, repo, chash);
  void invalidateCompare(queryClient, repo, reference, chash);
}
</script>

<template>
  <CControl class="max-w-[80ch]">
    <CControlCol>
      <div>
        <CButton @click="onClick()"> Enqueue </CButton>
        with priority <input v-model="enqueuePriority" type="number" class="bg-background w-[8ch] px-1" />
      </div>
      <details>
        <summary>Explanation</summary>
        <div class="mt-2">
          Add this commit to the queue (again). Any existing measurement and run data will be deleted. Commits with
          higher priority value appear earlier in the queue. Within a priority, the queue is FIFO.
        </div>
        <div class="mt-2">
          Priority of new commits: 0 <br />
          Priority of commits added by !bench: 1
        </div>
      </details>
    </CControlCol>
  </CControl>
</template>
