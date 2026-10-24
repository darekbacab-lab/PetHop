<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import '../../js/pethop-data.js';
import SiteHeader from '../components/SiteHeader.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { staticPets } from '../services/pets.js';
import { storageService } from '../services/storage.js';

const router = useRouter();
const route = useRoute();
const PETS = [
  { id: 'chino', name: 'Chino', species: 'Perro', age: 'Adulto', gender: 'Macho', status: 'Publicada', requests: 3, days: 12, image: 'chino' },
  { id: 'luna', name: 'Luna', species: 'Perro', age: 'Joven', gender: 'Hembra', status: 'En revisión', requests: 2, days: 8, image: 'luna' },
  { id: 'milo', name: 'Milo', species: 'Gato', age: 'Joven', gender: 'Macho', status: 'En proceso de adopción', requests: 1, days: 19, image: 'milo' }
];
const DEMO_REQUESTS = [
  { id: 'req-1', adopter: 'Mariana López', pet: 'Chino', petId: 'chino', date: '2026-10-05', status: 'Nueva', home: 'Casa con patio', experience: 'Tuvo un perro durante 8 años', household: '2 adultos', reason: 'Buscamos un compañero para la familia.', conditions: 'Patio cercado y paseos diarios.', notes: 'Puede convivir con otro perro tranquilo.' },
  { id: 'req-2', adopter: 'Carlos Méndez', pet: 'Luna', petId: 'luna', date: '2026-10-04', status: 'En revisión', home: 'Departamento', experience: 'Cuida perros de familiares', household: '1 adulto', reason: 'Quiero darle un hogar a una mascota.', conditions: 'Espacios interiores y salidas frecuentes.', notes: 'Solicitó conocer su nivel de actividad.' },
  { id: 'req-3', adopter: 'Ana Torres', pet: 'Milo', petId: 'milo', date: '2026-10-02', status: 'Aceptada', home: 'Casa', experience: 'Convive actualmente con un gato', household: '2 adultos y 1 niño', reason: 'Acompañar a nuestra gata adulta.', conditions: 'Adaptación gradual en habitación separada.', notes: 'Pendiente acordar encuentro.' }
];
const NAV_ITEMS = [
  { label: 'Inicio', icon: 'layout-dashboard' }, { label: 'Mis mascotas', icon: 'paw-print' },
  { label: 'Publicar mascota', icon: 'plus-circle' }, { label: 'Solicitudes', icon: 'clipboard-list' },
  { label: 'En proceso', icon: 'refresh-cw' }, { label: 'Completadas', icon: 'check-circle-2' },
  { label: 'Mensajes', icon: 'messages-square' }, { label: 'Seguimiento', icon: 'route' },
  { label: 'Perfil / Organización', icon: 'contact-round' }, { label: 'Configuración', icon: 'settings-2' },
  { label: 'Ayuda', icon: 'circle-help' }
];
function initialState() {
  const stored = storageService.read('pethop_provider_workspace', {}) || {};
  const knownPets = stored.pets || [...PETS];
  const applications = storageService.read('pethop_applications', []) || [];
  const providerStatus = { Recibida:'Nueva', 'En revisión':'En revisión', Contactada:'En proceso', Aprobada:'Aceptada', Adoptada:'Adoptada', Rechazada:'Rechazada' };
  const requestRecords = (Array.isArray(applications) ? applications : []).map(app => {
    const form = app.data || {};
    const pet = knownPets.find(item => String(item.id) === String(app.petId))?.name || app.petId || 'Mascota';
    return { id: app.id, adopter: form.fullName || form.nombre || 'Adoptante', adopterEmail: form.email || form.correo || '', pet, petId: String(app.petId || ''), date: app.date || new Date().toISOString(), status: providerStatus[app.status] || 'Nueva', home: form.housing || 'No especificado', experience: form.experience || 'No especificada', household: form.people || 'No especificado', reason: form.reason || 'No especificado', conditions: [form.outdoorSpace, form.permission].filter(Boolean).join(' · ') || 'No especificadas', notes: [form.otherPets, form.otherPetsType, form.otherPetsDetails].filter(Boolean).join(' · ') || 'Sin información adicional' };
  });
  return { ...stored, role: stored.role || '', section: stored.section || 'Inicio', pets: knownPets, requests: [...requestRecords, ...(stored.requests || DEMO_REQUESTS).filter(item => !requestRecords.some(actual => actual.id === item.id))], messages: stored.messages || [], profile: stored.profile || {}, draft: stored.draft || {}, step: stored.step || 0, filter: stored.filter || 'Todas', petFilter: stored.petFilter || 'Todas', notify: stored.notify ?? true };
}
const state = reactive(initialState());
const entryChoosingRole = ref(false); const section = ref(route.query.section === 'mensajes' ? 'Mensajes' : state.section); const notice = ref('');
if (route.query.section === 'mensajes') state.section = 'Mensajes';
const sidebarOpen = ref(false);
const narrowScreen = ref(false);
let sidebarMediaQuery;
function updateScreenWidth(event) { narrowScreen.value = event.matches; }
const requestFilter = ref(state.filter); const petFilter = ref(state.petFilter);
const selectedRequest = ref(null); const selectedPet = ref(null); const selectedPetStatus = ref(''); const editingPet = ref(false); const petEditForm = ref({}); const selectedMessageId = ref(state.activeMessage || '');
const messagesRevision = ref(0);
const steps = ['Información básica','Salud','Personalidad','Historia','Requisitos','Fotografías','Revisión'];
const trackingSteps = ['Solicitud recibida','Solicitud en revisión','Contacto realizado','Adopción en preparación','Entrega / encuentro','Seguimiento inicial','Adopción completada'];
const stepFields = [
  [['name','Nombre','text'],['species','Especie','select','Perro|Gato|Otro'],['age','Edad','select','Cachorro|Joven|Adulto|Senior'],['gender','Sexo','select','Hembra|Macho|No especificado'],['size','Tamaño','select','Pequeño|Mediano|Grande'],['location','Ubicación','text']],
  [['vaccination','Vacunación','select','Al día|Pendiente de confirmar|No vacunada'],['sterilization','Esterilización','select','Sí|Pendiente de confirmar|No'],['deworming','Desparasitación','select','Al día|Pendiente de confirmar|No'],['condition','Condición médica','text'],['healthNotes','Observaciones de salud','textarea']],
  [['personality','Personalidad y convivencia','checks','Cariñosa|Tranquila|Juguetona|Sociable|Independiente|Convive con perros|Convive con gatos|Convive con niños']],
  [['story','¿Cómo llegó a la adopción?','textarea'],['background','Antecedentes relevantes','textarea']],
  [['homeType','Tipo de hogar','text'],['conditions','Condiciones necesarias','textarea'],['experience','Experiencia recomendada','textarea'],['requirements','Otros requisitos','textarea']],
  [['photos','Fotografías','file']], []
];
const requests = computed(() => state.requests.filter(item => requestFilter.value === 'Todas' || item.status === ({ Nuevas: 'Nueva', Aceptadas: 'Aceptada' }[requestFilter.value] || requestFilter.value)));
const visibleRequests = computed(() => ['En proceso','Seguimiento'].includes(section.value) ? state.requests.filter(item => ['Aceptada','En proceso'].includes(item.status)) : requests.value);
const visiblePets = computed(() => state.pets.filter(item => petFilter.value === 'Todas' || item.status === petFilter.value));
const availablePetLocations = computed(() => [...new Set([...state.pets, ...staticPets].map(pet => String(pet.location || '').trim()).filter(location => location && location.toLocaleLowerCase('es-MX') !== 'playa'))].sort((a, b) => a.localeCompare(b, 'es-MX')));
const recentRequests = computed(() => state.requests.filter(item => ['Nueva', 'En revisión'].includes(item.status)).slice(0, 3));
const recentPets = computed(() => state.pets.slice(0, 3));
const providerPhotoFiles = { chino:'img2.jpeg', luna:'img3.jpeg', max:'img4.jpeg', kira:'img7.jpeg', milo:'img5.jpg', nala:'img6.jpg' };
function providerPetPhoto(pet) { return pet.photos?.[0] || (providerPhotoFiles[pet.image] ? `/img/${providerPhotoFiles[pet.image]}` : (String(pet.image || '').includes('.') ? `/img/${pet.image}` : '')); }
const overviewStats = computed(() => [
  { label: 'Mascotas publicadas', value: state.pets.length, icon: 'paw-print', section: 'Mis mascotas' },
  { label: 'Solicitudes nuevas', value: state.requests.filter(item => item.status === 'Nueva').length, icon: 'inbox', section: 'Solicitudes' },
  { label: 'Adopciones en proceso', value: state.requests.filter(item => ['Aceptada', 'En proceso'].includes(item.status)).length, icon: 'refresh-cw', section: 'En proceso' },
  { label: 'Adopciones completadas', value: state.pets.filter(item => item.status === 'Adoptada').length, icon: 'heart-handshake', section: 'Completadas' }
]);
const conversations = computed(() => {
  messagesRevision.value;
  const shared = globalThis.PetHopData?.providerThreads(storageService) || [];
  const legacy = state.messages.filter(message => !shared.some(item => item.id === message.id)).map(message => ({
    ...message,
    messages: (message.messages || []).map(item => ({ from: item.from || item.sender, text: item.text, date: item.date || item.createdAt }))
  }));
  return [...shared, ...legacy];
});
const activeConversation = computed(() => conversations.value.find(item => item.id === selectedMessageId.value) || conversations.value[0]);
const completion = computed(() => { const fields = stepFields.slice(0, 5).flat().map(([key]) => key); return Math.round(fields.filter(key => Array.isArray(state.draft[key]) ? state.draft[key].length : String(state.draft[key] || '').trim()).length / fields.length * 100); });
const profileCompletion = computed(() => [state.profile.name, state.profile.contact, state.profile.location, state.profile.email, state.profile.phone, state.profile.description].filter(Boolean).length);
function persist(message = '') { if (!storageService.write('pethop_provider_workspace', state)) { notice.value = 'No se pudieron guardar los cambios en este dispositivo.'; return false; } if (message) toast(message); return true; }
let toastTimer;
function toast(message) { notice.value = message; clearTimeout(toastTimer); toastTimer = setTimeout(() => notice.value = '', 3200); }
function chooseRole(role) { state.role = role; section.value = state.section = route.query.section === 'mensajes' ? 'Mensajes' : 'Inicio'; persist(); }
function go(item) { section.value = state.section = item; persist(); }
function updateStatus(pet, newStatus) {
  pet.status = newStatus;
  if (newStatus === 'Adoptada') for (const request of state.requests.filter(item => item.petId === pet.id && item.status !== 'Rechazada')) { request.status = 'Adoptada'; syncApplication(request.id, 'Adoptada'); }
  persist('Estado de publicación actualizado.'); selectedPet.value = null;
}
function openPet(pet) { selectedPet.value = pet; selectedPetStatus.value = pet.status; editingPet.value = false; }
function savePetStatus() { if (!selectedPet.value) return; updateStatus(selectedPet.value, selectedPetStatus.value); }
function editPetInformation() {
  if (!selectedPet.value) return;
  petEditForm.value = { ...selectedPet.value, personality: Array.isArray(selectedPet.value.personality) ? selectedPet.value.personality.join(', ') : (selectedPet.value.personality || '') };
  editingPet.value = true;
}
function savePetInformation() {
  if (!selectedPet.value) return;
  const updatedPet = { ...petEditForm.value };
  if (typeof updatedPet.personality === 'string') updatedPet.personality = updatedPet.personality.split(',').map(value => value.trim()).filter(Boolean);
  Object.assign(selectedPet.value, updatedPet, { status: 'En revisión' });
  if (persist('Información actualizada. La publicación quedó En revisión.')) { selectedPetStatus.value = 'En revisión'; editingPet.value = false; selectedPet.value = null; }
}
function advancePublish(event) {
  if (!event.currentTarget.reportValidity()) return;
  if (state.step < 6) { state.step += 1; persist(); return; }
  const reviewed = event.currentTarget.elements.reviewed;
  if (!reviewed?.checked) { reviewed?.focus(); return; }
  state.pets.unshift({ ...state.draft, id: `pet-${Date.now()}`, status: 'En revisión', requests: 0, days: 0, image: 'default' });
  state.draft = {}; state.step = 0; section.value = state.section = 'Mis mascotas'; persist('Perfil guardado: tu mascota quedó En revisión.');
}
function syncApplication(id, status) {
  const apps = storageService.read('pethop_applications', []); if (!Array.isArray(apps)) return;
  const app = apps.find(item => item.id === id); if (app) { app.status = status; storageService.write('pethop_applications', apps); }
}
function requestAction(action) {
  const request = selectedRequest.value; if (!request) return;
  const appStatus = { review:'En revisión', contact:'Contactada', accept:'Aprobada', reject:'Rechazada' }[action];
  request.status = { review:'En revisión', contact:'En proceso', accept:'Aceptada', reject:'Rechazada' }[action];
  syncApplication(request.id, appStatus);
  if (action === 'accept') { const pet = state.pets.find(item => item.id === request.petId); if (pet) pet.status = 'En proceso de adopción'; section.value = state.section = 'En proceso'; }
  if (action === 'contact') {
    const owner = request.adopterEmail ? request.adopterEmail.trim().toLowerCase() : `solicitud-${request.id}`;
    const id = `${owner}:${request.petId}`;
    const previous = globalThis.PetHopData?.messages(storageService).find(item => item.id === id);
    const legacyThread = state.messages.find(item => item.id === id || item.requestId === request.id);
    const oldMessages = (previous?.messages || legacyThread?.messages || []).map(item => ({ sender: item.sender || (item.from === 'provider' ? 'provider' : 'adopter'), text: item.text, createdAt: item.createdAt || item.date || new Date().toISOString() }));
    const thread = { ...(previous || {}), id, requestId: request.id, petId: request.petId, petName: request.pet, owner, adopter: request.adopter, status: request.status, messages: oldMessages };
    const saved = globalThis.PetHopData?.upsertMessageThread(storageService, thread);
    if (saved === false) { toast('No se pudo guardar la conversación en este dispositivo.'); return; }
    selectedMessageId.value = state.activeMessage = thread.id; section.value = state.section = 'Mensajes'; messagesRevision.value += 1;
  }
  if (action === 'review' || action === 'reject') section.value = state.section = 'Solicitudes';
  persist(); selectedRequest.value = null;
}
async function processPhotos(event) {
  const files = [...event.target.files];
  if (files.length > 6 || files.some(file => !file.type.startsWith('image/') || file.size > 12 * 1024 * 1024)) { toast('Selecciona hasta 6 imágenes de máximo 12 MB cada una.'); event.target.value = ''; return; }
  const convert = file => new Promise((resolve, reject) => { const reader = new FileReader(); reader.onerror = reject; reader.onload = () => { const image = new Image(); image.onerror = reject; image.onload = () => { const scale = Math.min(1, 900 / Math.max(image.naturalWidth, image.naturalHeight)); const canvas = document.createElement('canvas'); canvas.width = Math.max(1, Math.round(image.naturalWidth * scale)); canvas.height = Math.max(1, Math.round(image.naturalHeight * scale)); canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height); resolve(canvas.toDataURL('image/jpeg', .76)); }; image.src = reader.result; }; reader.readAsDataURL(file); });
  try { state.draft.photos = await Promise.all(files.map(convert)); state.draft.photoNames = files.map(file => file.name); persist(); } catch { toast('No se pudieron procesar las fotos. Intenta con otras imágenes.'); }
}
function saveMessage(event) {
  const text = event.currentTarget.elements.message.value.trim(); const conversation = activeConversation.value;
  if (!text || !conversation) return;
  const createdAt = new Date().toISOString();
  const isShared = globalThis.PetHopData?.messages(storageService).some(item => item.id === conversation.id);
  let ok = false;
  if (isShared) ok = globalThis.PetHopData.appendMessage(storageService, conversation.id, { sender: 'provider', text, createdAt });
  else {
    const thread = state.messages.find(item => item.id === conversation.id);
    if (thread) { thread.messages ||= []; thread.messages.push({ from: 'provider', text, date: createdAt }); ok = persist(); }
  }
  if (ok) { event.currentTarget.reset(); messagesRevision.value += 1; }
  else toast('No se pudo guardar el mensaje en este dispositivo.');
}
function saveProfile() { persist('Perfil guardado correctamente.'); }
function resetDemo() { if (!confirm('¿Restablecer mascotas y datos de demostración? Las solicitudes reales y conversaciones guardadas se conservarán.')) return; state.pets = [...PETS]; state.requests = state.requests.filter(item => !String(item.id).startsWith('req-')).concat(DEMO_REQUESTS); section.value = state.section = 'Inicio'; persist(); }
watch(section, value => { document.body.classList.toggle('provider-dashboard-active', Boolean(state.role)); document.title = `${value} | PetHop`; }, { immediate: true });
watch(() => route.query.section, value => { if (value === 'mensajes') go('Mensajes'); });
watch(sidebarOpen, value => document.body.classList.toggle('provider-sidebar-open', value));
watch(narrowScreen, narrow => { if (!narrow) sidebarOpen.value = false; });
onMounted(() => {
  sidebarMediaQuery = window.matchMedia('(max-width: 760px)');
  updateScreenWidth(sidebarMediaQuery);
  sidebarMediaQuery.addEventListener('change', updateScreenWidth);
});
onUnmounted(() => { sidebarMediaQuery?.removeEventListener('change', updateScreenWidth); document.body.classList.remove('provider-dashboard-active', 'provider-sidebar-open'); });
watch(() => state.draft, () => { storageService.write('pethop_provider_workspace', state); }, { deep: true });
watch(requestFilter, value => { state.filter = value; storageService.write('pethop_provider_workspace', state); });
watch(petFilter, value => { state.petFilter = value; storageService.write('pethop_provider_workspace', state); });
watch(() => state.step, () => { storageService.write('pethop_provider_workspace', state); });
</script>

<template>
 <div class="provider-page" :class="{ 'provider-dashboard-active': Boolean(state.role) }" @keydown.esc.window="sidebarOpen=false">
   <SiteHeader />
   <main id="provider-app" class="provider-main" aria-live="polite">
   <section v-if="!state.role" class="provider-entry"><div class="provider-entry-copy"><span class="eyebrow">PET HOP · ADOPCIÓN RESPONSABLE</span><h1>Una adopción responsable empieza con una buena conexión.</h1><p>Organiza publicaciones, solicitudes y seguimientos desde un solo lugar, seas una persona particular o una organización.</p></div><div class="provider-entry-card"><span class="provider-entry-icon"><Icon name="heart-handshake" /></span><h2>¿Cómo quieres usar PetHop?</h2><div class="provider-choice-grid"><button type="button" class="provider-choice" @click="router.push('/explorar.html')"><span><Icon name="search" /></span><strong>Quiero adoptar</strong><small>Descubrir mascotas y seguir mis solicitudes.</small></button><button type="button" class="provider-choice" @click="entryChoosingRole = true"><span><Icon name="paw-print" /></span><strong>Quiero dar en adopción</strong><small>Publicar y acompañar adopciones responsables.</small></button></div><div v-if="entryChoosingRole" class="provider-role-picker"><h3>¿Quién dará en adopción?</h3><div class="provider-choice-grid"><button class="provider-choice" @click="chooseRole('particular')"><span><Icon name="user-round" /></span><strong>Soy una persona particular</strong><small>Gestiona las mascotas que estás ayudando.</small></button><button class="provider-choice" @click="chooseRole('organization')"><span><Icon name="building-2" /></span><strong>Soy un refugio u organización</strong><small>Coordina tu equipo y tus publicaciones.</small></button></div></div></div></section>
   <div v-else class="provider-workspace"><button v-if="sidebarOpen" type="button" class="provider-sidebar-backdrop" aria-label="Cerrar navegación del panel" @click="sidebarOpen=false"></button><aside id="provider-sidebar" class="provider-sidebar" :class="{ 'is-open': sidebarOpen }" :inert="narrowScreen && !sidebarOpen" :aria-hidden="narrowScreen && !sidebarOpen"><div class="provider-account-badge"><Icon :name="state.role === 'organization' ? 'building-2' : 'user-round'" /><span>{{ state.profile.name || (state.role === 'organization' ? 'Mi organización' : 'Mi perfil') }}</span><small>{{ state.role === 'organization' ? 'Organización' : 'Particular' }}</small></div><nav aria-label="Panel de proveedor"><button v-for="item in NAV_ITEMS" :key="item.label" type="button" class="provider-nav-item" :class="{ 'is-active': section === item.label }" :aria-current="section === item.label ? 'page' : undefined" @click="go(item.label);sidebarOpen=false"><Icon :name="item.icon" /><span>{{ item.label }}</span><b v-if="item.label === 'Solicitudes'">{{ state.requests.filter(request => request.status === 'Nueva').length }}</b></button></nav><RouterLink to="/" class="provider-back-home"><Icon name="arrow-left" />Volver a PetHop</RouterLink></aside>
    <section class="provider-content"><button class="provider-mobile-menu" type="button" aria-controls="provider-sidebar" :aria-expanded="sidebarOpen" :aria-label="sidebarOpen ? 'Cerrar navegación del panel' : 'Abrir navegación del panel'" @click="sidebarOpen=!sidebarOpen"><Icon :name="sidebarOpen ? 'x' : 'menu'" /></button>
      <template v-if="section === 'Inicio'">
        <div class="provider-section-header">
          <div><span class="eyebrow">PANEL DE ADOPCIONES</span><h1>{{ 'Hola' + (state.profile.name ? ', ' + state.profile.name.split(' ')[0] : '') }}</h1><p>Aquí tienes un resumen de tus adopciones y lo que requiere tu atención.</p></div>
          <button type="button" class="button button-primary" @click="go('Publicar mascota')"><Icon name="plus" /> Publicar mascota</button>
        </div>
        <div class="provider-stat-grid">
          <button v-for="item in overviewStats" :key="item.label" type="button" class="provider-stat-card" @click="go(item.section)"><span><Icon :name="item.icon" /></span><small>{{ item.label }}</small><strong>{{ item.value }}</strong></button>
        </div>
        <div class="provider-home-grid">
          <section class="provider-panel">
            <div class="provider-panel-title"><div><span class="eyebrow">ACTIVIDAD RECIENTE</span><h2>Solicitudes por revisar</h2></div><button type="button" class="provider-text-action" @click="go('Solicitudes')">Ver todas <Icon name="arrow-up-right" /></button></div>
            <button v-for="item in recentRequests" :key="item.id" type="button" class="provider-request-row" @click="selectedRequest=item"><span class="provider-request-avatar">{{ item.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span class="provider-request-copy"><strong>{{ item.adopter }}</strong><small>{{ item.pet }} · {{ new Date(item.date).toLocaleDateString('es-MX') }}</small></span><span class="provider-status">{{ item.status }}</span><Icon name="chevron-right" /></button>
            <div v-if="!recentRequests.length" class="provider-empty"><Icon name="inbox" /><strong>Todo al día</strong><p>No hay solicitudes por revisar.</p></div>
          </section>
          <section class="provider-panel provider-journey"><span class="eyebrow">EL PROCESO</span><h2>De la primera consulta al nuevo hogar</h2><div class="provider-mini-timeline"><div v-for="(step,index) in ['Solicitud','Evaluación','Comunicación','Encuentro','Seguimiento']" :key="step"><span>{{ index+1 }}</span><small>{{ step }}</small></div></div><button type="button" class="button button-outline" @click="go('Seguimiento')">Ver seguimiento <Icon name="arrow-right" /></button></section>
        </div>
        <section class="provider-panel"><div class="provider-panel-title"><div><span class="eyebrow">TUS PUBLICACIONES</span><h2>Mascotas recientes</h2></div><button type="button" class="provider-text-action" @click="go('Mis mascotas')">Ver todas <Icon name="arrow-up-right" /></button></div><div class="provider-pet-grid"><article v-for="pet in recentPets" :key="pet.id" class="provider-pet-card" tabindex="0" role="button" :aria-label="'Administrar publicación de ' + pet.name" @click="openPet(pet)" @keydown.enter="openPet(pet)" @keydown.space.prevent="openPet(pet)"><ImageWithSkeleton :frame-class="['provider-pet-photo', 'provider-photo-' + (pet.image || 'default')]" :frame-style="pet.photos?.[0] ? { backgroundImage: 'url(' + pet.photos[0] + ')' } : undefined" :src="providerPetPhoto(pet)" :alt="`Fotografía de ${pet.name}`" role="img" /><div class="provider-pet-card-body"><div class="provider-card-line"><span class="provider-status">{{ pet.status }}</span><small>{{ pet.days || 0 }} días publicada</small></div><h3>{{ pet.name }}</h3><p>{{ pet.species }} · {{ pet.age }} · {{ pet.gender }}</p><div class="provider-card-line provider-pet-request-count"><span><Icon name="inbox" /> {{ pet.requests || 0 }} solicitudes</span></div></div></article><div v-if="!recentPets.length" class="provider-empty"><Icon name="paw-print" /><strong>Sin publicaciones</strong><p>Publica una mascota para comenzar a organizar sus solicitudes.</p></div></div></section>
      </template>
      <template v-else-if="section === 'Mis mascotas' || section === 'Completadas'"><div class="provider-section-header"><div><span class="eyebrow">TUS PUBLICACIONES</span><h1>{{ section }}</h1><p>Consulta su estado, las solicitudes recibidas y el tiempo de publicación.</p></div><button class="button button-primary" @click="go('Publicar mascota')"><Icon name="plus" /> Publicar mascota</button></div><div v-if="section==='Mis mascotas'" class="provider-filter-pills"><button v-for="filter in ['Todas','Publicada','En revisión','En proceso de adopción','Adoptada','Pausada']" :key="filter" :class="{ 'is-selected': petFilter===filter }" @click="petFilter=filter">{{ filter }}</button></div><div class="provider-pet-grid"><article v-for="pet in (section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets)" :key="pet.id" class="provider-pet-card" tabindex="0" role="button" :aria-label="`Ver detalles de ${pet.name}`" @click="openPet(pet)" @keydown.enter="openPet(pet)" @keydown.space.prevent="openPet(pet)"><ImageWithSkeleton :frame-class="['provider-pet-photo', 'provider-photo-' + (pet.image || 'default')]" :frame-style="pet.photos?.[0] ? { backgroundImage: 'url(' + pet.photos[0] + ')' } : undefined" :src="providerPetPhoto(pet)" :alt="`Fotografía de ${pet.name}`" role="img" /><div class="provider-pet-card-body"><span class="provider-status">{{ pet.status }}</span><h3>{{ pet.name }}</h3><p>{{ pet.species }} · {{ pet.age }} · {{ pet.gender }}</p><p>{{ pet.days || 0 }} días publicada · {{ pet.requests || 0 }} solicitudes</p></div></article><p v-if="!(section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets).length" class="provider-empty">{{ section==='Completadas' ? 'Aquí veremos las adopciones completadas.' : 'No hay mascotas en este estado. Prueba otro filtro o publica una mascota.' }}</p></div></template>
      <template v-else-if="section === 'Publicar mascota'"><div class="provider-section-header"><div><span class="eyebrow">NUEVA PUBLICACIÓN</span><h1>Publicar mascota</h1><p>Completa la información paso a paso para presentar su perfil de forma clara.</p></div></div><section class="provider-panel provider-publish-panel"><div class="provider-steps"><button v-for="(label,index) in steps" :key="label" type="button" class="provider-step" :class="{ 'is-current':state.step===index,'is-done':index<state.step }" :disabled="index>state.step" @click="state.step=index"><span><Icon v-if="index<state.step" name="check" /><template v-else>{{ String(index+1).padStart(2,'0') }}</template></span><small>{{ label }}</small></button></div><div class="provider-progress"><span :style="{ width: `${completion}%` }"></span></div><form class="provider-form-grid" @submit.prevent="advancePublish"><h2>{{ steps[state.step] }}</h2><p class="provider-step-hint">Paso {{ state.step+1 }} de {{ steps.length }}</p><template v-if="state.step===6"><div class="provider-review-card"><div class="provider-review-progress"><div><strong>Información del perfil: {{ completion }}% completa</strong><p>Este porcentaje refleja qué tan completa está la información proporcionada.</p></div></div><div class="provider-review-pet"><img v-if="state.draft.photos?.[0]" :src="state.draft.photos[0]" :alt="`Fotografía de ${state.draft.name}`"><div><h3>{{ state.draft.name || 'Nombre de la mascota' }}</h3><p>{{ state.draft.species || 'Especie por agregar' }} · {{ state.draft.age || 'Edad por agregar' }} · {{ state.draft.location || 'Ubicación por agregar' }}</p><p>Revisa los datos antes de publicar. Podrás actualizarlos después.</p></div></div></div></template><template v-else><template v-for="field in stepFields[state.step]" :key="field[0]"><label v-if="field[2]==='text'" class="provider-field">{{ field[1] }}<input v-model="state.draft[field[0]]" :required="['name','location'].includes(field[0])"></label><label v-else-if="field[2]==='select'" class="provider-field">{{ field[1] }}<select v-model="state.draft[field[0]]" :required="field[0]==='species'"><option value="">Selecciona</option><option v-for="option in field[3].split('|')" :key="option">{{ option }}</option></select></label><label v-else-if="field[2]==='textarea'" class="provider-field">{{ field[1] }}<textarea v-model="state.draft[field[0]]" rows="3"></textarea></label><fieldset v-else-if="field[2]==='checks'" class="provider-field provider-checks"><legend>{{ field[1] }}</legend><label v-for="option in field[3].split('|')" :key="option"><input type="checkbox" :value="option" v-model="state.draft[field[0]]"><span>{{ option }}</span></label></fieldset><label v-else-if="field[2]==='file'" class="provider-field provider-photo-field">Fotografías<input type="file" accept="image/*" multiple @change="processPhotos"><small>Hasta 6 fotos de 12 MB. Se optimizan y guardan localmente.</small><span class="provider-photo-preview"><img v-for="(photo,index) in state.draft.photos" :key="index" :src="photo" :alt="`Vista previa ${index+1}`"></span></label></template></template><label v-if="state.step===6" class="provider-ack"><input name="reviewed" type="checkbox" required> Confirmo que la información es correcta y puedo responder las solicitudes de adopción.</label><div class="provider-form-actions"><button v-if="state.step>0" type="button" class="button button-outline" @click="state.step--"><Icon name="arrow-left" /> Anterior</button><span v-else></span><button type="submit" class="button button-primary"><template v-if="state.step<6">Continuar <Icon name="arrow-right" /></template><template v-else>Publicar mascota</template></button></div></form></section></template>
      <template v-else-if="['Solicitudes','En proceso','Seguimiento'].includes(section)"><div class="provider-section-header"><div><span class="eyebrow">{{ section==='Seguimiento'?'ACOMPAÑAMIENTO':'EVALUACIÓN RESPONSABLE' }}</span><h1>{{ section === 'Solicitudes' ? 'Solicitudes de adopción' : section==='Seguimiento'?'Seguimiento de adopción':section }}</h1><p>{{ section==='Seguimiento'?'Adoptante y persona que da en adopción comparten las etapas del mismo proceso.':'Conoce a cada familia y continúa el proceso con calma.' }}</p></div></div><template v-if="section==='Solicitudes'"><div class="provider-filter-pills"><button v-for="filter in ['Todas','Nuevas','En revisión','Aceptadas','Rechazadas']" :key="filter" :class="{ 'is-selected':requestFilter===filter }" @click="requestFilter=filter">{{ filter }}<small>{{ filter==='Todas'?state.requests.length:state.requests.filter(item=>item.status===({Nuevas:'Nueva',Aceptadas:'Aceptada'}[filter]||filter)).length }}</small></button></div><section class="provider-panel"><button v-for="item in visibleRequests" :key="item.id" class="provider-request-row" @click="selectedRequest=item"><span class="provider-request-avatar">{{ item.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ item.adopter }}</strong><small>{{ item.pet }} · {{ new Date(item.date).toLocaleDateString('es-MX') }}</small></span><span class="provider-status">{{ item.status }}</span><Icon name="chevron-right" /></button><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="clipboard-check" /><strong>No hay solicitudes</strong><p>Las solicitudes aparecerán aquí al llegar.</p></div></section></template><section v-else-if="section==='En proceso'" class="provider-panel provider-pipeline-list"><article v-for="item in visibleRequests" :key="item.id" class="provider-pipeline-card"><div><span class="provider-status">{{ item.status==='Aceptada'?'Evaluación aceptada':'En proceso' }}</span><h3>{{ item.pet }} · {{ item.adopter }}</h3><p>Próximo paso: acordar el encuentro y preparar la adopción.</p></div><button class="button button-outline" @click="selectedRequest=item">Ver solicitud <Icon name="arrow-right" /></button></article><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="refresh-cw" /><strong>Aún no hay adopciones en curso</strong><p>Al aceptar una solicitud, podrás coordinar la evaluación y el encuentro.</p></div></section><section v-else class="provider-panel"><article v-for="item in visibleRequests" :key="item.id" class="provider-track-card"><div class="provider-track-heading"><div><span class="provider-status">{{ item.status }}</span><h2>{{ item.pet }} · {{ item.adopter }}</h2></div><button type="button" class="provider-text-action" @click="selectedRequest=item">Abrir solicitud <Icon name="arrow-right" /></button></div><ol class="provider-timeline"><li v-for="(step,index) in trackingSteps" :key="step" :class="{ 'is-complete':index <= (item.status==='Aceptada'?2:3) }"><span><Icon v-if="index <= (item.status==='Aceptada'?2:3)" name="check" /><span v-else>{{ index+1 }}</span></span><div><strong>{{ step }}</strong><small v-if="index===0">{{ new Date(item.date).toLocaleDateString('es-MX') }}</small></div></li></ol></article><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="route" /><strong>Sin seguimientos activos</strong><p>Los seguimientos de solicitudes aceptadas se organizarán aquí.</p></div></section></template>
      <template v-else-if="section==='Mensajes'"><div class="provider-section-header"><div><span class="eyebrow">COMUNICACIÓN</span><h1>Mensajes</h1><p>Conversaciones sencillas vinculadas a una mascota y su solicitud.</p></div></div><div class="provider-messages-layout"><section class="provider-panel provider-conversation-list"><h2>Conversaciones</h2><button v-for="thread in conversations" :key="thread.id" class="provider-conversation" :class="{ 'is-selected':(activeConversation?.id===thread.id) }" @click="selectedMessageId=state.activeMessage=thread.id;persist()"><span class="provider-request-avatar">{{ thread.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ thread.adopter }}</strong><small>{{ thread.pet }} · {{ thread.status }}</small></span></button><div v-if="!conversations.length" class="provider-empty"><Icon name="messages-square" /><strong>Sin conversaciones</strong><p>Al contactar a una familia podrás continuar la conversación aquí.</p></div></section><section class="provider-panel provider-chat-panel"><template v-if="activeConversation"><div class="provider-chat-head"><span class="provider-request-avatar">{{ activeConversation.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><div><strong>{{ activeConversation.adopter }}</strong><small>{{ activeConversation.pet }} · {{ activeConversation.status }}</small></div></div><div class="provider-chat-transcript" aria-live="polite"><p v-for="(message,index) in activeConversation.messages" :key="index" :class="{ 'is-outgoing':message.from==='provider' }">{{ message.text }}<small>{{ new Date(message.date).toLocaleString('es-MX') }}</small></p><p v-if="!activeConversation.messages.length" class="provider-chat-note">Inicia la conversación con {{ activeConversation.adopter }} sobre {{ activeConversation.pet }}.</p></div><form class="provider-chat-form" @submit.prevent="saveMessage"><label class="sr-only" for="provider-message">Escribe un mensaje</label><textarea id="provider-message" name="message" rows="2" placeholder="Escribe un mensaje..." required></textarea><button class="button button-primary">Enviar</button></form></template><div v-else class="provider-chat-empty"><Icon name="messages-square" /><h3>Comunicación clara, adopciones responsables</h3><p>Selecciona una conversación para enviar un mensaje sobre la mascota.</p></div></section></div></template>
      <template v-else-if="section==='Perfil / Organización'">
        <div class="provider-section-header"><div><span class="eyebrow">TU IDENTIDAD EN PETHOP</span><h1>{{ state.role==='organization'?'Perfil de organización':'Perfil de persona particular' }}</h1><p>Una presentación clara genera confianza con las familias adoptantes.</p></div></div>
        <div class="provider-profile-layout">
          <section class="provider-panel">
            <div class="provider-verification" :class="{ 'is-verified': state.profile.verified }"><Icon :name="state.profile.verified ? 'badge-check' : 'shield-check'" /><div><strong>{{ state.profile.verified ? (state.role==='organization'?'Organización verificada':'Identidad verificada') : 'Verificación pendiente' }}</strong><p>{{ state.profile.verified ? 'Tu información está verificada en este prototipo.' : 'Puedes usar PetHop normalmente mientras completas la verificación.' }}</p></div><span>{{ profileCompletion }}/6 datos</span></div>
            <form class="provider-form-grid" @submit.prevent="saveProfile">
              <label class="provider-field">{{ state.role==='organization'?'Nombre de la organización':'Nombre' }}<input v-model="state.profile.name" :placeholder="state.role==='organization'?'Nombre del refugio u organización':'Tu nombre'" required></label>
              <label v-if="state.role==='organization'" class="provider-field">Descripción<textarea v-model="state.profile.description" rows="3"></textarea></label>
              <label class="provider-field">Persona de contacto<input v-model="state.profile.contact" placeholder="Nombre de contacto"></label>
              <label class="provider-field">Ubicación general<input v-model="state.profile.location" placeholder="Ciudad o municipio"></label>
              <label class="provider-field">Correo electrónico<input v-model="state.profile.email" type="email" placeholder="correo@ejemplo.com"></label>
              <label class="provider-field">Teléfono<input v-model="state.profile.phone" placeholder="Teléfono de contacto"></label>
              <div class="provider-profile-summary"><span><Icon name="paw-print" /></span><div><strong>{{ state.pets.length }} mascotas {{ state.role==='organization'?'disponibles':'publicadas' }}</strong><small>Las publicaciones están vinculadas a este perfil.</small></div></div>
              <div class="provider-form-actions"><button type="submit" class="button button-primary">Guardar perfil <Icon name="check" /></button></div>
            </form>
          </section>
          <aside class="provider-panel provider-profile-aside"><span class="provider-aside-icon"><Icon :name="state.role==='organization'?'building-2':'user-round'" /></span><h3>{{ state.role==='organization'?'Representa a tu organización':'Acompaña cada adopción' }}</h3><p>La verificación ayuda a crear confianza. No limita el acceso a las herramientas de PetHop.</p><button type="button" class="button button-outline" @click="state.profile.verificationRequested=true;persist('Solicitud de verificación registrada en este prototipo.')"><Icon name="shield-check" /> Solicitar verificación</button></aside>
        </div>
      </template>
      <template v-else-if="section==='Configuración'"><div class="provider-section-header"><div><span class="eyebrow">PREFERENCIAS</span><h1>Configuración</h1><p>Ajusta la experiencia de tu espacio de adopciones.</p></div></div><section class="provider-panel provider-settings"><label class="provider-setting-row"><span><strong>Notificaciones de nuevas solicitudes</strong><small>Preferencia local del panel.</small></span><input v-model="state.notify" type="checkbox" @change="persist()"></label><label class="provider-setting-row"><span><strong>Mostrar perfil como verificado</strong><small>Representación de demostración; no es una validación oficial.</small></span><input v-model="state.profile.verified" type="checkbox" @change="persist()"></label><button type="button" class="button button-outline" @click="resetDemo"><Icon name="rotate-ccw" /> Restablecer datos de demostración</button><p class="provider-data-note">La información se guarda únicamente en este navegador.</p></section></template>
      <template v-else><div class="provider-section-header"><div><span class="eyebrow">CENTRO DE AYUDA</span><h1>¿Cómo podemos ayudarte?</h1><p>Guía rápida para organizar una adopción responsable.</p></div></div><div class="provider-help-grid"><article v-for="[number,title,copy] in [['01','Publica con claridad','Completa salud, personalidad, historia y requisitos para orientar a las familias.'],['02','Evalúa cada solicitud','Revisa las condiciones del hogar y conversa con el adoptante antes de decidir.'],['03','Coordina el encuentro','Usa el seguimiento para preparar la entrega y continuar en contacto después.'],['04','Cuida tus datos','En este prototipo la información se guarda en este navegador y no se envía a un servidor.']]" :key="number" class="provider-help-card"><span>{{ number }}</span><h2>{{ title }}</h2><p>{{ copy }}</p></article></div></template>
    </section></div>
   </main>
   <div v-if="selectedPet" class="provider-modal-backdrop provider-pet-edit-backdrop" @click.self="selectedPet=null"><section class="provider-request-modal provider-pet-details-modal" role="dialog" aria-modal="true" aria-labelledby="pet-details-title"><button type="button" class="provider-modal-close" aria-label="Cerrar" @click="selectedPet=null; editingPet=false"><Icon name="x" /></button><span class="eyebrow">{{ editingPet ? 'EDITAR PUBLICACIÓN' : 'DETALLES DE PUBLICACIÓN' }}</span><div class="provider-pet-modal-title"><h2 id="pet-details-title">{{ editingPet ? 'Editar información' : selectedPet.name }}</h2><span class="provider-status" :class="{ 'provider-status-review': editingPet || selectedPet.status === 'En revisión' }">{{ editingPet ? 'En revisión al guardar' : selectedPet.status }}</span></div><p class="provider-request-lead">{{ editingPet ? `Actualiza el perfil de ${selectedPet.name}.` : `${selectedPet.species} · ${selectedPet.age} · ${selectedPet.gender} · ${selectedPet.location || 'Ubicación por confirmar'}` }}</p>
     <template v-if="editingPet">
       <form class="provider-pet-edit-form" @submit.prevent="savePetInformation">
         <div class="provider-pet-edit-grid">
           <label class="provider-field">Nombre<input v-model="petEditForm.name" required></label>
           <label class="provider-field">Especie<select v-model="petEditForm.species" required><option>Perro</option><option>Gato</option><option>Otro</option></select></label>
           <label class="provider-field">Edad<select v-model="petEditForm.age"><option value="">Por confirmar</option><option>Cachorro</option><option>Joven</option><option>Adulto</option><option>Senior</option></select></label>
           <label class="provider-field">Sexo<select v-model="petEditForm.gender"><option value="">Por confirmar</option><option>Hembra</option><option>Macho</option><option>No especificado</option></select></label>
           <label class="provider-field">Tamaño<select v-model="petEditForm.size"><option value="">Por confirmar</option><option>Pequeño</option><option>Mediano</option><option>Grande</option></select></label>
           <label class="provider-field">Ubicación<select v-model="petEditForm.location" required><option value="" disabled>Selecciona una ubicación</option><option v-for="location in availablePetLocations" :key="location" :value="location">{{ location }}</option></select></label>
           <label class="provider-field">Vacunación<input v-model="petEditForm.vaccination"></label>
           <label class="provider-field">Desparasitación<input v-model="petEditForm.deworming"></label>
           <label class="provider-field">Esterilización<input v-model="petEditForm.sterilization"></label>
           <label class="provider-field">Personalidad<textarea v-model="petEditForm.personality" rows="2" placeholder="Separa las características con comas"></textarea></label>
           <label class="provider-field provider-pet-edit-wide">Descripción<textarea v-model="petEditForm.description" rows="3"></textarea></label>
           <label class="provider-field provider-pet-edit-wide">Información de salud<textarea v-model="petEditForm.healthNotes" rows="3"></textarea></label>
           <label class="provider-field provider-pet-edit-wide">Historia<textarea v-model="petEditForm.story" rows="3"></textarea></label>
           <label class="provider-field provider-pet-edit-wide">Requisitos de adopción<textarea v-model="petEditForm.requirements" rows="3"></textarea></label>
         </div>
         <div class="provider-modal-actions provider-pet-edit-actions"><button type="button" class="button button-outline" @click="editingPet=false">Cancelar</button><button type="submit" class="button button-primary"><Icon name="check" /> Guardar cambios</button></div>
         <p class="provider-modal-note provider-pet-review-note"><Icon name="shield-check" /> Al guardar, la publicación pasará a En revisión mientras se verifican los cambios.</p>
       </form>
     </template>
     <template v-else>
       <div class="provider-applicant-grid"><article><small>Estado de adopción</small><p>{{ selectedPet.status }}</p></article><article><small>Actividad</small><p>{{ selectedPet.requests || 0 }} solicitudes · {{ selectedPet.days || 0 }} días publicada</p></article></div><label class="provider-field">Actualizar estado<select v-model="selectedPetStatus"><option v-for="status in ['Publicada','En revisión','En proceso de adopción','Adoptada','Pausada']" :key="status">{{ status }}</option></select></label><p class="provider-modal-note">Al marcarla como adoptada, las solicitudes activas asociadas también se reflejarán como completadas.</p><div class="provider-modal-actions provider-pet-details-actions"><button type="button" class="button button-outline" @click="selectedPet=null">Cerrar</button><button type="button" class="button button-outline" @click="editPetInformation">Editar información</button><button type="button" class="button button-primary" @click="savePetStatus">Guardar estado</button></div>
     </template>
   </section></div>
   <div v-if="selectedRequest" class="provider-modal-backdrop" @click.self="selectedRequest=null"><section class="provider-request-modal" role="dialog" aria-modal="true" aria-labelledby="request-title"><button class="provider-modal-close" aria-label="Cerrar" @click="selectedRequest=null"><Icon name="x" /></button><span class="eyebrow">SOLICITUD DE ADOPCIÓN</span><h2 id="request-title">{{ selectedRequest.adopter }} <span class="provider-status">{{ selectedRequest.status }}</span></h2><p class="provider-request-lead">Solicitud para <strong>{{ selectedRequest.pet }}</strong> · {{ new Date(selectedRequest.date).toLocaleDateString('es-MX') }}</p><div class="provider-applicant-grid"><article v-for="[label,value] in [['Tipo de vivienda',selectedRequest.home],['Experiencia con mascotas',selectedRequest.experience],['Personas en el hogar',selectedRequest.household],['Motivo de adopción',selectedRequest.reason],['Condiciones del hogar',selectedRequest.conditions],['Información relevante',selectedRequest.notes]]" :key="label"><small>{{ label }}</small><p>{{ value || 'No proporcionado' }}</p></article></div><div class="provider-modal-actions"><button class="button button-outline" @click="requestAction('contact')">Contactar</button><button class="button button-outline" @click="requestAction('review')">Continuar revisión</button><button class="button button-primary" @click="requestAction('accept')">Aceptar solicitud</button><button class="button button-danger" @click="requestAction('reject')">Rechazar</button></div><p class="provider-modal-note">Aceptar inicia la coordinación; la adopción se completa después del encuentro y seguimiento.</p></section></div>
   <div v-if="notice" class="provider-toast is-visible" role="status">{{ notice }}</div>
 </div>
</template>

<style scoped>
.provider-pet-details-modal { width:min(820px,100%); padding:34px; border:1px solid #dcebed; border-radius:24px; }
.provider-pet-edit-backdrop { display:flex; align-items:flex-start; justify-content:center; overflow-y:auto; padding:20px; }
.provider-pet-edit-backdrop .provider-pet-details-modal { flex:0 0 auto; max-height:calc(100dvh - 40px); margin:0 auto; }
.provider-pet-modal-title { display:flex; align-items:center; justify-content:space-between; gap:14px; margin:9px 42px 2px 0; }
.provider-pet-modal-title h2 { margin:0; }
.provider-pet-modal-title .provider-status { flex:0 0 auto; }
.provider-pet-details-modal .provider-request-lead { margin-top:5px; }
.provider-pet-details-modal .provider-applicant-grid { margin:20px 0; }
.provider-pet-details-modal .provider-applicant-grid article { padding:15px; border-color:#dcebed; background:#f7fcfc; }
.provider-pet-details-modal .provider-field { gap:7px; }
.provider-pet-details-modal .provider-field :deep(input),.provider-pet-details-modal .provider-field :deep(select),.provider-pet-details-modal .provider-field :deep(textarea) { min-height:44px; border-color:#c5e1e4; background:#fbfefe; color:var(--text); font:500 .88rem "Montserrat",Arial,sans-serif; }
.provider-pet-edit-form { margin-top:19px; }
.provider-pet-edit-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
.provider-pet-edit-wide { grid-column:1/-1; }
.provider-pet-edit-grid textarea { resize:vertical; }
.provider-pet-edit-actions { justify-content:flex-end; margin-top:18px; }
.provider-pet-details-actions { align-items:center; margin-top:18px; }
.provider-pet-details-actions .button:nth-child(2) { margin-right:auto; }
.provider-pet-review-note { display:flex; align-items:flex-start; gap:7px; margin-top:12px; }
.provider-pet-review-note :deep(svg) { flex:0 0 17px; width:17px; height:17px; color:var(--primary-strong); }
@media(max-width:600px) {
  .provider-pet-details-modal { max-height:calc(100dvh - 24px); padding:26px 18px 20px; border-radius:18px; }
  .provider-pet-modal-title { align-items:flex-start; flex-direction:column; gap:7px; margin-right:38px; }
  .provider-pet-edit-grid { grid-template-columns:1fr; gap:11px; }
  .provider-pet-edit-wide { grid-column:auto; }
  .provider-pet-edit-actions { display:grid; grid-template-columns:1fr 1fr; }
  .provider-pet-edit-actions .button { width:100%; justify-content:center; }
  .provider-pet-details-actions { display:grid; grid-template-columns:1fr 1fr; }
  .provider-pet-details-actions .button { width:100%; justify-content:center; }
  .provider-pet-details-actions .button:nth-child(2) { margin-right:0; }
  .provider-pet-details-actions .button:last-child { grid-column:1/-1; }
}
</style>
