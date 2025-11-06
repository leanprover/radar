<script setup lang="ts">
import CButton from "@/components/CButton.vue";
import CControl from "@/components/CControl.vue";
import CControlRow from "@/components/CControlRow.vue";

const { label = "Filter:", placeholder = undefined } = defineProps<{ label?: string; placeholder?: string }>();

const filter = defineModel<string>({ required: true });
</script>

<template>
  <CControl>
    <CControlRow>
      <label class="contents">
        <span>{{ label }}</span>
        <input v-model="filter" type="text" :placeholder class="bg-background grow px-1" />
      </label>
      <CButton @click="filter = ''">Clear</CButton>
    </CControlRow>
  </CControl>
</template>
