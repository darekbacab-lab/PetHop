<script setup>
import { computed, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { getFavorites, getPet } from '../services/pets.js';
import Icon from './Icon.vue';
const route = useRoute();
const open = ref(false);
const favoritesOpen = ref(false);
const favorites = computed(() => getFavorites().map(getPet).filter(Boolean));
const count = computed(() => favorites.value.length);
const links = [
  { label: 'Inicio', to: '/', icon: 'house' },
  { label: 'Explorar', to: '/explorar.html', icon: 'search' },
  { label: 'Dar en adopción', to: '/proveedor.html', icon: 'hand-heart' },
  { label: 'Seguimiento', to: '/seguimiento.html', icon: 'paw-print' }
];
watch(open, value => document.body.classList.toggle('menu-open', value));
watch(() => route.fullPath, () => { open.value = false; });
function closeMenu() { open.value = false; }
onUnmounted(() => document.body.classList.remove('menu-open'));
</script>
<template>
  <header class="site-header" @keydown.esc.window="closeMenu"><div class="header-container">
    <button type="button" class="menu-toggle" :class="{ 'menu-toggle-open': open }" :aria-expanded="open" aria-controls="main-menu" :aria-label="open ? 'Cerrar menú' : 'Abrir menú'" @click="open = !open"><Icon :name="open ? 'x' : 'menu'" /></button>
    <RouterLink to="/" class="brand" aria-label="PetHop"><img src="/img/image1.png" alt="PetHop"></RouterLink>
    <nav id="main-menu" class="main-nav" :class="{ open }" :inert="!open" :aria-hidden="!open" aria-label="Navegación principal">
      <div class="menu-drawer-header"><div class="menu-drawer-brand"><img src="/img/image1.png" alt="PetHop"></div><button type="button" class="menu-drawer-close" aria-label="Cerrar menú" @click="closeMenu"><Icon name="x" /></button></div>
      <RouterLink v-for="item in links" :key="item.to" class="nav-link" :class="{ active: route.path === item.to || (item.to === '/' && route.path === '/index.html') }" :to="item.to" @click="open = false"><Icon :name="item.icon" class="nav-item-icon" /><span>{{ item.label }}</span></RouterLink>
      <button type="button" class="favorites-button menu-favorites" @click="favoritesOpen = true; open = false"><Icon name="heart" class="nav-item-icon" /><span class="favorites-label">Favoritos</span><span class="favorites-count">{{ count }}</span></button>
    </nav>
    <div class="header-right"><RouterLink to="/#como-funciona" class="how-button">Cómo funciona</RouterLink><RouterLink to="/acceso.html" class="login-button"><Icon name="log-in" />Ingresar</RouterLink></div>
  </div></header><button v-if="open" class="menu-backdrop is-visible" type="button" aria-label="Cerrar menú" @click="open=false"></button>
  <div v-if="favoritesOpen" class="dynamic-modal open" role="presentation" @click.self="favoritesOpen=false"><section class="dynamic-modal-content" role="dialog" aria-modal="true" aria-labelledby="favorites-title" @keydown.esc="favoritesOpen=false"><button type="button" class="dynamic-modal-close" aria-label="Cerrar favoritos" @click="favoritesOpen=false"><Icon name="x" /></button><h2 id="favorites-title">Favoritos</h2><p v-if="!favorites.length">Aún no has guardado ninguna mascota.</p><template v-else><p>Tus mascotas guardadas.</p><div class="dynamic-modal-list"><div v-for="pet in favorites" :key="pet.id" class="dynamic-modal-list-item"><div><strong>{{ pet.name }}</strong><span>Guardada en favoritos</span></div><RouterLink class="pet-button" :to="{ path: '/mascota.html', query: { pet: pet.id } }" @click="favoritesOpen=false">Ver perfil</RouterLink></div></div></template><RouterLink v-if="!favorites.length" to="/explorar.html" class="primary-button" @click="favoritesOpen=false">Explorar mascotas</RouterLink></section></div>
</template>
