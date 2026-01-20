<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import PFormAdminToken from "@/components/pages/admin/PFormAdminToken.vue";
import { radarTitle } from "@/lib/utils.ts";
import { useTitle } from "@vueuse/core";

useTitle(radarTitle("Admin page"));
</script>

<template>
  <CSection title="Admin token" class="max-w-[100ch]">
    <PFormAdminToken />
    <div>To get a token, ask your local admin.</div>
  </CSection>
</template>
