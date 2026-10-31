<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import ThemeToggle from '../components/ThemeToggle.vue';
import { storageService } from '../services/storage.js';
import { API_BASE, api } from '../services/api.js';
import { mapPet } from '../services/petApi.js';

const router = useRouter();
const route = useRoute();
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
  return { ...stored, role: '', section: stored.section || 'Inicio', pets: [], requests: [], messages: [], profile: {}, draft: stored.draft || {}, step: stored.step || 0, filter: stored.filter || 'Todas', petFilter: stored.petFilter || 'Todas', notify: stored.notify ?? true };
}
const state = reactive(initialState());
const entryChoosingRole = ref(false); const section = ref(route.query.section === 'mensajes' ? 'Mensajes' : route.query.section === 'perfil' ? 'Perfil / Organización' : state.section); const notice = ref('');
const inbox = ref([]); const currentProviderId = ref('');
if (route.query.section === 'mensajes') state.section = 'Mensajes';
else if (route.query.section === 'perfil') state.section = 'Perfil / Organización';
const sidebarOpen = ref(false);
const narrowScreen = ref(false);
let sidebarMediaQuery;
function updateScreenWidth(event) { narrowScreen.value = event.matches; }
const requestFilter = ref(state.filter); const petFilter = ref(state.petFilter);
const selectedRequest = ref(null); const selectedPet = ref(null); const selectedPetStatus = ref(''); const editingPet = ref(false); const petEditForm = ref({}); const selectedMessageId = ref(state.activeMessage || '');
const savingPetChanges = ref(false);
const messagesRevision = ref(0);
const steps = ['Información básica','Salud','Personalidad','Historia','Requisitos','Fotografías','Revisión'];
const trackingSteps = ['Solicitud recibida','Solicitud en revisión','Contacto realizado','Adopción en preparación','Entrega / encuentro','Seguimiento inicial','Adopción completada'];
const stepFields = [
  [['name','Nombre','text'],['species','Especie','select','Perro|Gato|Otro'],['age','Edad','select','Cachorro|Joven|Adulto|Senior'],['gender','Sexo','select','Hembra|Macho|No especificado'],['size','Tamaño','select','Pequeño|Mediano|Grande'],['location','Ubicación','location']],
  [['vaccination','Vacunación','select','Al día|Pendiente de confirmar|No vacunada'],['sterilization','Esterilización','select','Sí|Pendiente de confirmar|No'],['deworming','Desparasitación','select','Al día|Pendiente de confirmar|No'],['condition','Condición médica','text'],['healthNotes','Observaciones de salud','textarea']],
  [['personality','Personalidad y convivencia','checks','Cariñosa|Tranquila|Juguetona|Sociable|Independiente|Convive con perros|Convive con gatos|Convive con niños']],
  [['story','¿Cómo llegó a la adopción?','textarea'],['background','Antecedentes relevantes','textarea']],
  [['homeType','Tipo de hogar','text'],['conditions','Condiciones necesarias','textarea'],['experience','Experiencia recomendada','textarea'],['requirements','Otros requisitos','textarea']],
  [['photos','Fotografías','file']], []
];
const requests = computed(() => state.requests.filter(item => requestFilter.value === 'Todas' || item.status === ({ Nuevas: 'Nueva', Aceptadas: 'Aceptada' }[requestFilter.value] || requestFilter.value)));
const visibleRequests = computed(() => ['En proceso','Seguimiento'].includes(section.value) ? state.requests.filter(item => ['Aceptada','En proceso'].includes(item.status)) : requests.value);
const visiblePets = computed(() => state.pets.filter(item => petFilter.value === 'Todas' || item.status === petFilter.value));
const availablePetLocations = computed(() => [...new Set(state.pets.map(pet => String(pet.location || '').trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'es-MX')));
const recentRequests = computed(() => state.requests.filter(item => ['Nueva', 'En revisión'].includes(item.status)).slice(0, 3));
const recentPets = computed(() => state.pets.slice(0, 3));
function providerPetPhoto(pet) { return pet.photos?.[0] || pet.image || ''; }
function imageToJpeg(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = reject;
    reader.onload = () => {
      const image = new Image();
      image.onerror = reject;
      image.onload = () => {
        const scale = Math.min(1, 900 / Math.max(image.naturalWidth, image.naturalHeight));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
        canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
        canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', .76));
      };
      image.src = reader.result;
    };
    reader.readAsDataURL(file);
  });
}
const overviewStats = computed(() => [
  { label: 'Mascotas publicadas', value: state.pets.length, icon: 'paw-print', section: 'Mis mascotas' },
  { label: 'Solicitudes nuevas', value: state.requests.filter(item => item.status === 'Nueva').length, icon: 'inbox', section: 'Solicitudes' },
  { label: 'Adopciones en proceso', value: state.requests.filter(item => ['Aceptada', 'En proceso'].includes(item.status)).length, icon: 'refresh-cw', section: 'En proceso' },
  { label: 'Adopciones completadas', value: state.pets.filter(item => item.status === 'Adoptada').length, icon: 'heart-handshake', section: 'Completadas' }
]);
const conversations = computed(() => {
  messagesRevision.value;
  return inbox.value;
});
const activeConversation = computed(() => conversations.value.find(item => item.id === selectedMessageId.value) || conversations.value[0]);
const completion = computed(() => { const fields = stepFields.slice(0, 5).flat().map(([key]) => key); return Math.round(fields.filter(key => Array.isArray(state.draft[key]) ? state.draft[key].length : String(state.draft[key] || '').trim()).length / fields.length * 100); });
const profileCompletion = computed(() => [state.profile.name, state.profile.contact, state.profile.location, state.profile.email, state.profile.phone, state.profile.description].filter(Boolean).length);
function persist(message = '') { const { photos, photoNames, ...draft } = state.draft || {}; if (!storageService.write('pethop_provider_workspace', { ...state, draft })) { notice.value = 'No se pudieron guardar las preferencias locales.'; return false; } if (message) toast(message); return true; }
let toastTimer;
function toast(message) { notice.value = message; clearTimeout(toastTimer); toastTimer = setTimeout(() => notice.value = '', 3200); }
function chooseRole() { router.push({ path: '/acceso.html', query: { redirect: '/proveedor.html' } }); }
function go(item) { section.value = state.section = item; persist(); }
async function loadProviderData() {
  try {
    const user = await api('/auth/me');
    if (user.role !== 'SHELTER' && user.role !== 'ADMIN') { state.role = ''; return; }
    currentProviderId.value = user.id;
    const [shelter, petPage, applicationPage, threadList] = await Promise.all([
      api('/shelters/me'), api('/pets/mine?page=0&pageSize=48'), api('/shelters/me/applications?page=0&size=50'), api('/conversations')
    ]);
    state.role = 'organization'; state.profile = { name: shelter.organizationName, description: shelter.description || '', location: [shelter.city, shelter.state].filter(Boolean).join(', '), phone: shelter.contactPhone || '', contact: user.fullName, email: user.email, verified: shelter.verificationStatus === 'VERIFIED', reviewStatus: shelter.verificationStatus === 'PENDING' ? 'En revisión' : '' };
    state.pets = (petPage.content || []).map(item => {
      const pet = mapPet(item);
      return { ...pet, status: ({ AVAILABLE: 'Publicada', IN_PROCESS: 'En proceso de adopción', ADOPTED: 'Adoptada', UNAVAILABLE: 'Pausada', DRAFT: 'Borrador' })[item.adoptionStatus], age: pet.ageGroup, requests: 0, days: 0, image: pet.photos?.[0] || '' };
    });
    state.requests = (applicationPage.content || []).map(item => ({
      id: item.id, adopter: item.applicantName, adopterEmail: item.applicantEmail, pet: item.petName, petId: item.petId,
      date: item.submittedAt, status: ({ SUBMITTED: 'Nueva', UNDER_REVIEW: 'En revisión', APPROVED: 'Aceptada', REJECTED: 'Rechazada', WITHDRAWN: 'Retirada' })[item.status] || item.status,
      home: item.housingType || 'No especificado', experience: item.experience || 'No especificada', household: item.availability || 'No especificado',
      reason: item.motivation, conditions: item.availability || 'No especificadas', notes: item.additionalInformation || 'Sin información adicional'
    }));
    inbox.value = await Promise.all(threadList.map(async thread => {
      let messages = [];
      try {
        const page = await api('/conversations/' + thread.id + '/messages?page=0&size=100');
        messages = (page.content || []).map(message => ({ from: message.senderId === currentProviderId.value ? 'provider' : 'adopter', text: message.content, date: message.createdAt }));
      } catch { /* A thread remains listed if message history cannot be fetched. */ }
      return { ...thread, pet: thread.petName, adopter: thread.otherParticipant, status: '', messages };
    }));
    messagesRevision.value += 1;
    if (route.query.section === 'mensajes') state.section = 'Mensajes';
    section.value = state.section || 'Inicio';
  } catch (error) { state.role = ''; if (error.status !== 401 && error.status !== 403) toast(error.message || 'No se pudieron cargar los datos del refugio.'); }
}
function openPet(pet) { selectedPet.value = pet; selectedPetStatus.value = pet.status; editingPet.value = false; }
async function updateStatus(pet, newStatus) {
  const status = ({ Publicada: 'AVAILABLE', Pausada: 'UNAVAILABLE', Borrador: 'DRAFT' })[newStatus];
  if (!status) { toast('El estado de adopción se gestiona al revisar solicitudes y formalizar la adopción.'); return; }
  try { await api('/pets/' + pet.id + '/status', { method: 'PATCH', body: { adoptionStatus: status } }); pet.status = newStatus; toast('Estado de publicación actualizado.'); selectedPet.value = null; }
  catch (error) { toast(error.message || 'No se pudo actualizar el estado.'); }
}
function savePetStatus() { if (!selectedPet.value) return; updateStatus(selectedPet.value, selectedPetStatus.value); }
async function deletePet(pet) {
  if (!window.confirm(`¿Eliminar la publicación de ${pet.name}? Dejará de aparecer en el catálogo.`)) return;
  try {
    await api('/pets/' + encodeURIComponent(pet.id), { method: 'DELETE' });
    state.pets = state.pets.filter(item => String(item.id) !== String(pet.id));
    selectedPet.value = null;
    editingPet.value = false;
    persist(`${pet.name} se eliminó del catálogo.`);
  } catch (error) { toast(error.message || 'No se pudo eliminar la publicación.'); }
}
function editPetInformation() {
  if (!selectedPet.value) return;
  const images = selectedPet.value.images || [];
  petEditForm.value = {
    ...selectedPet.value,
    photosToKeep: images.map((image, index) => ({ ...image, displayUrl: selectedPet.value.photos?.[index] || `${API_BASE.replace(/\/api\/v1$/, '')}${image.url}` })),
    newPhotos: [],
    personality: Array.isArray(selectedPet.value.personality) ? selectedPet.value.personality.join(', ') : (selectedPet.value.personality || '')
  };
  editingPet.value = true;
}
function removeEditPhoto(imageId) {
  petEditForm.value.photosToKeep = petEditForm.value.photosToKeep.filter(image => image.id !== imageId);
}
async function addEditPhotos(event) {
  const files = [...event.target.files];
  const remaining = 6 - petEditForm.value.photosToKeep.length - petEditForm.value.newPhotos.length;
  if (!files.length) return;
  if (files.length > remaining || files.some(file => !file.type.startsWith('image/') || file.size > 12 * 1024 * 1024)) {
    toast(`Puedes agregar hasta ${Math.max(0, remaining)} fotos más; cada archivo debe pesar menos de 12 MB.`);
    event.target.value = '';
    return;
  }
  try {
    const additions = await Promise.all(files.map(async file => ({ dataUrl: await imageToJpeg(file), name: file.name })));
    petEditForm.value.newPhotos.push(...additions);
  } catch { toast('No se pudieron procesar las fotos. Intenta con otros archivos.'); }
  event.target.value = '';
}
function removeNewEditPhoto(index) { petEditForm.value.newPhotos.splice(index, 1); }
async function savePetInformation() {
  if (!selectedPet.value) return;
  const updatedPet = { ...petEditForm.value };
  const personality = Array.isArray(updatedPet.personality)
    ? updatedPet.personality.join(', ')
    : String(updatedPet.personality || '').split(',').map(value => value.trim()).filter(Boolean).join(', ');
  savingPetChanges.value = true;
  try {
    await api('/pets/' + selectedPet.value.id, { method: 'PATCH', body: {
      name: updatedPet.name, species: ({ Perro:'DOG', Gato:'CAT', Otro:'OTHER' })[updatedPet.species] || 'DOG',
      sex: ({ Hembra:'FEMALE', Macho:'MALE' })[updatedPet.gender] || 'UNKNOWN',
      size: ({ Pequeño:'SMALL', Mediano:'MEDIUM', Grande:'LARGE' })[updatedPet.size] || null,
      ageValue: updatedPet.ageValue ?? null, ageUnit: updatedPet.ageUnit || null,
      description: updatedPet.description || updatedPet.story || 'Información pendiente de ampliar.',
      personality, healthSummary: updatedPet.healthSummary || '',
      sterilized: Boolean(updatedPet.sterilized), vaccinated: Boolean(updatedPet.vaccinated), dewormed: Boolean(updatedPet.dewormed),
      location: updatedPet.location || '', adoptionStatus: 'AVAILABLE'
    } });
    const retainedImageIds = new Set(updatedPet.photosToKeep.map(image => image.id));
    const removedImages = (selectedPet.value.images || []).filter(image => !retainedImageIds.has(image.id));
    for (const image of removedImages) {
      await api(`/pets/${encodeURIComponent(selectedPet.value.id)}/images/${encodeURIComponent(image.id)}`, { method: 'DELETE' });
    }
    for (const [index, photo] of updatedPet.newPhotos.entries()) {
      const blob = await (await fetch(photo.dataUrl)).blob();
      const baseName = photo.name.replace(/\.[^.]+$/, '') || `mascota-${index + 1}`;
      const file = new File([blob], `${baseName}.jpg`, { type: 'image/jpeg' });
      const form = new FormData(); form.append('file', file);
      await api(`/pets/${encodeURIComponent(selectedPet.value.id)}/images`, { method: 'POST', body: form });
    }
    const refreshed = mapPet(await api('/pets/' + encodeURIComponent(selectedPet.value.id)));
    const refreshedPet = { ...refreshed, status: ({ AVAILABLE:'Publicada', IN_PROCESS:'En proceso de adopción', ADOPTED:'Adoptada', UNAVAILABLE:'Pausada', DRAFT:'Borrador' })[refreshed.adoptionStatus], age: refreshed.ageGroup, requests: selectedPet.value.requests || 0, days: selectedPet.value.days || 0, image: refreshed.photos?.[0] || '' };
    state.pets = state.pets.map(pet => String(pet.id) === String(refreshedPet.id) ? refreshedPet : pet);
    editingPet.value = false; selectedPet.value = null; persist(); toast('Información y fotografías actualizadas.');
  } catch (error) { toast(error.message || 'No se pudo actualizar la publicación.'); }
  finally { savingPetChanges.value = false; }
}
async function advancePublish(event) {
  if (!event.currentTarget.reportValidity()) return;
  if (state.step < 6) { state.step += 1; persist(); return; }
  const reviewed = event.currentTarget.elements.reviewed;
  if (!reviewed?.checked) { reviewed?.focus(); return; }
  const draft = state.draft;
  const species = ({ Perro:'DOG', Gato:'CAT', Otro:'OTHER' })[draft.species] || 'DOG';
  const sex = ({ Hembra:'FEMALE', Macho:'MALE', 'No especificado':'UNKNOWN' })[draft.gender] || 'UNKNOWN';
  const ageValue = ({ Cachorro:0, Joven:1, Adulto:3, Senior:8 })[draft.age];
  const payload = {
    name: draft.name, species, sex, size: ({ Pequeño:'SMALL', Mediano:'MEDIUM', Grande:'LARGE' })[draft.size] || null,
    ageValue: ageValue == null ? null : ageValue, ageUnit: ageValue == null ? null : 'YEARS',
    description: [draft.story, draft.background].filter(Boolean).join('\n\n') || 'Información pendiente de ampliar.',
    personality: Array.isArray(draft.personality) ? draft.personality.join(', ') : (draft.personality || ''),
    healthSummary: [draft.condition, draft.healthNotes].filter(Boolean).join(' · '),
    sterilized: draft.sterilization === 'Sí', vaccinated: draft.vaccination === 'Al día',
    dewormed: draft.deworming === 'Al día', location: draft.location, adoptionStatus: 'AVAILABLE'
  };
  try {
    const created = await api('/pets', { method: 'POST', body: payload });
    try {
      for (const [index, dataUrl] of (draft.photos || []).entries()) {
        const blob = await (await fetch(dataUrl)).blob();
        const originalName = draft.photoNames?.[index] || ('mascota-' + (index + 1));
        const baseName = originalName.replace(/\.[^.]+$/, '') || ('mascota-' + (index + 1));
        const file = new File([blob], baseName + '.jpg', { type: 'image/jpeg' });
        const form = new FormData(); form.append('file', file);
        await api('/pets/' + created.id + '/images', { method: 'POST', body: form });
      }
    } catch (uploadError) {
      try { await api('/pets/' + created.id, { method: 'DELETE' }); } catch { /* Keep the upload error visible if cleanup also fails. */ }
      throw uploadError;
    }
    const response = await api('/pets/mine?page=0&pageSize=48');
    state.pets = (response.content || []).map(item => { const pet = mapPet(item); return { ...pet, status: ({ AVAILABLE:'Publicada', IN_PROCESS:'En proceso de adopción', ADOPTED:'Adoptada', UNAVAILABLE:'Pausada', DRAFT:'Borrador' })[item.adoptionStatus], age: pet.ageGroup, requests: 0, days: 0, image: pet.photos?.[0] || '' }; });
    state.draft = {}; state.step = 0; section.value = state.section = 'Mis mascotas'; persist('La publicación quedó guardada en PetHop.');
  } catch (error) { toast(error.message || 'No se pudo guardar la publicación.'); }
}
async function requestAction(action) {
  const request = selectedRequest.value; if (!request) return;
  if (action === 'contact') {
    const thread = inbox.value.find(item => String(item.petId) === String(request.petId));
    if (thread) { selectedMessageId.value = thread.id; section.value = state.section = 'Mensajes'; selectedRequest.value = null; }
    else toast('El adoptante puede iniciar la conversación desde el perfil de la mascota.');
    return;
  }
  const status = { review: 'UNDER_REVIEW', accept: 'APPROVED', reject: 'REJECTED' }[action];
  try {
    await api('/adoption-applications/' + request.id + '/status', { method: 'PATCH', body: { status } });
    request.status = { review: 'En revisión', accept: 'Aceptada', reject: 'Rechazada' }[action];
    if (action === 'accept') { const pet = state.pets.find(item => item.id === request.petId); if (pet) pet.status = 'En proceso de adopción'; section.value = state.section = 'En proceso'; }
    if (action === 'review' || action === 'reject') section.value = state.section = 'Solicitudes';
    selectedRequest.value = null; toast('La solicitud se actualizó en PetHop.');
  } catch (error) { toast(error.message || 'No se pudo actualizar la solicitud.'); }
}
async function formalizeAdoption(request) {
  try {
    await api('/adoptions', { method: 'POST', body: { applicationId: request.id, adoptionDate: new Date().toISOString().slice(0, 10), agreementNotes: '' } });
    request.status = 'Adoptada';
    const pet = state.pets.find(item => item.id === request.petId); if (pet) pet.status = 'Adoptada';
    selectedRequest.value = null; section.value = state.section = 'Completadas'; toast('Adopción formalizada y registrada en PetHop.');
  } catch (error) { toast(error.message || 'No se pudo formalizar la adopción.'); }
}
async function processPhotos(event) {
  const files = [...event.target.files];
  if (files.length > 6 || files.some(file => !file.type.startsWith('image/') || file.size > 12 * 1024 * 1024)) { toast('Selecciona hasta 6 imágenes de máximo 12 MB cada una.'); event.target.value = ''; return; }
  const convert = file => new Promise((resolve, reject) => { const reader = new FileReader(); reader.onerror = reject; reader.onload = () => { const image = new Image(); image.onerror = reject; image.onload = () => { const scale = Math.min(1, 900 / Math.max(image.naturalWidth, image.naturalHeight)); const canvas = document.createElement('canvas'); canvas.width = Math.max(1, Math.round(image.naturalWidth * scale)); canvas.height = Math.max(1, Math.round(image.naturalHeight * scale)); canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height); resolve(canvas.toDataURL('image/jpeg', .76)); }; image.src = reader.result; }; reader.readAsDataURL(file); });
  try { state.draft.photos = await Promise.all(files.map(convert)); state.draft.photoNames = files.map(file => file.name); persist(); } catch { toast('No se pudieron procesar las fotos. Intenta con otras imágenes.'); }
}
async function saveMessage(event) {
  const text = event.currentTarget.elements.message.value.trim(); const conversation = activeConversation.value;
  if (!text || !conversation) return;
  try {
    const result = await api('/conversations/' + conversation.id + '/messages', { method: 'POST', body: { content: text } });
    conversation.messages.push({ from: 'provider', text: result.content, date: result.createdAt });
    event.currentTarget.reset(); messagesRevision.value += 1;
  } catch (error) { toast(error.message || 'No se pudo enviar el mensaje.'); }
}
async function saveProfile() {
  try {
    const location = String(state.profile.location || '').split(',').map(value => value.trim());
    const result = await api('/shelters/me', { method: 'PATCH', body: {
      organizationName: state.profile.name, description: state.profile.description || '',
      city: location[0] || '', state: location[1] || '', contactPhone: state.profile.phone || ''
    } });
    state.profile.name = result.organizationName; toast('Perfil guardado en PetHop.');
  } catch (error) { toast(error.message || 'No se pudo guardar el perfil.'); }
}
watch(section, value => { document.body.classList.toggle('provider-dashboard-active', Boolean(state.role)); document.title = `${value} | PetHop`; }, { immediate: true });
watch(() => route.query.section, value => { if (value === 'mensajes') go('Mensajes'); });
watch(sidebarOpen, value => document.body.classList.toggle('provider-sidebar-open', value));
watch(narrowScreen, narrow => { if (!narrow) sidebarOpen.value = false; });
onMounted(() => {
  sidebarMediaQuery = window.matchMedia('(max-width: 760px)');
  updateScreenWidth(sidebarMediaQuery);
  sidebarMediaQuery.addEventListener('change', updateScreenWidth);
  loadProviderData();
});
onUnmounted(() => { sidebarMediaQuery?.removeEventListener('change', updateScreenWidth); document.body.classList.remove('provider-dashboard-active', 'provider-sidebar-open'); });
watch(() => state.draft, () => persist(), { deep: true });
watch(requestFilter, value => { state.filter = value; persist(); });
watch(petFilter, value => { state.petFilter = value; persist(); });
watch(() => state.step, () => persist());
</script>

<template>
 <div class="provider-page" :class="{ 'provider-dashboard-active': Boolean(state.role) }" @keydown.esc.window="sidebarOpen=false">
   <SiteHeader />
   <main id="provider-app" class="provider-main" aria-live="polite">
   <section v-if="!state.role" class="provider-entry"><div class="provider-entry-copy"><ThemeToggle /><span class="eyebrow">PET HOP · ADOPCIÓN RESPONSABLE</span><h1>Una adopción responsable empieza con <span>una buena conexión.</span></h1><p>Organiza publicaciones, solicitudes y seguimientos desde un solo lugar, seas una persona particular o una organización.</p></div><div class="provider-entry-card"><span class="provider-entry-icon"><Icon name="heart-handshake" /></span><h2>¿Cómo quieres usar PetHop?</h2><div class="provider-choice-grid"><button type="button" class="provider-choice" @click="router.push('/explorar.html')"><span><Icon name="search" /></span><strong>Quiero adoptar</strong><small>Descubrir mascotas y seguir mis solicitudes.</small></button><button type="button" class="provider-choice" @click="entryChoosingRole = true"><span><Icon name="paw-print" /></span><strong>Quiero dar en adopción</strong><small>Publicar y acompañar adopciones responsables.</small></button></div><div v-if="entryChoosingRole" class="provider-role-picker"><h3>¿Quién dará en adopción?</h3><div class="provider-choice-grid"><button class="provider-choice" @click="chooseRole('particular')"><span><Icon name="user-round" /></span><strong>Soy una persona particular</strong><small>Gestiona las mascotas que estás ayudando.</small></button><button class="provider-choice" @click="chooseRole('organization')"><span><Icon name="building-2" /></span><strong>Soy un refugio u organización</strong><small>Coordina tu equipo y tus publicaciones.</small></button></div></div></div></section>
   <div v-else class="provider-workspace"><button v-if="sidebarOpen" type="button" class="provider-sidebar-backdrop" aria-label="Cerrar navegación del panel" @click="sidebarOpen=false"></button><aside id="provider-sidebar" class="provider-sidebar" :class="{ 'is-open': sidebarOpen }" :inert="narrowScreen && !sidebarOpen" :aria-hidden="narrowScreen && !sidebarOpen"><div class="provider-account-badge"><Icon :name="state.role === 'organization' ? 'building-2' : 'user-round'" /><span>{{ state.profile.name || (state.role === 'organization' ? 'Mi organización' : 'Mi perfil') }}</span><small>{{ state.role === 'organization' ? 'Organización' : 'Particular' }}</small></div><nav aria-label="Panel de proveedor"><button v-for="item in NAV_ITEMS" :key="item.label" type="button" class="provider-nav-item" :class="{ 'is-active': section === item.label }" :aria-current="section === item.label ? 'page' : undefined" @click="go(item.label);sidebarOpen=false"><Icon :name="item.icon" /><span>{{ item.label }}</span><b v-if="item.label === 'Solicitudes'">{{ state.requests.filter(request => request.status === 'Nueva').length }}</b></button></nav><ThemeToggle class="provider-theme-toggle" /><RouterLink to="/" class="provider-back-home"><Icon name="arrow-left" />Volver a PetHop</RouterLink></aside>
    <section class="provider-content"><button class="provider-mobile-menu" type="button" aria-controls="provider-sidebar" :aria-expanded="sidebarOpen" :aria-label="sidebarOpen ? 'Cerrar navegación del panel' : 'Abrir navegación del panel'" @click="sidebarOpen=!sidebarOpen"><Icon :name="sidebarOpen ? 'x' : 'menu'" /></button>
      <template v-if="section === 'Inicio'">
        <div class="provider-section-header">
          <div><span class="eyebrow">PANEL DE ADOPCIONES</span><h1 class="provider-greeting">Hola<span v-if="state.profile.name">, {{ state.profile.name.split(' ')[0] }}</span></h1><p>Aquí tienes un resumen de tus adopciones y lo que requiere tu atención.</p></div>
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
      <template v-else-if="section === 'Mis mascotas' || section === 'Completadas'"><div class="provider-section-header"><div><span class="eyebrow">TUS PUBLICACIONES</span><h1>{{ section }}</h1><p>Consulta su estado, las solicitudes recibidas y el tiempo de publicación.</p></div><button class="button button-primary" @click="go('Publicar mascota')"><Icon name="plus" /> Publicar mascota</button></div><div v-if="section==='Mis mascotas'" class="provider-filter-pills"><button v-for="filter in ['Todas','Publicada','En revisión','En proceso de adopción','Adoptada','Pausada']" :key="filter" :class="{ 'is-selected': petFilter===filter }" @click="petFilter=filter">{{ filter }}</button></div><div class="provider-pet-grid"><article v-for="pet in (section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets)" :key="pet.id" class="provider-pet-card" tabindex="0" role="button" :aria-label="`Ver detalles de ${pet.name}`" @click="openPet(pet)" @keydown.enter="openPet(pet)" @keydown.space.prevent="openPet(pet)"><ImageWithSkeleton :frame-class="['provider-pet-photo', 'provider-photo-' + (pet.image || 'default')]" :frame-style="pet.photos?.[0] ? { backgroundImage: 'url(' + pet.photos[0] + ')' } : undefined" :src="providerPetPhoto(pet)" :alt="`Fotografía de ${pet.name}`" role="img"><span v-if="!providerPetPhoto(pet)" class="provider-pet-no-photo" aria-hidden="true"><Icon name="paw-print" /><small>Sin fotografía</small></span></ImageWithSkeleton><div class="provider-pet-card-body"><span class="provider-status">{{ pet.status }}</span><h3>{{ pet.name }}</h3><p>{{ pet.species }} · {{ pet.age }} · {{ pet.gender }}</p><p>{{ pet.days || 0 }} días publicada · {{ pet.requests || 0 }} solicitudes</p></div></article><p v-if="!(section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets).length" class="provider-empty">{{ section==='Completadas' ? 'Aquí veremos las adopciones completadas.' : 'No hay mascotas en este estado. Prueba otro filtro o publica una mascota.' }}</p></div></template>
      <template v-else-if="section === 'Publicar mascota'"><div class="provider-section-header"><div><span class="eyebrow">NUEVA PUBLICACIÓN</span><h1>Publicar mascota</h1><p>Completa la información paso a paso para presentar su perfil de forma clara.</p></div></div><section class="provider-panel provider-publish-panel"><div class="provider-steps"><button v-for="(label,index) in steps" :key="label" type="button" class="provider-step" :class="{ 'is-current':state.step===index,'is-done':index<state.step }" :disabled="index>state.step" @click="state.step=index"><span><Icon v-if="index<state.step" name="check" /><template v-else>{{ String(index+1).padStart(2,'0') }}</template></span><small>{{ label }}</small></button></div><div class="provider-progress"><span :style="{ width: `${completion}%` }"></span></div><form class="provider-form-grid" @submit.prevent="advancePublish"><h2>{{ steps[state.step] }}</h2><p class="provider-step-hint">Paso {{ state.step+1 }} de {{ steps.length }}</p><template v-if="state.step===6"><div class="provider-review-card"><div class="provider-review-progress"><div><strong>Información del perfil: {{ completion }}% completa</strong><p>Este porcentaje refleja qué tan completa está la información proporcionada.</p></div></div><div class="provider-review-pet"><img v-if="state.draft.photos?.[0]" :src="state.draft.photos[0]" :alt="`Fotografía de ${state.draft.name}`"><div><h3>{{ state.draft.name || 'Nombre de la mascota' }}</h3><p>{{ state.draft.species || 'Especie por agregar' }} · {{ state.draft.age || 'Edad por agregar' }} · {{ state.draft.location || 'Ubicación por agregar' }}</p><p>Revisa los datos antes de publicar. Podrás actualizarlos después.</p></div></div></div></template><template v-else><template v-for="field in stepFields[state.step]" :key="field[0]"><label v-if="field[2]==='text'" class="provider-field">{{ field[1] }}<input v-model="state.draft[field[0]]" :required="['name','location'].includes(field[0])"></label><label v-else-if="field[2]==='location'" class="provider-field">{{ field[1] }}<input v-model="state.draft[field[0]]" type="text" maxlength="180" placeholder="Ciudad o municipio" required></label><label v-else-if="field[2]==='select'" class="provider-field">{{ field[1] }}<select v-model="state.draft[field[0]]" :required="field[0]==='species'"><option value="">Selecciona</option><option v-for="option in field[3].split('|')" :key="option">{{ option }}</option></select></label><label v-else-if="field[2]==='textarea'" class="provider-field">{{ field[1] }}<textarea v-model="state.draft[field[0]]" rows="3"></textarea></label><fieldset v-else-if="field[2]==='checks'" class="provider-field provider-checks"><legend>{{ field[1] }}</legend><label v-for="option in field[3].split('|')" :key="option"><input type="checkbox" :value="option" v-model="state.draft[field[0]]"><span>{{ option }}</span></label></fieldset><label v-else-if="field[2]==='file'" class="provider-field provider-photo-field">Fotografías<input class="provider-photo-input" type="file" accept="image/*" multiple @change="processPhotos"><small>Hasta 6 fotografías optimizadas y guardadas en PetHop (máximo 10 MB cada una).</small><span class="provider-photo-preview"><img v-for="(photo,index) in state.draft.photos" :key="index" :src="photo" :alt="`Vista previa ${index+1}`"></span></label></template></template><label v-if="state.step===6" class="provider-ack"><input name="reviewed" type="checkbox" required> Confirmo que la información es correcta y puedo responder las solicitudes de adopción.</label><div class="provider-form-actions"><button v-if="state.step>0" type="button" class="button button-outline" @click="state.step--"><Icon name="arrow-left" /> Anterior</button><span v-else></span><button type="submit" class="button button-primary"><template v-if="state.step<6">Continuar <Icon name="arrow-right" /></template><template v-else>Publicar mascota</template></button></div></form></section></template>
      <template v-else-if="['Solicitudes','En proceso','Seguimiento'].includes(section)"><div class="provider-section-header"><div><span class="eyebrow">{{ section==='Seguimiento'?'ACOMPAÑAMIENTO':'EVALUACIÓN RESPONSABLE' }}</span><h1>{{ section === 'Solicitudes' ? 'Solicitudes de adopción' : section==='Seguimiento'?'Seguimiento de adopción':section }}</h1><p>{{ section==='Seguimiento'?'Adoptante y persona que da en adopción comparten las etapas del mismo proceso.':'Conoce a cada familia y continúa el proceso con calma.' }}</p></div></div><template v-if="section==='Solicitudes'"><div class="provider-filter-pills"><button v-for="filter in ['Todas','Nuevas','En revisión','Aceptadas','Rechazadas']" :key="filter" :class="{ 'is-selected':requestFilter===filter }" @click="requestFilter=filter">{{ filter }}<small>{{ filter==='Todas'?state.requests.length:state.requests.filter(item=>item.status===({Nuevas:'Nueva',Aceptadas:'Aceptada'}[filter]||filter)).length }}</small></button></div><section class="provider-panel"><button v-for="item in visibleRequests" :key="item.id" class="provider-request-row" @click="selectedRequest=item"><span class="provider-request-avatar">{{ item.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ item.adopter }}</strong><small>{{ item.pet }} · {{ new Date(item.date).toLocaleDateString('es-MX') }}</small></span><span class="provider-status">{{ item.status }}</span><Icon name="chevron-right" /></button><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="clipboard-check" /><strong>No hay solicitudes</strong><p>Las solicitudes aparecerán aquí al llegar.</p></div></section></template><section v-else-if="section==='En proceso'" class="provider-panel provider-pipeline-list"><article v-for="item in visibleRequests" :key="item.id" class="provider-pipeline-card"><div><span class="provider-status">{{ item.status==='Aceptada'?'Evaluación aceptada':'En proceso' }}</span><h3>{{ item.pet }} · {{ item.adopter }}</h3><p>Próximo paso: acordar el encuentro y preparar la adopción.</p></div><button class="button button-outline" @click="selectedRequest=item">Ver solicitud <Icon name="arrow-right" /></button></article><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="refresh-cw" /><strong>Aún no hay adopciones en curso</strong><p>Al aceptar una solicitud, podrás coordinar la evaluación y el encuentro.</p></div></section><section v-else class="provider-panel"><article v-for="item in visibleRequests" :key="item.id" class="provider-track-card"><div class="provider-track-heading"><div><span class="provider-status">{{ item.status }}</span><h2>{{ item.pet }} · {{ item.adopter }}</h2></div><button type="button" class="provider-text-action" @click="selectedRequest=item">Abrir solicitud <Icon name="arrow-right" /></button></div><ol class="provider-timeline"><li v-for="(step,index) in trackingSteps" :key="step" :class="{ 'is-complete':index <= (item.status==='Aceptada'?2:3) }"><span><Icon v-if="index <= (item.status==='Aceptada'?2:3)" name="check" /><span v-else>{{ index+1 }}</span></span><div><strong>{{ step }}</strong><small v-if="index===0">{{ new Date(item.date).toLocaleDateString('es-MX') }}</small></div></li></ol></article><div v-if="!visibleRequests.length" class="provider-empty"><Icon name="route" /><strong>Sin seguimientos activos</strong><p>Los seguimientos de solicitudes aceptadas se organizarán aquí.</p></div></section></template>
      <template v-else-if="section==='Mensajes'"><div class="provider-section-header"><div><span class="eyebrow">COMUNICACIÓN</span><h1>Mensajes</h1><p>Conversaciones sencillas vinculadas a una mascota y su solicitud.</p></div></div><div class="provider-messages-layout"><section class="provider-panel provider-conversation-list"><h2>Conversaciones</h2><button v-for="thread in conversations" :key="thread.id" class="provider-conversation" :class="{ 'is-selected':(activeConversation?.id===thread.id) }" @click="selectedMessageId=state.activeMessage=thread.id;persist()"><span class="provider-request-avatar">{{ thread.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ thread.adopter }}</strong><small>{{ thread.pet }} · {{ thread.status }}</small></span></button><div v-if="!conversations.length" class="provider-empty"><Icon name="messages-square" /><strong>Sin conversaciones</strong><p>Al contactar a una familia podrás continuar la conversación aquí.</p></div></section><section class="provider-panel provider-chat-panel"><template v-if="activeConversation"><div class="provider-chat-head"><span class="provider-request-avatar">{{ activeConversation.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><div><strong>{{ activeConversation.adopter }}</strong><small>{{ activeConversation.pet }} · {{ activeConversation.status }}</small></div></div><div class="provider-chat-transcript" aria-live="polite"><p v-for="(message,index) in activeConversation.messages" :key="index" :class="{ 'is-outgoing':message.from==='provider' }">{{ message.text }}<small>{{ new Date(message.date).toLocaleString('es-MX') }}</small></p><p v-if="!activeConversation.messages.length" class="provider-chat-note">Inicia la conversación con {{ activeConversation.adopter }} sobre {{ activeConversation.pet }}.</p></div><form class="provider-chat-form" @submit.prevent="saveMessage"><label class="sr-only" for="provider-message">Escribe un mensaje</label><textarea id="provider-message" name="message" rows="2" placeholder="Escribe un mensaje..." required></textarea><button class="button button-primary">Enviar</button></form></template><div v-else class="provider-chat-empty"><Icon name="messages-square" /><h3>Comunicación clara, adopciones responsables</h3><p>Selecciona una conversación para enviar un mensaje sobre la mascota.</p></div></section></div></template>
      <template v-else-if="section==='Perfil / Organización'">
        <div class="provider-section-header"><div><span class="eyebrow">TU IDENTIDAD EN PETHOP</span><h1>{{ state.role==='organization'?'Perfil de organización':'Perfil de persona particular' }}</h1><p>Una presentación clara genera confianza con las familias adoptantes.</p></div></div>
        <div v-if="state.profile.reviewStatus === 'En revisión'" class="provider-review-banner" role="status"><span class="provider-review-banner-icon"><Icon name="shield-check" /></span><div><strong>Tu registro quedó en revisión</strong><p>El perfil de {{ state.profile.name || 'tu organización' }} está marcado «En revisión». Tus datos ya están disponibles en este panel.</p></div><span class="provider-review-banner-status">En revisión</span></div>
        <div class="provider-profile-layout">
          <section class="provider-panel">
            <div class="provider-verification" :class="{ 'is-verified': state.profile.verified }"><Icon :name="state.profile.verified ? 'badge-check' : 'shield-check'" /><div><strong>{{ state.profile.verified ? (state.role==='organization'?'Organización verificada':'Identidad verificada') : 'Verificación pendiente' }}</strong><p>{{ state.profile.verified ? 'La organización aparece verificada en PetHop.' : 'Puedes usar PetHop normalmente mientras completas la verificación.' }}</p></div><span>{{ profileCompletion }}/6 datos</span></div>
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
          <aside class="provider-panel provider-profile-aside"><span class="provider-aside-icon"><Icon :name="state.role==='organization'?'building-2':'user-round'" /></span><h3>{{ state.role==='organization'?'Representa a tu organización':'Acompaña cada adopción' }}</h3><p>La verificación ayuda a crear confianza. No limita el acceso a las herramientas de PetHop.</p><p>La verificación administrativa estará disponible cuando se habilite el flujo de revisión.</p></aside>
        </div>
      </template>
      <template v-else-if="section==='Configuración'"><div class="provider-section-header"><div><span class="eyebrow">PREFERENCIAS</span><h1>Configuración</h1><p>Ajusta la experiencia de tu espacio de adopciones.</p></div></div><section class="provider-panel provider-settings"><label class="provider-setting-row"><span><strong>Notificaciones de nuevas solicitudes</strong><small>Preferencia de visualización guardada en este navegador.</small></span><input v-model="state.notify" type="checkbox" @change="persist()"></label><p class="provider-data-note">Mascotas, solicitudes y perfiles se guardan en PostgreSQL a través de la API.</p></section></template>
      <template v-else><div class="provider-section-header"><div><span class="eyebrow">CENTRO DE AYUDA</span><h1>¿Cómo podemos <span>ayudarte?</span></h1><p>Guía rápida para organizar una adopción responsable.</p></div></div><div class="provider-help-grid"><article v-for="[number,title,copy] in [['01','Publica con claridad','Completa salud, personalidad, historia y requisitos para orientar a las familias.'],['02','Evalúa cada solicitud','Revisa las condiciones del hogar y conversa con el adoptante antes de decidir.'],['03','Coordina el encuentro','Usa el seguimiento para preparar la entrega y continuar en contacto después.'],['04','Cuida tus datos','Los registros de mascotas, solicitudes y mensajes se consultan desde el servidor.']]" :key="number" class="provider-help-card"><span>{{ number }}</span><h2>{{ title }}</h2><p>{{ copy }}</p></article></div></template>
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
         <section class="provider-edit-photos provider-pet-edit-wide" aria-label="Fotografías de la mascota"><div class="provider-edit-photos-heading"><strong>Fotografías</strong><span>{{ petEditForm.photosToKeep.length + petEditForm.newPhotos.length }} de 6</span></div><p class="provider-modal-note">Agrega imágenes o marca las existentes para quitarlas. Los cambios se aplicarán al guardar.</p><div v-if="petEditForm.photosToKeep.length || petEditForm.newPhotos.length" class="provider-edit-photo-grid"><article v-for="photo in petEditForm.photosToKeep" :key="photo.id" class="provider-edit-photo"><img :src="photo.displayUrl" :alt="`Fotografía de ${selectedPet.name}`"><button type="button" aria-label="Quitar fotografía" @click="removeEditPhoto(photo.id)"><Icon name="x" /> Quitar</button></article><article v-for="(photo,index) in petEditForm.newPhotos" :key="`new-${index}`" class="provider-edit-photo"><img :src="photo.dataUrl" :alt="`Fotografía nueva ${index+1} de ${selectedPet.name}`"><button type="button" aria-label="Quitar foto nueva" @click="removeNewEditPhoto(index)"><Icon name="x" /> Quitar</button></article></div><label class="provider-field provider-edit-photo-input">Agregar fotos<input type="file" accept="image/*" multiple :disabled="petEditForm.photosToKeep.length + petEditForm.newPhotos.length >= 6" @change="addEditPhotos"><small>Hasta 6 fotos en total; se optimizan antes de subir.</small></label></section>
         <div class="provider-modal-actions provider-pet-edit-actions"><button type="button" class="button button-outline" :disabled="savingPetChanges" @click="editingPet=false">Cancelar</button><button type="submit" class="button button-primary" :disabled="savingPetChanges"><Icon name="check" /> {{ savingPetChanges ? 'Guardando…' : 'Guardar cambios' }}</button></div>
         <p class="provider-modal-note provider-pet-review-note"><Icon name="shield-check" /> Al guardar, la publicación pasará a En revisión mientras se verifican los cambios.</p>
       </form>
     </template>
     <template v-else>
       <div class="provider-applicant-grid"><article><small>Estado de publicación</small><p>{{ selectedPet.status }}</p></article><article><small>Actividad</small><p>{{ selectedPet.requests || 0 }} solicitudes</p></article></div><label class="provider-field">Actualizar estado<select v-model="selectedPetStatus"><option v-for="status in ['Publicada','Borrador','Pausada']" :key="status">{{ status }}</option></select></label><p class="provider-modal-note">El proceso y el cierre de adopción se actualizan al revisar la solicitud y formalizar el acuerdo.</p><div class="provider-modal-actions provider-pet-details-actions"><button type="button" class="button button-danger-outline" @click="deletePet(selectedPet)"><Icon name="x" /> Eliminar mascota</button><button type="button" class="button button-outline" @click="selectedPet=null">Cerrar</button><button type="button" class="button button-outline" @click="editPetInformation">Editar información</button><button type="button" class="button button-primary" @click="savePetStatus">Guardar estado</button></div>
     </template>
   </section></div>
   <div v-if="selectedRequest" class="provider-modal-backdrop" @click.self="selectedRequest=null"><section class="provider-request-modal" role="dialog" aria-modal="true" aria-labelledby="request-title"><button class="provider-modal-close" aria-label="Cerrar" @click="selectedRequest=null"><Icon name="x" /></button><span class="eyebrow">SOLICITUD DE ADOPCIÓN</span><h2 id="request-title">{{ selectedRequest.adopter }} <span class="provider-status">{{ selectedRequest.status }}</span></h2><p class="provider-request-lead">Solicitud para <strong>{{ selectedRequest.pet }}</strong> · {{ new Date(selectedRequest.date).toLocaleDateString('es-MX') }}</p><div class="provider-applicant-grid"><article v-for="[label,value] in [['Tipo de vivienda',selectedRequest.home],['Experiencia con mascotas',selectedRequest.experience],['Personas en el hogar',selectedRequest.household],['Motivo de adopción',selectedRequest.reason],['Condiciones del hogar',selectedRequest.conditions],['Información relevante',selectedRequest.notes]]" :key="label"><small>{{ label }}</small><p>{{ value || 'No proporcionado' }}</p></article></div><div class="provider-modal-actions"><button class="button button-outline" @click="requestAction('contact')">Contactar</button><button class="button button-outline" @click="requestAction('review')">Continuar revisión</button><button class="button button-primary" @click="requestAction('accept')">Aceptar solicitud</button><button v-if="selectedRequest.status === 'Aceptada'" class="button button-primary" @click="formalizeAdoption(selectedRequest)">Formalizar adopción</button><button class="button button-danger" @click="requestAction('reject')">Rechazar</button></div><p class="provider-modal-note">Aceptar inicia la coordinación; la adopción se completa después del encuentro y seguimiento.</p></section></div>
   <div v-if="notice" class="provider-toast is-visible" role="status">{{ notice }}</div>
 </div>
</template>

<style scoped>
.provider-review-banner { display:flex; align-items:center; gap:14px; margin:0 0 20px; padding:17px 19px; border:1px solid #b8ded4; border-radius:16px; background:#effaf5; box-shadow:0 5px 16px rgba(18,63,75,.05); }
.provider-review-banner-icon { display:grid; width:42px; height:42px; flex:0 0 42px; place-items:center; border-radius:13px; background:#d9f1e8; color:var(--primary-strong); }
.provider-review-banner > div { min-width:0; flex:1; }
.provider-review-banner strong { color:var(--text); font:700 .88rem/1.4 "Poppins",sans-serif; }
.provider-review-banner p { margin:3px 0 0; color:var(--text-soft); font-size:.75rem; line-height:1.5; }
.provider-review-banner-status { flex:0 0 auto; padding:7px 10px; border:1px solid #c6e5d8; border-radius:999px; background:#fff; color:#37695b; font-size:.68rem; font-weight:800; }
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
.provider-edit-photos { display:grid;gap:9px;margin-top:5px; }
.provider-edit-photos-heading { display:flex;align-items:center;justify-content:space-between;gap:12px;color:var(--text);font-size:.9rem;font-weight:700; }
.provider-edit-photos-heading span { color:var(--text-soft);font-size:.78rem;font-weight:600; }
.provider-edit-photos > .provider-modal-note { margin:0; }
.provider-edit-photo-grid { display:grid;grid-template-columns:repeat(auto-fill,minmax(105px,1fr));gap:10px; }
.provider-edit-photo { position:relative;overflow:hidden;aspect-ratio:1;border:1px solid var(--border);border-radius:12px;background:#eaf5f4; }
.provider-edit-photo img { width:100%;height:100%;object-fit:cover; }
.provider-edit-photo button { position:absolute;right:5px;bottom:5px;display:flex;align-items:center;gap:4px;padding:5px 8px;border:0;border-radius:20px;background:rgba(24,55,61,.9);color:white;font-size:.68rem;font-weight:700;cursor:pointer; }
.provider-edit-photo button :deep(svg) { width:13px;height:13px; }
.provider-edit-photo-input { margin-top:3px; }
.provider-edit-photo-input :deep(input[type="file"]) { width:100%;min-height:48px;padding:6px;border:1px solid var(--border);border-radius:11px;background:#f7fbfb;color:var(--text-soft);font:500 .8rem "Montserrat",Arial,sans-serif; }
.provider-edit-photo-input :deep(input[type="file"]::file-selector-button) { margin-right:12px;padding:9px 14px;border:0;border-radius:8px;background:var(--primary-strong);color:#fff;font:700 .78rem "Montserrat",Arial,sans-serif;cursor:pointer;transition:background .18s ease,transform .18s ease; }
.provider-edit-photo-input :deep(input[type="file"]::file-selector-button:hover) { background:var(--primary);transform:translateY(-1px); }
.provider-edit-photo-input :deep(input[type="file"]:focus-visible) { outline:3px solid color-mix(in srgb,var(--primary) 28%,transparent);outline-offset:2px; }
.provider-edit-photo-input :deep(input[type="file"]:disabled) { opacity:.55;cursor:not-allowed; }
.provider-pet-edit-actions { justify-content:flex-end; margin-top:18px; }
.provider-pet-details-actions { align-items:center; margin-top:18px; }
.provider-pet-details-actions .button:nth-child(2) { margin-right:auto; }
.provider-pet-details-actions .button-danger-outline { border-color:#d88b8b;color:#a53c3c;background:#fff; }
.provider-pet-details-actions .button-danger-outline:hover { border-color:#a53c3c;background:#fff4f4; }
.provider-pet-review-note { display:flex; align-items:flex-start; gap:7px; margin-top:12px; }
.provider-pet-review-note :deep(svg) { flex:0 0 17px; width:17px; height:17px; color:var(--primary-strong); }
@media(max-width:600px) {
  .provider-review-banner { align-items:flex-start; flex-wrap:wrap; gap:11px; padding:14px; }
  .provider-review-banner > div { flex-basis:calc(100% - 56px); }
  .provider-review-banner-status { margin-left:53px; }
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
