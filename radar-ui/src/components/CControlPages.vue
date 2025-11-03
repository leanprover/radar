<script setup lang="ts">
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";
import { computed, watchEffect } from "vue";

const { total, pageSizes = [100, 500, 1000, 5000] } = defineProps<{ total: number; pageSizes?: number[] }>();

const page = defineModel<number>("page", { required: true });
const pageSize = defineModel<number>("pageSize", { default: 1 });

const pages = computed(() => {
  if (pageSize.value < 1) return 1;
  return Math.ceil(total / pageSize.value);
});

watchEffect(() => {
  const minPage = 0;
  const maxPage = Math.max(minPage, pages.value - 1);
  if (page.value < minPage) page.value = minPage;
  if (page.value > maxPage) page.value = maxPage;
});

watchEffect(() => {
  const minPageSize = Math.max(1, Math.min(...pageSizes));
  if (pageSize.value < minPageSize) pageSize.value = minPageSize;
});
</script>

<template>
  <CControl>
    <CControlRow>
      <CButton @click="page = 0">First</CButton>
      <CButton @click="page -= 1">Prev</CButton>
      <div>Page {{ page + 1 }} / {{ pages }}</div>
      <CButton @click="page += 1">Next</CButton>
      <CButton @click="page = pages - 1">Last</CButton>
      <label class="ml-auto">
        Rows:
        <select v-model="pageSize" class="bg-background px-1">
          <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
        </select>
      </label>
    </CControlRow>
  </CControl>
</template>
