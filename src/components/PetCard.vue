<script setup>
import { computed } from 'vue';
import Icon from './Icon.vue';
import ImageWithSkeleton from './ImageWithSkeleton.vue';

const props = defineProps({ pet: { type: Object, required: true }, favorite: Boolean });
const emit = defineEmits(['favorite']);
const photo = computed(() => props.pet.photos?.[0] || props.pet.image || '');
const availableStatus = computed(() => props.pet.status && props.pet.status !== 'Publicada' ? props.pet.status : 'Disponible');

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
    <RouterLink class="pet-card-hit-area" :to="{ path: '/mascota.html', query: { pet: pet.id } }" :aria-label="`Abrir perfil de ${pet.name}`" />
    <ImageWithSkeleton frame-class="catalog-card-image" :src="photo" :alt="`Imagen de ${pet.name}`">
      <span v-if="!photo" class="catalog-no-photo" aria-hidden="true"><Icon name="paw-print" /></span>
      <button
        class="favorite"
        :class="{ active: favorite }"
        type="button"
        :aria-pressed="favorite"
        :aria-label="favorite ? `Quitar ${pet.name} de favoritos` : `Agregar ${pet.name} a favoritos`"
        @click.stop="emit('favorite', pet.id)"
      ><Icon name="heart" :filled="favorite" :stroke-width="1.8" /></button>
    </ImageWithSkeleton>
    <div class="catalog-card-content">
      <div class="catalog-card-top">
        <div>
          <span class="pet-species">
            <Icon v-if="pet.species.toLocaleLowerCase('es-MX') === 'gato'" name="cat" class="pet-species-icon" />
            <Icon v-else-if="pet.species.toLocaleLowerCase('es-MX') === 'perro'" name="dog" class="pet-species-icon" />
            <Icon v-else name="paw-print" class="pet-species-icon" />
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
