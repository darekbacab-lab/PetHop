import { ref } from 'vue';
import '../../js/pethop-data.js';
import { storageService, sessionStorageService } from './storage.js';

export const staticPets = [
  { id: 'chino', name: 'Chino', species: 'Perro', age: 'Adulto', gender: 'Macho', size: 'Mediano', location: 'Playa del Carmen', image: 'img2.jpeg', description: 'Chino es un perro adulto de tamaño mediano. El refugio puede contarte cómo convive, qué actividades disfruta y qué tipo de hogar se adapta mejor a él.' },
  { id: 'luna', name: 'Luna', species: 'Perro', age: 'Joven', gender: 'Hembra', size: 'Mediano', location: 'Playa del Carmen', image: 'img3.jpeg', description: 'Luna busca una familia que la acompañe con cariño y paciencia.' },
  { id: 'milo', name: 'Milo', species: 'Gato', age: 'Joven', gender: 'Macho', size: 'Pequeño', location: 'Cancún', image: 'img5.jpg', description: 'Milo busca un hogar responsable.' },
  { id: 'nala', name: 'Nala', species: 'Gato', age: 'Adulto', gender: 'Hembra', size: 'Pequeño', location: 'Cozumel', image: 'img6.jpg', description: 'Nala busca un hogar responsable.' },
  { id: 'max', name: 'Max', species: 'Perro', age: 'Senior', gender: 'Macho', size: 'Grande', location: 'Cancún', image: 'img4.jpeg', description: 'Max busca un hogar tranquilo.' },
  { id: 'kira', name: 'Kira', species: 'Perro', age: 'Cachorro', gender: 'Hembra', size: 'Pequeño', location: 'Playa del Carmen', image: 'img7.jpeg', description: 'Kira busca una familia que la acompañe mientras crece.' }
];

const savedFavorites = storageService.read('pethop_favorites', []);
const favoriteState = ref(Array.isArray(savedFavorites) ? savedFavorites.filter(Boolean) : []);

export function getPets() {
  const directory = globalThis.PetHopData?.petDirectory(Object.fromEntries(staticPets.map(pet => [pet.id, pet])), storageService) || Object.fromEntries(staticPets.map(pet => [pet.id, pet]));
  return Object.values(directory).map(pet => ({ ...pet, image: pet.image?.includes('.') ? pet.image : (staticPets.find(base => base.id === String(pet.id).toLowerCase())?.image || 'img2.jpeg') }));
}

export function getPet(id) {
  const pet = getPets().find(item => String(item.id).toLowerCase() === String(id || '').toLowerCase());
  const selected = sessionStorageService.read('pethop_selected_pet', null);
  return selected?.id === pet?.id ? { ...pet, ...selected } : pet;
}

export function getFavorites() {
  return favoriteState.value;
}

export function toggleFavorite(id) {
  const favorites = getFavorites();
  const key = String(id).toLowerCase();
  const next = favorites.includes(key) ? favorites.filter(item => item !== key) : [...favorites, key];
  const saved = storageService.write('pethop_favorites', next);
  if (saved) favoriteState.value = next;
  return saved;
}
