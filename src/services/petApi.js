import { api, API_BASE } from './api.js';
import { normalizePersonality } from './petForm.js';

const speciesLabel = { DOG: 'Perro', CAT: 'Gato', OTHER: 'Otro' };
const sexLabel = { MALE: 'Macho', FEMALE: 'Hembra', UNKNOWN: 'Sin especificar' };
const sizeLabel = { SMALL: 'Pequeño', MEDIUM: 'Mediano', LARGE: 'Grande' };
const lifeStageLabel = { PUPPY: 'Cachorro', YOUNG: 'Joven', ADULT: 'Adulto', SENIOR: 'Senior' };
const healthLabel = value => value === true ? 'Sí' : value === false ? 'No' : 'Pendiente de confirmar';

export function mapPet(pet) {
  const age = lifeStageLabel[pet.lifeStage] || 'No especificada';
  const personalityTraits = normalizePersonality(pet.personalityTraits?.length ? pet.personalityTraits : pet.personality);
  const apiOrigin = API_BASE.replace(/\/api\/v1$/, '');
  const images = (pet.images || [])
    .map(image => image?.url)
    .filter(Boolean)
    .map(url => /^https?:\/\//i.test(url) ? url : `${apiOrigin}${url.startsWith('/') ? '' : '/'}${url}`);
  return {
    ...pet, species: speciesLabel[pet.species] || pet.species, gender: sexLabel[pet.sex] || pet.sex,
    size: sizeLabel[pet.size] || pet.size || 'Por confirmar', age, ageGroup: age,
    personalityTraits,
    image: images[0] || '', photos: images,
    vaccination: healthLabel(pet.vaccinated),
    deworming: healthLabel(pet.dewormed),
    sterilization: healthLabel(pet.sterilized)
  };
}

export async function listPets(filters = {}) {
  const params = new URLSearchParams();
  if (filters.species) params.set('species', ({ Perro: 'DOG', Gato: 'CAT', Otro: 'OTHER' })[filters.species] || filters.species);
  if (filters.gender) params.set('sex', ({ Macho: 'MALE', Hembra: 'FEMALE' })[filters.gender] || filters.gender);
  if (filters.size) params.set('petSize', ({ Pequeño: 'SMALL', Mediano: 'MEDIUM', Grande: 'LARGE' })[filters.size] || filters.size);
  if (filters.location) params.set('location', filters.location);
  if (filters.search) params.set('search', filters.search);
  if (filters.sort) params.set('sort', filters.sort);
  params.set('page', String(Math.max(0, Number.parseInt(filters.page, 10) || 0)));
  params.set('pageSize', String(Math.min(48, Math.max(1, Number.parseInt(filters.pageSize, 10) || 48))));
  const result = await api(`/pets?${params}`);
  return {
    ...result,
    content: Array.isArray(result?.content) ? result.content.map(mapPet) : [],
    page: Number.isInteger(result?.page) ? result.page : 0,
    size: Number.isInteger(result?.size) ? result.size : 0,
    totalElements: Number.isFinite(result?.totalElements) ? result.totalElements : 0,
    totalPages: Number.isInteger(result?.totalPages) ? result.totalPages : 0
  };
}

export async function getPetById(id) { return mapPet(await api(`/pets/${encodeURIComponent(id)}`)); }
export async function listFavorites() { return (await api('/favorites')).map(mapPet); }
export async function addFavorite(id) { return mapPet(await api(`/favorites/${encodeURIComponent(id)}`, { method: 'POST' })); }
export async function removeFavorite(id) { return api(`/favorites/${encodeURIComponent(id)}`, { method: 'DELETE' }); }
