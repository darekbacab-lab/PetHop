<script setup>
import { computed } from 'vue';
import { sessionStorageService } from '../services/storage.js';
import Icon from './Icon.vue';

const props = defineProps({ pet: { type: Object, required: true }, favorite: Boolean });
const emit = defineEmits(['favorite']);
const photo = computed(() => props.pet.photos?.[0] || `/img/${props.pet.image || 'img2.jpeg'}`);
const availableStatus = computed(() => props.pet.status && props.pet.status !== 'Publicada' ? props.pet.status : 'Disponible');

function rememberSelectedPet() {
  sessionStorageService.write('pethop_selected_pet', props.pet);
}
</script>

<template>
  <article
    class="catalog-card pet-card"
    :data-pet-id="pet.id"
    :data-name="pet.name"
    :data-species="pet.species"
    :data-age="pet.age"
    :data-gender="pet.gender"
    :data-size="pet.size"
    :data-location="pet.location"
  >
    <RouterLink class="pet-card-hit-area" :to="{ path: '/mascota.html', query: { pet: pet.id } }" :aria-label="`Abrir perfil de ${pet.name}`" @click="rememberSelectedPet" />
    <div class="catalog-card-image" :style="{ backgroundImage: `url('${photo}')` }">
      <span class="sr-only">Imagen de {{ pet.name }}</span>
      <button
        class="favorite"
        :class="{ active: favorite }"
        type="button"
        :aria-pressed="favorite"
        :aria-label="favorite ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`"
        @click.stop="emit('favorite', pet.id)"
      ><Icon name="heart" :filled="favorite" :stroke-width="1.8" /></button>
    </div>
    <div class="catalog-card-content">
      <div class="catalog-card-top">
        <div>
          <span class="pet-species">
            <Icon v-if="pet.species.toLocaleLowerCase('es-MX') === 'gato'" name="cat" class="pet-species-icon" />
            <Icon v-else name="dog" class="pet-species-icon" />
            {{ pet.species.toLocaleUpperCase('es-MX') }}
          </span>
          <h3>{{ pet.name }}</h3>
        </div>
        <span class="available">{{ availableStatus }}</span>
      </div>
      <div class="catalog-details">
        <span>{{ pet.age || 'Por confirmar' }}</span>
        <span>{{ pet.gender || 'Por confirmar' }}</span>
        <span>{{ pet.size || 'Por confirmar' }}</span>
      </div>
      <div class="catalog-location">
        <Icon name="map-pin" />
        {{ pet.location || 'Ubicación por confirmar' }}
      </div>
    </div>
  </article>
</template>
