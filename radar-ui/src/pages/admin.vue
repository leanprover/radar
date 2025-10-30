<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import CSectionTitle from "@/components/CSectionTitle.vue";
import PFormAdminToken from "@/components/pages/admin/PFormAdminToken.vue";
import PFormMaintain from "@/components/pages/admin/PFormMaintain.vue";
import PFormRecomputeSignificance from "@/components/pages/admin/PFormRecomputeSignificance.vue";
</script>

<template>
  <CSection class="max-w-[100ch]">
    <CSectionTitle>Admin token</CSectionTitle>
    <PFormAdminToken />
    <div>To get a token, ask your local admin.</div>
  </CSection>

  <CSection class="max-w-[100ch]">
    <CSectionTitle>Maintenance</CSectionTitle>
    <PFormMaintain />
    <div>
      Perform some aggressive maintenance steps, including vacuuming the DB. Most of these steps already run daily, so
      this button should not be necessary under normal circumstances. In other words: Don't push unless you know what
      you're doing.
    </div>
  </CSection>

  <CSection class="max-w-[100ch]">
    <CSectionTitle>Recompute significance</CSectionTitle>
    <PFormRecomputeSignificance />
    <div>
      Recompute the significance of all commits, which is stored in the DB. This is useful after changing the
      significance settings in the server config. It may take a while for the changes to take effect.
    </div>
  </CSection>
</template>
