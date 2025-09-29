<template>
  <div v-if="$slots['subtitle']" class="flex flex-col">
    <h2 class="text-base font-bold"><slot /></h2>
    <div class="text-foreground-alt text-xs"><slot name="subtitle" /></div>
  </div>
  <h2 v-else class="text-base font-bold"><slot /></h2>
</template>
