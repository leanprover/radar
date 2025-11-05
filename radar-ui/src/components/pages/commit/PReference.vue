<script setup lang="ts">
import { useRepoHistory } from "@/api/repoHistory.ts";
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";
import CLoading from "@/components/CLoading.vue";
import PReferenceDetails from "@/components/pages/commit/PReferenceDetails.vue";
import {
  ComboboxAnchor,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxPortal,
  ComboboxRoot,
} from "reka-ui";
import { reactive, ref } from "vue";

const { repo, chash } = defineProps<{ repo: string; chash: string | undefined }>();
const reference = defineModel<string>();

const search = ref<string>("");
const history = reactive(useRepoHistory(repo, { n: 16, s: search }));
</script>

<template>
  <CControl class="max-w-[80ch]">
    <CControlRow>
      <div>Search:</div>

      <ComboboxRoot v-model="reference" ignore-filter class="flex grow">
        <ComboboxAnchor class="flex grow">
          <ComboboxInput v-model="search" placeholder="<parent>" class="bg-background grow px-1" />
        </ComboboxAnchor>

        <ComboboxPortal>
          <ComboboxContent position="popper" align="start" class="bg-background z-[100] min-w-[40ch] border">
            <CLoading v-if="!history.isSuccess" :error="history.error" class="px-1" />
            <template v-else>
              <ComboboxEmpty>No search results.</ComboboxEmpty>
              <ComboboxItem
                v-for="entry in history.data.entries"
                :key="entry.commit.chash"
                :value="entry.commit.chash"
                class="hover:bg-background-alt cursor-default px-1"
              >
                {{ entry.commit.title }}
              </ComboboxItem>
            </template>
          </ComboboxContent>
        </ComboboxPortal>
      </ComboboxRoot>

      <CButton @click="reference = undefined">Clear</CButton>
    </CControlRow>
  </CControl>

  <div v-if="chash === undefined">No reference commit selected.</div>
  <PReferenceDetails v-else :repo :chash />
</template>
