<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import PFormAdminToken from "@/components/pages/admin/PFormAdminToken.vue";
import PFormMaintenance from "@/components/pages/admin/PFormMaintenance.vue";
</script>

<template>
  <CSection class="max-w-[100ch]">
    <CSectionTitle>Admin token</CSectionTitle>
    <PFormAdminToken />
    <div>To get a token, ask your local admin.</div>
  </CSection>

  <CSection class="max-w-[100ch]">
    <CSectionTitle>Maintenance</CSectionTitle>
    <PFormMaintenance />
    <div>
      Perform some aggressive maintenance steps, including vacuuming the DB. Most of these steps already run daily, so
      this button should not be necessary under normal circumstances. In other words: Don't push unless you know what
      you're doing.
    </div>
  </CSection>
</template>
