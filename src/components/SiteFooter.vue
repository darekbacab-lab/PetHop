<script setup>
import { computed } from 'vue';
import { currentUser } from '../services/auth.js';
const trackingLink = computed(() => currentUser.value?.role === 'SHELTER' ? '/refugio.html?section=solicitudes' : '/seguimiento.html');
</script>
<template>
  <footer class="site-footer"><div class="footer-container">
    <div class="footer-brand"><RouterLink to="/inicio.html" class="brand" aria-label="PetHop"><img src="/img/image1.png" alt="PetHop"></RouterLink><p>Plataforma digital para facilitar procesos de adopción responsable.</p></div>
    <div class="footer-column"><h2>Explorar</h2><RouterLink to="/explorar.html">Mascotas</RouterLink><RouterLink to="/inicio.html#como-funciona">Cómo funciona</RouterLink><RouterLink :to="trackingLink">Seguimiento</RouterLink></div>
    <div class="footer-column"><h2>Organizaciones</h2><RouterLink to="/inicio.html#registrar">Registrar refugio</RouterLink></div>
    <div class="footer-column"><h2>Ayuda</h2><RouterLink to="/aliados.html">Aliados</RouterLink><RouterLink to="/acceso.html">Ingresar</RouterLink><a href="mailto:pethop13@gmail.com">Escríbenos: pethop13@gmail.com</a></div>
  </div><div class="footer-bottom"><span>© {{ new Date().getFullYear() }} PetHop · Adopción responsable</span></div></footer>
</template>
