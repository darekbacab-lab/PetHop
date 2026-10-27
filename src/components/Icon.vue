<script setup>
import { computed } from 'vue';
import { lucideIcons } from '../icons/lucide-icons.js';

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  strokeWidth: { type: [Number, String], default: 2 },
  filled: Boolean,
  class: { type: [String, Array, Object], default: '' }
});

const nodes = computed(() => lucideIcons[props.name] || []);

</script>

<template>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :fill="filled ? 'currentColor' : 'none'"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    :class="['lucide', `lucide-${name}`, props.class]"
  >
    <component v-for="([tag, attrs], index) in nodes" :key="index" :is="tag" v-bind="attrs" />
  </svg>
</template>
