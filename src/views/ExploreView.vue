<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import PetCard from '../components/PetCard.vue';
import { getFavorites, getPets, toggleFavorite } from '../services/pets.js';

const route = useRoute();
const router = useRouter();
const pets = ref(getPets());
const favorites = ref(getFavorites());
const query = ref(String(route.query.search || ''));
const favoritesOnly = ref(route.query.favorites === '1');
const filters = reactive({ species: '', age: '', gender: '', size: '', location: '', sort: '' });
const appliedFilters = reactive({ ...filters });
const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
const locations = computed(() => [...new Set(pets.value.map(pet => pet.location).filter(Boolean))].sort());
const visiblePets = computed(() => {
  const search = normalize(query.value);
  const result = pets.value.filter(pet => {
    const text = normalize(`${pet.name} ${pet.species} ${pet.location} ${pet.description || ''}`);
    return (!search || text.includes(search)) && (!appliedFilters.species || normalize(pet.species).includes(normalize(appliedFilters.species))) && (!appliedFilters.age || normalize(pet.age).includes(normalize(appliedFilters.age))) && (!appliedFilters.gender || normalize(pet.gender).includes(normalize(appliedFilters.gender))) && (!appliedFilters.size || normalize(pet.size).includes(normalize(appliedFilters.size))) && (!appliedFilters.location || normalize(pet.location).includes(normalize(appliedFilters.location))) && (!favoritesOnly.value || favorites.value.includes(String(pet.id).toLowerCase()));
  });
  if (filters.sort === 'name') result.sort((a, b) => a.name.localeCompare(b.name, 'es'));
  if (filters.sort === 'age') { const rank = { cachorro: 0, joven: 1, adulto: 2, senior: 3 }; result.sort((a, b) => (rank[normalize(a.age)] ?? 99) - (rank[normalize(b.age)] ?? 99)); }
  return result;
});
watch(() => route.query.search, value => { query.value = String(value || ''); });
watch(() => route.query.favorites, value => { favoritesOnly.value = value === '1'; });
function refresh() { pets.value = getPets(); favorites.value = getFavorites(); }
function favorite(id) { toggleFavorite(id); refresh(); }
function applyFilters() { Object.assign(appliedFilters, filters); refresh(); }
function clearFilters() { Object.keys(filters).forEach(key => { filters[key] = ''; appliedFilters[key] = ''; }); query.value = ''; favoritesOnly.value = false; router.replace({ path: route.path }); }
</script>

<template>
  <div class="catalog-page"><SiteHeader />
    <main>
      <section class="catalog-header"><div class="catalog-header-container"><div class="catalog-header-copy"><h1>Encuentra una mascota que conecte <span>contigo.</span></h1></div></div></section>
      <section class="catalog-section"><div class="catalog-container">
        <form class="catalog-search" @submit.prevent="">
          <div class="catalog-search-input"><span class="search-symbol" aria-hidden="true"><Icon name="search" /></span><label class="sr-only" for="catalogSearch">Buscar mascotas</label><input id="catalogSearch" v-model="query" name="search" type="search" placeholder="Buscar por nombre, especie o ubicación" autocomplete="off"></div>
          <button type="submit" class="search-button">Buscar</button>
        </form>
        <div class="catalog-layout">
          <aside class="filter-panel"><div class="filter-panel-header"><span class="eyebrow">FILTROS</span><h2>Encuentra mejor coincidencias.</h2></div>
            <div class="catalog-filter-group"><label for="speciesFilter">Especie</label><select id="speciesFilter" v-model="filters.species"><option value="">Todas</option><option>Perro</option><option>Gato</option><option>Otro</option></select></div>
            <div class="catalog-filter-group"><label for="ageFilter">Edad</label><select id="ageFilter" v-model="filters.age"><option value="">Todas</option><option>Cachorro</option><option>Joven</option><option>Adulto</option><option>Senior</option></select></div>
            <div class="catalog-filter-group"><label for="genderFilter">Sexo</label><select id="genderFilter" v-model="filters.gender"><option value="">Todos</option><option>Macho</option><option>Hembra</option></select></div>
            <div class="catalog-filter-group"><label for="sizeFilter">Tamaño</label><select id="sizeFilter" v-model="filters.size"><option value="">Todos</option><option>Pequeño</option><option>Mediano</option><option>Grande</option></select></div>
            <div class="catalog-filter-group"><label for="locationFilter">Ubicación</label><select id="locationFilter" v-model="filters.location"><option value="">Todas</option><option v-for="location in locations" :key="location">{{ location }}</option></select></div>
            <div class="filter-actions"><button class="btn btn-primary catalog-apply" type="button" @click="applyFilters">Aplicar filtros</button><button class="clear-filters" type="button" @click="clearFilters">Limpiar filtros</button></div>
          </aside>
          <div class="catalog-results"><div class="results-header"><div><span class="eyebrow">RESULTADOS</span><h2>Mascotas disponibles</h2><p class="results-count" aria-live="polite"><strong>{{ visiblePets.length }}</strong>{{ visiblePets.length === 1 ? ' mascota disponible' : ' mascotas disponibles' }}</p></div><div class="sort-control"><label for="sortPets">Ordenar</label><select v-model="filters.sort" id="sortPets"><option value="">Recomendadas</option><option value="name">Nombre</option><option value="age">Edad</option></select></div><button v-if="favoritesOnly" class="clear-filters" type="button" @click="favoritesOnly = false; router.replace({ path: route.path })">Ver todas</button></div>
            <div class="catalog-grid pets-grid"><PetCard v-for="pet in visiblePets" :key="pet.id" :pet="pet" :favorite="favorites.includes(String(pet.id).toLowerCase())" @favorite="favorite" /></div>
            <div v-if="!visiblePets.length" class="catalog-empty"><h2>No encontramos mascotas con esos criterios.</h2><p>Prueba ajustar tu búsqueda o limpiar los filtros.</p><button class="primary-button" type="button" @click="clearFilters">Limpiar filtros</button></div>
          </div>
        </div>
      </div></section>
    </main><SiteFooter />
  </div>
</template>
