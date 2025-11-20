<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import PFormAdminToken from "@/components/pages/admin/PFormAdminToken.vue";
</script>

<template>
  <CSection title="Admin token" class="max-w-[100ch]">
    <PFormAdminToken />
    <div>To get a token, ask your local admin.</div>
  </CSection>
</template>
