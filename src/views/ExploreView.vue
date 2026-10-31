<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import PetCard from '../components/PetCard.vue';
import { addFavorite, listFavorites, listPets, removeFavorite } from '../services/petApi.js';

const route = useRoute();
const router = useRouter();
const pets = ref([]);
const favorites = ref([]);
const loading = ref(false);
const loadError = ref('');
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
    return (!search || text.includes(search)) && (!appliedFilters.age || normalize(pet.ageGroup).includes(normalize(appliedFilters.age))) && (!favoritesOnly.value || favorites.value.includes(String(pet.id).toLowerCase()));
  });
  if (filters.sort === 'name') result.sort((a, b) => a.name.localeCompare(b.name, 'es'));
  if (filters.sort === 'age') { const rank = { cachorro: 0, joven: 1, adulto: 2, senior: 3 }; result.sort((a, b) => (rank[normalize(a.age)] ?? 99) - (rank[normalize(b.age)] ?? 99)); }
  return result;
});
watch(() => route.query.search, value => { query.value = String(value || ''); loadPets(); });
watch(() => route.query.favorites, value => { favoritesOnly.value = value === '1'; });
async function loadPets() {
  loading.value = true; loadError.value = '';
  try {
    const result = await listPets({ ...filters, search: query.value, sort: filters.sort === 'age' ? '' : filters.sort });
    pets.value = result.content;
    if (filters.sort === 'age') { const rank = { cachorro: 0, joven: 1, adulto: 2, senior: 3 }; pets.value.sort((a,b) => (rank[normalize(a.ageGroup)] ?? 99) - (rank[normalize(b.ageGroup)] ?? 99)); }
  } catch (error) { pets.value = []; loadError.value = error.message || 'No fue posible cargar las mascotas. Verifica que el backend esté iniciado.'; }
  finally { loading.value = false; }
}
async function loadFavorites() { try { favorites.value = (await listFavorites()).map(pet => String(pet.id).toLowerCase()); } catch { favorites.value = []; } }
async function refresh() { await Promise.all([loadPets(), loadFavorites()]); }
async function favorite(id) {
  try {
    const key = String(id).toLowerCase();
    if (favorites.value.includes(key)) { await removeFavorite(id); favorites.value = favorites.value.filter(item => item !== key); }
    else { await addFavorite(id); favorites.value = [...favorites.value, key]; }
  } catch (error) { loadError.value = error.status === 401 ? 'Inicia sesión para guardar mascotas en favoritos.' : error.message; }
}
function applyFilters() { Object.assign(appliedFilters, filters); loadPets(); }
function clearFilters() { Object.keys(filters).forEach(key => { filters[key] = ''; appliedFilters[key] = ''; }); query.value = ''; favoritesOnly.value = false; router.replace({ path: route.path }); loadPets(); }
onMounted(refresh);
</script>

<template>
  <div class="catalog-page"><SiteHeader />
    <main>
      <section class="catalog-header"><div class="catalog-header-container"><div class="catalog-header-copy"><h1>Encuentra una mascota <span>que conecte contigo.</span></h1></div></div></section>
      <section class="catalog-section"><div class="catalog-container">
        <form class="catalog-search" @submit.prevent="applyFilters">
          <div class="catalog-search-input"><span class="search-symbol" aria-hidden="true"><Icon name="search" /></span><label class="sr-only" for="catalogSearch">Buscar mascotas</label><input id="catalogSearch" v-model="query" name="search" type="search" placeholder="Buscar por nombre, especie o ubicación" autocomplete="off"></div>
          <button type="submit" class="search-button">Buscar</button>
        </form>
        <div class="catalog-layout">
          <section class="filter-panel catalog-filterbar" aria-label="Filtros de búsqueda">
            <div class="catalog-filter-controls">
              <div class="catalog-filter-group"><label for="speciesFilter">Especie</label><select id="speciesFilter" v-model="filters.species"><option value="">Todas</option><option>Perro</option><option>Gato</option><option>Otro</option></select></div>
              <div class="catalog-filter-group"><label for="ageFilter">Edad</label><select id="ageFilter" v-model="filters.age"><option value="">Todas</option><option>Cachorro</option><option>Joven</option><option>Adulto</option><option>Senior</option></select></div>
              <div class="catalog-filter-group"><label for="genderFilter">Sexo</label><select id="genderFilter" v-model="filters.gender"><option value="">Todos</option><option>Macho</option><option>Hembra</option></select></div>
              <div class="catalog-filter-group"><label for="sizeFilter">Tamaño</label><select id="sizeFilter" v-model="filters.size"><option value="">Todos</option><option>Pequeño</option><option>Mediano</option><option>Grande</option></select></div>
              <div class="catalog-filter-group"><label for="locationFilter">Ubicación</label><select id="locationFilter" v-model="filters.location"><option value="">Todas</option><option v-for="location in locations" :key="location">{{ location }}</option></select></div>
              <div class="catalog-filter-group"><label for="sortPets">Ordenar</label><select v-model="filters.sort" id="sortPets"><option value="">Recomendadas</option><option value="name">Nombre</option><option value="age">Edad</option></select></div>
              <div class="filter-actions"><button class="btn btn-primary catalog-apply" type="button" @click="applyFilters">Aplicar filtros</button><button class="clear-filters" type="button" @click="clearFilters">Limpiar filtros</button></div>
            </div>
          </section>
        <div class="catalog-results"><div class="results-header"><div><h2>Mascotas disponibles</h2><p class="results-count" aria-live="polite"><strong>{{ loadError ? '—' : visiblePets.length }}</strong>{{ loadError ? ' disponibilidad no consultada' : visiblePets.length === 1 ? ' mascota disponible' : ' mascotas disponibles' }}</p></div><button v-if="favoritesOnly" class="clear-filters" type="button" @click="favoritesOnly = false; router.replace({ path: route.path })">Ver todas</button></div>
            <div v-if="loadError" class="catalog-empty" role="alert"><h2>No se pudieron consultar las mascotas.</h2><p>{{ loadError }}</p><button class="primary-button" type="button" @click="refresh">Reintentar</button></div>
            <div v-else-if="loading" class="catalog-empty" role="status"><p>Cargando mascotas…</p></div>
            <div v-else class="catalog-grid pets-grid"><PetCard v-for="pet in visiblePets" :key="pet.id" :pet="pet" :favorite="favorites.includes(String(pet.id).toLowerCase())" @favorite="favorite" /></div>
            <div v-if="!loading && !loadError && !visiblePets.length" class="catalog-empty"><h2>No encontramos mascotas con esos criterios.</h2><p>Prueba ajustar tu búsqueda o limpiar los filtros.</p><button class="primary-button" type="button" @click="clearFilters">Limpiar filtros</button></div>
          </div>
        </div>
      </div></section>
    </main><SiteFooter />
  </div>
</template>

<style scoped>
.catalog-header { margin-bottom:24px; padding-top:26px; }
.catalog-header h1 { max-width:970px; color:#14545c; font-size:clamp(2.8rem,5.4vw,4.7rem); font-weight:850; line-height:.99; letter-spacing:-.055em; }
.catalog-header h1 span { color:#39b0b9; }
.catalog-layout { display:block; }
.catalog-filterbar { position:static; margin-bottom:18px; padding:12px 16px; border-color:#5eaeb4; background-color:var(--background); }
.catalog-filter-controls { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)) minmax(130px,1.2fr) minmax(168px,1.55fr) repeat(2,minmax(140px,1.2fr)); gap:10px; align-items:end; }
.catalog-filter-controls .catalog-filter-group { min-width:0; margin:0; display:block; width:auto; }
.catalog-filter-group label { margin-bottom:4px; font-size:.7rem; }
.catalog-filter-controls .catalog-filter-group select { min-width:0; min-height:40px; width:100%; padding:7px 26px 7px 10px; border-color:#5eaeb4; background-color:var(--white); font-size:.8rem; }
.catalog-filter-controls .filter-actions { display:contents; }
.catalog-filter-controls .filter-actions > button { width:100%; min-width:0; min-height:40px; padding:8px 10px; white-space:nowrap; }
.results-header > div:first-child h2 { color:#14545c; font-size:1.7rem; font-weight:800; }
.results-count { display:flex; align-items:baseline; gap:7px; }
.catalog-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }
@media (max-width:1000px) {
  .catalog-filter-controls { grid-template-columns:repeat(3,minmax(0,1fr)); }
  .catalog-filter-controls .catalog-apply { grid-column:auto; }
  .catalog-filter-controls .filter-actions { display:grid; grid-column:1 / -1; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; }
  .catalog-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
}
@media (max-width:700px) {
  .catalog-header { margin-bottom:8px; padding-top:16px; }
  .catalog-header h1 { margin:0; font-size:clamp(2.05rem,7.8vw,2.8rem); line-height:1; }
  .catalog-section { padding-top:12px; }
  .catalog-search { align-items:stretch; flex-direction:row; gap:7px; margin-bottom:10px; }
  .catalog-search-input input { min-height:44px; padding:10px 10px 10px 40px; font-size:.78rem; }
  .catalog-search .search-button { flex:0 0 25%; min-width:78px; min-height:44px; padding:0 10px; }
  .catalog-filterbar { margin-bottom:10px; padding:10px; }
  .catalog-filter-controls { grid-template-columns:repeat(2,minmax(0,1fr)); gap:6px 8px; }
  .catalog-filter-group label { margin-bottom:3px; font-size:.65rem; }
  .catalog-filter-controls .catalog-filter-group select { min-height:40px; padding:6px 24px 6px 8px; font-size:.74rem; }
  .catalog-filter-controls .filter-actions { grid-column:1 / -1; display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:4px; }
  .catalog-filter-controls .filter-actions > button { width:100%; min-height:42px; padding:7px 5px; white-space:normal; }
  .catalog-results { margin-top:0; }
  .results-header { flex-direction:row; align-items:flex-end; flex-wrap:wrap; gap:6px 12px; margin-bottom:12px; }
  .results-header > div:first-child h2 { font-size:1.45rem; line-height:1.08; }
  .results-count { margin-top:2px; }
  .catalog-grid { grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
}
@media (max-width:480px) {
  .results-header { align-items:flex-start; flex-direction:column; }
  .sort-control { width:100%; justify-content:space-between; }
  .catalog-grid { grid-template-columns:1fr; }
}
@media (max-width:360px) {
  .catalog-search { align-items:stretch; flex-direction:column; }
  .catalog-search .search-button { flex:0 0 auto; width:100%; }
}
</style>
