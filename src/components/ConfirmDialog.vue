<script setup>
import { ref } from 'vue';
import { useFocusTrap } from '../services/dialogFocus.js';

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, required: true },
  message: { type: String, required: true },
  confirmLabel: { type: String, default: 'Confirmar' },
  cancelLabel: { type: String, default: 'Cancelar' },
  danger: { type: Boolean, default: false }
});
const emit = defineEmits(['confirm', 'cancel']);
const dialog = ref(null);
useFocusTrap(dialog, () => props.open, () => emit('cancel'));
</script>

<template>
  <div v-if="open" class="dynamic-modal open" role="presentation" @click.self="emit('cancel')">
    <section ref="dialog" class="dynamic-modal-content" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
      <h2 id="confirm-dialog-title">{{ title }}</h2>
      <p>{{ message }}</p>
      <div class="confirm-dialog-actions">
        <button type="button" class="button button-outline" @click="emit('cancel')">{{ cancelLabel }}</button>
        <button type="button" :class="['button', danger ? 'button-danger' : 'button-primary']" @click="emit('confirm')">{{ confirmLabel }}</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.confirm-dialog-actions { display:flex; justify-content:flex-end; gap:10px; margin-top:22px; }
.dynamic-modal-content h2 { margin:0 0 10px; font-size:1.25rem; }
.dynamic-modal-content p { margin:0; color:var(--text-soft); line-height:1.5; }
</style>
