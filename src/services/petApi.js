import { api, API_BASE } from './api.js';

const speciesLabel = { DOG: 'Perro', CAT: 'Gato', OTHER: 'Otro' };
const sexLabel = { MALE: 'Macho', FEMALE: 'Hembra', UNKNOWN: 'Sin especificar' };
const sizeLabel = { SMALL: 'Pequeño', MEDIUM: 'Mediano', LARGE: 'Grande' };

export function mapPet(pet) {
  const age = pet.ageValue == null ? 'Edad por confirmar' : `${pet.ageValue} ${pet.ageUnit === 'YEARS' ? (pet.ageValue === 1 ? 'año' : 'años') : pet.ageUnit === 'MONTHS' ? (pet.ageValue === 1 ? 'mes' : 'meses') : 'días'}`;
  const ageLower = age.toLocaleLowerCase('es');
  const ageGroup = pet.ageValue == null ? 'Por confirmar' : ageLower.includes('día') || ageLower.includes('mes') ? 'Cachorro' : pet.ageValue < 1 ? 'Cachorro' : pet.ageValue < 2 ? 'Joven' : pet.ageValue >= 8 ? 'Senior' : 'Adulto';
  const apiOrigin = API_BASE.replace(/\/api\/v1$/, '');
  const images = (pet.images || [])
    .map(image => image?.url)
    .filter(Boolean)
    .map(url => /^https?:\/\//i.test(url) ? url : `${apiOrigin}${url.startsWith('/') ? '' : '/'}${url}`);
  return {
    ...pet, species: speciesLabel[pet.species] || pet.species, gender: sexLabel[pet.sex] || pet.sex,
    size: sizeLabel[pet.size] || pet.size || 'Por confirmar', age, ageGroup,
    image: images[0] || '', photos: images,
    vaccination: pet.vaccinated ? 'Sí' : 'Pendiente de confirmar',
    deworming: pet.dewormed ? 'Sí' : 'Pendiente de confirmar',
    sterilization: pet.sterilized ? 'Sí' : 'Pendiente de confirmar'
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
