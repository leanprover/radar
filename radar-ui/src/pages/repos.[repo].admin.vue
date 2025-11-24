<script setup lang="ts">
import CSection from "@/components/CSection.vue";
import PFormMaintain from "@/components/pages/repoAdmin/PFormMaintain.vue";
import PFormRecomputeSignificance from "@/components/pages/repoAdmin/PFormRecomputeSignificance.vue";
import PMetricRenamer from "@/components/pages/repoAdmin/PMetricRenamer.vue";
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute("/repos.[repo].admin");
const repo = computed(() => route.params.repo);
</script>

<template>
  <CSection :title="`Admin page for ${repo}`"></CSection>

  <CSection title="Maintenance" class="max-w-[100ch]">
    <PFormMaintain :repo />
    <div>
      Perform some aggressive maintenance steps, including vacuuming the DB. Most of these steps already run daily, so
      this button should not be necessary under normal circumstances. In other words: Don't push unless you know what
      you're doing.
    </div>
  </CSection>

  <CSection title="Recompute significance" class="max-w-[100ch]">
    <PFormRecomputeSignificance :repo />
    <div>
      Recompute the significance of all commits, which is stored in the DB. This is useful after changing the
      significance settings in the server config. It may take a while for the changes to take effect.
    </div>
  </CSection>

  <CSection title="Measurement management" class="w-fit">
    <PMetricRenamer :repo />
  </CSection>
</template>
