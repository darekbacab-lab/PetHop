<script setup>
import { onMounted, ref } from 'vue';
import { initAnalytics } from '../services/analytics.js';

const visible = ref(false);
const KEY = 'pethop-cookie-consent';

onMounted(() => {
  try {
    visible.value = !localStorage.getItem(KEY);
  } catch {
    visible.value = true;
  }
});

function choose(value) {
  try {
    localStorage.setItem(KEY, value);
  } catch {
    // Sin almacenamiento disponible, la elección solo dura esta visita.
  }
  visible.value = false;
  if (value === 'accepted') initAnalytics();
}
</script>

<template>
  <div v-if="visible" class="cookie-banner" role="region" aria-label="Aviso de cookies">
    <p>Usamos cookies y almacenamiento local para que PetHop funcione y para entender cómo se usa. Puedes aceptar o rechazar; tu elección se guarda en este navegador. <RouterLink to="/privacidad.html">Ver el aviso de privacidad</RouterLink>.</p>
    <div class="cookie-actions">
      <button type="button" class="primary-button" @click="choose('accepted')">Aceptar</button>
      <button type="button" class="secondary-button" @click="choose('rejected')">Rechazar</button>
    </div>
  </div>
</template>
