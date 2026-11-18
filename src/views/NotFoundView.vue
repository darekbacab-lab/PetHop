<script setup>
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
</script>

<template>
  <div><SiteHeader />
    <main id="main-content" tabindex="-1" class="not-found-page"><div class="not-found-container">
      <p class="not-found-code" aria-hidden="true">404</p>
      <h1>Esta página <span>se escapó.</span></h1>
      <p>La dirección que buscaste no existe o se movió. Puede que la mascota que buscabas ya haya encontrado hogar.</p>
      <div class="not-found-actions">
        <RouterLink to="/inicio.html" class="primary-button">Volver al inicio</RouterLink>
        <RouterLink to="/explorar.html" class="secondary-button">Explorar mascotas</RouterLink>
      </div>
    </div></main><SiteFooter /></div>
</template>
