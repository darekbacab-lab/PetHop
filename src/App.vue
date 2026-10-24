<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import { routeLoading } from './router/index.js';
import PageLoadingSkeleton from './components/PageLoadingSkeleton.vue';
import AssistantChat from './components/AssistantChat.vue';
const route = useRoute();
const progress = ref(0);
function updateProgress() {
  const height = document.documentElement.scrollHeight - window.innerHeight;
  progress.value = height > 0 ? Math.min(100, Math.max(0, window.scrollY / height * 100)) : 0;
}
function handleShortcut(event) {
  const target = event.target;
  const typing = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target instanceof HTMLSelectElement;
  if (!typing && event.key === '/' && !event.ctrlKey && !event.metaKey && !event.altKey) {
    const input = document.querySelector('.search-input input, #searchInput');
    if (input) { event.preventDefault(); input.focus(); }
  }
}
function validateField(event) {
  const field = event.target;
  if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement || field instanceof HTMLTextAreaElement) || !field.closest('.form-field')) return;
  if (event.type === 'focusout' && field.value) field.classList.toggle('form-success', field.checkValidity());
  if (event.type === 'focusout' && field.value && !field.checkValidity()) field.classList.add('form-error');
  if (event.type === 'input' && field.checkValidity()) field.classList.remove('form-error');
}
onMounted(() => { window.addEventListener('scroll', updateProgress, { passive: true }); window.addEventListener('resize', updateProgress); document.addEventListener('keydown', handleShortcut); updateProgress(); });
onUnmounted(() => { window.removeEventListener('scroll', updateProgress); window.removeEventListener('resize', updateProgress); document.removeEventListener('keydown', handleShortcut); });
watch(() => route.fullPath, async () => { await nextTick(); updateProgress(); });
</script>

<template><div class="scroll-progress" :style="{ width: `${progress}%` }" aria-hidden="true"></div><div @focusout.capture="validateField" @input.capture="validateField"><PageLoadingSkeleton v-if="routeLoading" /><RouterView v-else /></div><AssistantChat /></template>
