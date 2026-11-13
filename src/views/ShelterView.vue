<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import Icon from '../components/Icon.vue';
import ImageWithSkeleton from '../components/ImageWithSkeleton.vue';
import { storageService } from '../services/storage.js';
import { API_BASE, api } from '../services/api.js';
import { currentUser, loadCurrentUser, logout } from '../services/auth.js';
import { mapPet } from '../services/petApi.js';
import { mergePhotoSelection, normalizePersonality, petPayload, PERSONALITY_OPTIONS } from '../services/petForm.js';
import { shelterWorkspaceKey } from '../services/shelterWorkspace.js';

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
const PROFILE_SECTIONS = { perfil: 'Perfil / Organización', inicio: 'Inicio', mascotas: 'Mis mascotas', solicitudes: 'Solicitudes', mensajes: 'Mensajes', 'en-proceso': 'En proceso', completadas: 'Completadas' };
const routeSection = query => PROFILE_SECTIONS[String(query || '').toLowerCase()] || '';
function initialState(stored = {}) {
  stored = stored || {};
  const draft = { ...(stored.draft || {}) };
  draft.lifeStage ||= draft.age || '';
  draft.personalityTraits = normalizePersonality(draft.personalityTraits ?? draft.personality);
  draft.personality = draft.personalityTraits;
  draft.photos = Array.isArray(draft.photos) ? draft.photos : [];
  draft.photoNames = Array.isArray(draft.photoNames) ? draft.photoNames : [];
  draft.photoKeys = Array.isArray(draft.photoKeys) ? draft.photoKeys : [];
  return { ...stored, role: '', section: stored.section || 'Inicio', pets: [], requests: [], messages: [], profile: {}, draft, step: stored.step || 0, filter: stored.filter || 'Todas', petFilter: stored.petFilter || 'Todas', notify: stored.notify ?? true };
}
const state = reactive(initialState());
const section = ref(routeSection(route.query.section) || state.section); const notice = ref('');
const profileExists = ref(false);
const profileSetup = ref({ organizationName: '', description: '', address: '', city: '', state: '', postalCode: '', contactPhone: '' });
const inbox = ref([]); const currentShelterId = ref('');
if (routeSection(route.query.section)) state.section = routeSection(route.query.section);
const sidebarOpen = ref(false);
const narrowScreen = ref(false);
let sidebarMediaQuery;
function updateScreenWidth(event) { narrowScreen.value = event.matches; }
const requestFilter = ref(state.filter); const petFilter = ref(state.petFilter);
const selectedRequest = ref(null); const selectedPet = ref(null); const selectedPetStatus = ref(''); const editingPet = ref(false); const petEditForm = ref({}); const selectedMessageId = ref(state.activeMessage || '');
const savingPetChanges = ref(false);
const profileEditor = ref(''); const profileDraft = ref({}); const savingProfile = ref(false);
const passwordForm = ref({ currentPassword: '', newPassword: '', confirm: '' });
const passwordFeedback = ref(''); const passwordSaving = ref(false);
const messagesRevision = ref(0);
const steps = ['Información básica','Salud','Personalidad','Historia','Requisitos','Fotografías','Revisión'];
const trackingSteps = ['Solicitud recibida','Solicitud en revisión','Contacto realizado','Adopción en preparación','Entrega / encuentro','Seguimiento inicial','Adopción completada'];
const stepFields = [
  [['name','Nombre','text','Nombre de la mascota'],['species','Especie','select','Perro|Gato|Otro'],['lifeStage','Etapa de vida','select','Cachorro|Joven|Adulto|Senior'],['gender','Sexo','select','Hembra|Macho|No especificado'],['size','Tamaño','select','Pequeño|Mediano|Grande'],['breed','Raza','text','Raza o mezcla, si se conoce'],['weight','Peso (kg)','number','Ej. 8.5'],['location','Ubicación','select','Playa del Carmen|Cancún|Cozumel']],
  [['vaccination','Vacunación','select','Al día|Pendiente de confirmar|No vacunada'],['sterilization','Esterilización','select','Sí|Pendiente de confirmar|No'],['deworming','Desparasitación','select','Al día|Pendiente de confirmar|No'],['condition','Condición médica','text','Diagnóstico o condición conocida'],['healthNotes','Observaciones de salud','textarea','Tratamientos o notas relevantes']],
  [['personalityTraits','Personalidad y convivencia','checks',PERSONALITY_OPTIONS.join('|')]],
  [['description','Descripción','textarea','Describe brevemente a la mascota.'],['story','¿Cómo llegó a la adopción?','textarea','Ejemplo: fue encontrada en la calle y recibió atención temporal.'],['background','Antecedentes relevantes','textarea','Ejemplo: convivencias anteriores, cambios de hogar o información conocida.']],
  [['homeType','Tipo de hogar','text','Casa, departamento u otro tipo de vivienda'],['conditions','Condiciones necesarias','textarea','Espacio, rutina o cuidados que requiere'],['experience','Experiencia recomendada','textarea','Experiencia útil para atender sus necesidades'],['requirements','Otros requisitos','textarea','Requisitos adicionales para la adopción']],
  [['photos','Fotografías','file']], []
];
const requests = computed(() => state.requests.filter(item => requestFilter.value === 'Todas' || item.status === ({ Nuevas: 'Nueva', Aceptadas: 'Aceptada' }[requestFilter.value] || requestFilter.value)));
const visibleRequests = computed(() => ['En proceso','Seguimiento'].includes(section.value) ? state.requests.filter(item => ['Aceptada','En proceso'].includes(item.status)) : requests.value);
const visiblePets = computed(() => state.pets.filter(item => petFilter.value === 'Todas' || item.status === petFilter.value));
const petLocationOptions = computed(() => {
  const supported = ['Playa del Carmen', 'Cancún', 'Cozumel'];
  const currentLegacyValue = String(petEditForm.value?.location || '').trim();
  return currentLegacyValue && !supported.includes(currentLegacyValue) ? [...supported, currentLegacyValue] : supported;
});
const recentRequests = computed(() => state.requests.filter(item => ['Nueva', 'En revisión'].includes(item.status)).slice(0, 3));
const recentPets = computed(() => state.pets.slice(0, 3));
function shelterPetPhoto(pet) { return pet.photos?.[0] || pet.image || ''; }
function shelterPetRecord(response, previous = {}) {
  const pet = mapPet(response);
  return {
    ...pet,
    status: ({ AVAILABLE: 'Publicada', IN_PROCESS: 'En proceso de adopción', ADOPTED: 'Adoptada', UNAVAILABLE: 'Pausada', DRAFT: 'Borrador' })[pet.adoptionStatus],
    age: pet.ageGroup,
    requests: previous.requests || 0,
    days: previous.days || 0,
    image: pet.photos?.[0] || ''
  };
}
function shelterEditPhotos(pet) {
  return (pet.images || []).map((image, index) => ({
    ...image,
    displayUrl: pet.photos?.[index] || `${API_BASE.replace(/\/api\/v1$/, '')}${image.url}`
  }));
}
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
const completion = computed(() => Math.round((state.step / (steps.length - 1)) * 100));
function persist(message = '') {
  const workspaceKey = shelterWorkspaceKey(currentShelterId.value);
  if (!workspaceKey) return true;
  const { photos, photoNames, photoKeys, ...draft } = state.draft || {};
  if (!storageService.write(workspaceKey, { ...state, draft })) { notice.value = 'No se pudieron guardar las preferencias locales.'; return false; }
  if (message) toast(message);
  return true;
}
let toastTimer;
function toast(message) { notice.value = message; clearTimeout(toastTimer); toastTimer = setTimeout(() => notice.value = '', 3200); }
function chooseRole() { router.push({ path: '/acceso.html', query: { mode: 'register', role: 'shelter' } }); }
async function signOut() {
  try { await logout(); }
  finally { await router.push('/acceso.html'); }
}
async function changePassword() {
  passwordFeedback.value = '';
  if (passwordForm.value.newPassword !== passwordForm.value.confirm) { passwordFeedback.value = 'Las contraseñas nuevas no coinciden.'; return; }
  passwordSaving.value = true;
  try {
    await api('/users/me/password', { method: 'PATCH', body: { currentPassword: passwordForm.value.currentPassword, newPassword: passwordForm.value.newPassword } });
    passwordForm.value = { currentPassword: '', newPassword: '', confirm: '' };
    passwordFeedback.value = 'Tu contraseña se actualizó correctamente.';
  } catch (error) { passwordFeedback.value = error.message || 'No se pudo actualizar la contraseña.'; }
  finally { passwordSaving.value = false; }
}
function go(item) { if (!profileExists.value && item !== 'Perfil / Organización') { toast('Completa primero el perfil de tu organización para habilitar el panel.'); return; } section.value = state.section = item; persist(); }
async function loadShelterData() {
  try {
    const authenticated = await loadCurrentUser();
    if (!authenticated || authenticated.role !== 'SHELTER') { state.role = ''; return; }
    const user = await api('/users/me');
    currentShelterId.value = user.id;
    Object.assign(state, initialState(storageService.read(shelterWorkspaceKey(user.id), {})));
    state.role = 'organization';
    let shelter;
    try { shelter = await api('/shelters/me'); }
    catch (error) {
      if (error.status !== 404) throw error;
      profileExists.value = false;
      profileSetup.value = { organizationName: '', description: '', address: '', city: '', state: '', postalCode: '', contactPhone: '' };
      state.profile = { contact: user.fullName || '', personalPhone: user.phone || '', email: user.email || '' };
      section.value = state.section = 'Perfil / Organización';
      return;
    }
    profileExists.value = true;
    const [shelterProfile, petPage, applicationPage, threadList, adoptionPage] = await Promise.all([
      Promise.resolve(shelter), api('/pets/mine?page=0&pageSize=48'), api('/shelters/me/applications?page=0&size=50'), api('/conversations'), api('/shelters/me/adoptions?page=0&size=50')
    ]);
    state.profile = {
      name: shelterProfile.organizationName || '', description: shelterProfile.description || '', address: shelterProfile.address || '',
      city: shelterProfile.city || '', region: shelterProfile.state || '', postalCode: shelterProfile.postalCode || '',
      contactPhone: shelterProfile.contactPhone || '', contact: user.fullName || '', personalPhone: user.phone || '',
      email: user.email || '', verificationStatus: shelterProfile.verificationStatus,
      verified: shelterProfile.verificationStatus === 'VERIFIED', reviewStatus: shelterProfile.verificationStatus === 'PENDING' ? 'En revisión' : '',
      metrics: { pets: petPage.totalElements ?? (petPage.content || []).length, applications: applicationPage.totalElements ?? (applicationPage.content || []).length, adoptions: adoptionPage.totalElements ?? (adoptionPage.content || []).length }
    };
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
        messages = (page.content || []).map(message => ({ from: message.senderId === currentShelterId.value ? 'shelter' : 'adopter', text: message.content, date: message.createdAt }));
      } catch { /* A thread remains listed if message history cannot be fetched. */ }
      const pet = state.pets.find(item => String(item.id) === String(thread.petId));
      return { ...thread, pet: thread.petName || pet?.name || 'Mascota', petLocation: pet?.location || '', adopter: thread.otherParticipant, status: '', messages };
    }));
    messagesRevision.value += 1;
    section.value = state.section = routeSection(route.query.section) || state.section || 'Inicio';
  } catch (error) { state.role = ''; if (error.status !== 401 && error.status !== 403) toast(error.message || 'No se pudieron cargar los datos del refugio.'); }
}
async function createOrganizationProfile() {
  savingProfile.value = true;
  try {
    await api('/shelters', { method: 'POST', body: { ...profileSetup.value, organizationName: profileSetup.value.organizationName.trim() } });
    profileExists.value = true;
    window.dispatchEvent(new Event('pethop:shelter-updated'));
    await loadShelterData();
    section.value = state.section = 'Perfil / Organización';
    toast('Perfil de organización creado.');
  } catch (error) { toast(error.message || 'No se pudo crear el perfil. Tus datos siguen disponibles para reintentar.'); }
  finally { savingProfile.value = false; }
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
  petEditForm.value = {
    ...selectedPet.value,
    photosToKeep: shelterEditPhotos(selectedPet.value),
    newPhotos: [],
    lifeStage: selectedPet.value.age,
    personalityTraits: normalizePersonality(selectedPet.value.personalityTraits?.length ? selectedPet.value.personalityTraits : selectedPet.value.personality)
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
  if (files.length > remaining || files.some(file => !file.type.startsWith('image/') || file.size > 10 * 1024 * 1024)) {
    toast(`Puedes agregar hasta ${Math.max(0, remaining)} fotos más; cada archivo debe pesar como máximo 10 MB.`);
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
  const originalImageIds = new Set((selectedPet.value.images || []).map(image => image.id));
  const retainedImageIds = new Set(updatedPet.photosToKeep.map(image => image.id));
  let uploadedPhotoCount = 0;
  savingPetChanges.value = true;
  try {
    await api('/pets/' + selectedPet.value.id, { method: 'PATCH', body: petPayload({ ...updatedPet, adoptionStatus: selectedPet.value.adoptionStatus }) });
    for (const [index, photo] of updatedPet.newPhotos.entries()) {
      const blob = await (await fetch(photo.dataUrl)).blob();
      const baseName = photo.name.replace(/\.[^.]+$/, '') || `mascota-${index + 1}`;
      const file = new File([blob], `${baseName}.jpg`, { type: 'image/jpeg' });
      const form = new FormData(); form.append('file', file);
      await api(`/pets/${encodeURIComponent(selectedPet.value.id)}/images`, { method: 'POST', body: form });
      uploadedPhotoCount += 1;
    }
    const removedImages = (selectedPet.value.images || []).filter(image => !retainedImageIds.has(image.id));
    for (const image of removedImages) {
      await api(`/pets/${encodeURIComponent(selectedPet.value.id)}/images/${encodeURIComponent(image.id)}`, { method: 'DELETE' });
    }
    const refreshed = await api('/pets/' + encodeURIComponent(selectedPet.value.id));
    const refreshedPet = shelterPetRecord(refreshed, selectedPet.value);
    state.pets = state.pets.map(pet => String(pet.id) === String(refreshedPet.id) ? refreshedPet : pet);
    editingPet.value = false; selectedPet.value = null; persist(); toast('Información y fotografías actualizadas.');
  } catch (error) {
    try {
      const refreshedPet = shelterPetRecord(await api('/pets/' + encodeURIComponent(selectedPet.value.id)), selectedPet.value);
      selectedPet.value = refreshedPet;
      state.pets = state.pets.map(pet => String(pet.id) === String(refreshedPet.id) ? refreshedPet : pet);
      petEditForm.value = {
        ...updatedPet,
        photosToKeep: shelterEditPhotos(refreshedPet).filter(image => retainedImageIds.has(image.id) || !originalImageIds.has(image.id)),
        newPhotos: updatedPet.newPhotos.slice(uploadedPhotoCount)
      };
    } catch { /* Preserve the current form when the server cannot be queried after a partial save. */ }
    toast(error.message || 'No se pudo actualizar la publicación. Revisa las fotografías e inténtalo de nuevo.');
  }
  finally { savingPetChanges.value = false; }
}
async function advancePublish(event) {
  if (!event.currentTarget.reportValidity()) return;
  if (state.step < 6) { state.step += 1; persist(); return; }
  const reviewed = event.currentTarget.elements.reviewed;
  if (!reviewed?.checked) { reviewed?.focus(); return; }
  const draft = state.draft;
  const payload = petPayload({ ...draft, adoptionStatus: 'AVAILABLE' });
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
  event.target.value = '';
  if (files.some(file => !file.type.startsWith('image/') || file.size > 10 * 1024 * 1024)) { toast('Cada foto debe ser una imagen de máximo 10 MB.'); return; }
  const selection = mergePhotoSelection(state.draft.photoKeys, files);
  if (selection.exceeded) { toast('Puedes seleccionar hasta 6 fotografías en total.'); return; }
  if (!selection.additions.length) { if (selection.duplicates) toast('Esas fotografías ya están seleccionadas.'); return; }
  try {
    const converted = await Promise.all(selection.additions.map(async ({ file, key }) => ({ dataUrl: await imageToJpeg(file), name: file.name, key })));
    state.draft.photos.push(...converted.map(item => item.dataUrl));
    state.draft.photoNames.push(...converted.map(item => item.name));
    state.draft.photoKeys.push(...converted.map(item => item.key));
    persist();
  } catch { toast('No se pudieron procesar las fotos. Intenta con otras imágenes.'); }
}
function removeDraftPhoto(index) {
  state.draft.photos.splice(index, 1);
  state.draft.photoNames.splice(index, 1);
  state.draft.photoKeys.splice(index, 1);
  persist();
}
async function saveMessage(event) {
  const text = event.currentTarget.elements.message.value.trim(); const conversation = activeConversation.value;
  if (!text || !conversation) return;
  try {
    const result = await api('/conversations/' + conversation.id + '/messages', { method: 'POST', body: { content: text } });
    conversation.messages.push({ from: 'shelter', text: result.content, date: result.createdAt });
    event.currentTarget.reset(); messagesRevision.value += 1;
  } catch (error) { toast(error.message || 'No se pudo enviar el mensaje.'); }
}
function editProfileGroup(group) {
  profileEditor.value = group;
  profileDraft.value = { ...state.profile };
}
function cancelProfileEdit() { profileEditor.value = ''; profileDraft.value = {}; }
async function saveProfile(group) {
  savingProfile.value = true;
  try {
    if (group === 'personal') {
      const user = await api('/users/me', { method: 'PATCH', body: { fullName: profileDraft.value.contact.trim(), phone: profileDraft.value.personalPhone.trim() || null } });
      currentUser.value = user;
      state.profile.contact = user.fullName;
      state.profile.personalPhone = user.phone || '';
    } else {
      const body = {
        organizationName: group === 'identity' ? profileDraft.value.name.trim() : state.profile.name,
        description: group === 'identity' ? profileDraft.value.description || '' : state.profile.description || '',
        address: group === 'location' ? profileDraft.value.address || '' : state.profile.address || '',
        city: group === 'location' ? profileDraft.value.city || '' : state.profile.city || '',
        state: group === 'location' ? profileDraft.value.region || '' : state.profile.region || '',
        postalCode: group === 'location' ? profileDraft.value.postalCode || '' : state.profile.postalCode || '',
        contactPhone: group === 'orgContact' ? profileDraft.value.contactPhone || '' : state.profile.contactPhone || ''
      };
      const shelter = await api('/shelters/me', { method: 'PATCH', body });
      state.profile.name = shelter.organizationName;
      state.profile.description = shelter.description || '';
      state.profile.address = shelter.address || '';
      state.profile.city = shelter.city || '';
      state.profile.region = shelter.state || '';
      state.profile.postalCode = shelter.postalCode || '';
      state.profile.contactPhone = shelter.contactPhone || '';
      state.profile.verificationStatus = shelter.verificationStatus;
      state.profile.verified = shelter.verificationStatus === 'VERIFIED';
      state.profile.reviewStatus = shelter.verificationStatus === 'PENDING' ? 'En revisión' : '';
      if (group === 'identity') window.dispatchEvent(new Event('pethop:shelter-updated'));
    }
    cancelProfileEdit();
    toast('Cambios guardados en tu perfil.');
  } catch (error) { toast(error.message || 'No se pudo guardar esta sección. Tus cambios siguen disponibles para reintentar.'); }
  finally { savingProfile.value = false; }
}
watch(section, value => { document.body.classList.toggle('shelter-dashboard-active', Boolean(state.role)); document.title = `${value} | PetHop`; }, { immediate: true });
watch(() => route.query.section, value => { const target = routeSection(value); if (target && profileExists.value) { section.value = state.section = target; } });
watch(sidebarOpen, value => document.body.classList.toggle('shelter-sidebar-open', value));
watch(narrowScreen, narrow => { if (!narrow) sidebarOpen.value = false; });
onMounted(() => {
  sidebarMediaQuery = window.matchMedia('(max-width: 760px)');
  updateScreenWidth(sidebarMediaQuery);
  sidebarMediaQuery.addEventListener('change', updateScreenWidth);
  loadShelterData();
});
onUnmounted(() => { sidebarMediaQuery?.removeEventListener('change', updateScreenWidth); document.body.classList.remove('shelter-dashboard-active', 'shelter-sidebar-open'); });
watch(() => state.draft, () => persist(), { deep: true });
watch(requestFilter, value => { state.filter = value; persist(); });
watch(petFilter, value => { state.petFilter = value; persist(); });
watch(() => state.step, () => persist());
</script>

<template>
 <div class="shelter-page" :class="{ 'shelter-dashboard-active': Boolean(state.role) }" @keydown.esc.window="sidebarOpen=false">
   <SiteHeader />
   <main id="shelter-app" class="shelter-main" aria-live="polite">
   <section v-if="!state.role" class="shelter-entry"><div class="shelter-entry-copy"><span class="eyebrow">PET HOP · ADOPCIÓN RESPONSABLE</span><h1>Una adopción responsable empieza con <span>una buena conexión.</span></h1><p>Organiza publicaciones, solicitudes y seguimientos desde un solo espacio.</p></div><div class="shelter-entry-card"><span class="shelter-entry-icon"><Icon name="heart-handshake" /></span><h2>¿Cómo quieres usar PetHop?</h2><div class="shelter-choice-grid"><button type="button" class="shelter-choice" @click="router.push('/explorar.html')"><span><Icon name="search" /></span><strong>Quiero adoptar</strong><small>Descubrir mascotas y seguir mis solicitudes.</small></button><button type="button" class="shelter-choice" @click="chooseRole"><span><Icon name="paw-print" /></span><strong>Quiero dar en adopción</strong><small>Crear una cuenta y completar el perfil de tu organización.</small></button></div></div></section>
   <div v-else class="shelter-workspace"><button v-if="sidebarOpen" type="button" class="shelter-sidebar-backdrop" aria-label="Cerrar navegación del panel" @click="sidebarOpen=false"></button><aside id="shelter-sidebar" class="shelter-sidebar" :class="{ 'is-open': sidebarOpen }" :inert="narrowScreen && !sidebarOpen" :aria-hidden="narrowScreen && !sidebarOpen"><div class="shelter-account-badge"><Icon :name="state.role === 'organization' ? 'building-2' : 'user-round'" /><span>{{ state.profile.name || (state.role === 'organization' ? 'Mi organización' : 'Mi perfil') }}</span><small>{{ state.role === 'organization' ? 'Organización' : 'Particular' }}</small></div><nav aria-label="Panel de refugio"><button v-for="item in NAV_ITEMS" :key="item.label" type="button" class="shelter-nav-item" :class="{ 'is-active': section === item.label }" :aria-current="section === item.label ? 'page' : undefined" @click="go(item.label);sidebarOpen=false"><Icon :name="item.icon" /><span>{{ item.label }}</span><b v-if="item.label === 'Solicitudes'">{{ state.requests.filter(request => request.status === 'Nueva').length }}</b></button></nav><RouterLink to="/inicio.html" class="shelter-back-home"><Icon name="arrow-left" />Volver a PetHop</RouterLink></aside>
    <section class="shelter-content"><button class="shelter-mobile-menu" type="button" aria-controls="shelter-sidebar" :aria-expanded="sidebarOpen" :aria-label="sidebarOpen ? 'Cerrar navegación del panel' : 'Abrir navegación del panel'" @click="sidebarOpen=!sidebarOpen"><Icon :name="sidebarOpen ? 'x' : 'menu'" /></button>
      <template v-if="!profileExists">
        <div class="shelter-section-header"><div><span class="eyebrow">PRIMER PASO</span><h1>Completa el perfil de tu organización</h1><p>Los datos del refugio se guardan por separado de los de tu cuenta personal.</p></div></div>
        <form class="shelter-panel shelter-profile-setup" @submit.prevent="createOrganizationProfile"><label class="shelter-field">Nombre de la organización<input v-model="profileSetup.organizationName" maxlength="180" placeholder="Nombre público del refugio" required></label><label class="shelter-field">Descripción<textarea v-model="profileSetup.description" rows="3" maxlength="4000" placeholder="Describe la labor de tu refugio (opcional)"></textarea></label><div class="shelter-form-grid"><label class="shelter-field">Ciudad<input v-model="profileSetup.city" maxlength="100"></label><label class="shelter-field">Estado<input v-model="profileSetup.state" maxlength="100"></label><label class="shelter-field">Dirección<input v-model="profileSetup.address" maxlength="240"></label><label class="shelter-field">Código postal<input v-model="profileSetup.postalCode" maxlength="20"></label><label class="shelter-field">Teléfono de contacto de la organización<input v-model="profileSetup.contactPhone" maxlength="32" type="tel"></label></div><p class="shelter-profile-setup-note"><Icon name="info" /> Tu nombre personal, teléfono y correo ya pertenecen a tu cuenta. Aquí solo registramos la información de la organización.</p><div class="shelter-form-actions"><button class="button button-primary" type="submit" :disabled="savingProfile">{{ savingProfile?'Guardando…':'Crear perfil de organización' }}<Icon name="arrow-right" /></button></div></form>
      </template>
      <template v-else-if="section === 'Inicio'">
        <div class="shelter-section-header">
          <div><span class="eyebrow">PANEL DE ADOPCIONES</span><h1 class="shelter-greeting">Hola<span v-if="state.profile.name">, {{ state.profile.name.split(' ')[0] }}</span></h1><p>Aquí tienes un resumen de tus adopciones y lo que requiere tu atención.</p></div>
          <button type="button" class="button button-primary" @click="go('Publicar mascota')"><Icon name="plus" /> Publicar mascota</button>
        </div>
        <div class="shelter-stat-grid">
          <button v-for="item in overviewStats" :key="item.label" type="button" class="shelter-stat-card" @click="go(item.section)"><span><Icon :name="item.icon" /></span><small>{{ item.label }}</small><strong>{{ item.value }}</strong></button>
        </div>
        <div class="shelter-home-grid">
          <section class="shelter-panel">
            <div class="shelter-panel-title"><div><span class="eyebrow">ACTIVIDAD RECIENTE</span><h2>Solicitudes por revisar</h2></div><button type="button" class="shelter-text-action" @click="go('Solicitudes')">Ver todas <Icon name="arrow-up-right" /></button></div>
            <button v-for="item in recentRequests" :key="item.id" type="button" class="shelter-request-row" @click="selectedRequest=item"><span class="shelter-request-avatar">{{ item.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span class="shelter-request-copy"><strong>{{ item.adopter }}</strong><small>{{ item.pet }} · {{ new Date(item.date).toLocaleDateString('es-MX') }}</small></span><span class="shelter-status">{{ item.status }}</span><Icon name="chevron-right" /></button>
            <div v-if="!recentRequests.length" class="shelter-empty"><Icon name="inbox" /><strong>Todo al día</strong><p>No hay solicitudes por revisar.</p></div>
          </section>
          <section class="shelter-panel shelter-journey"><span class="eyebrow">EL PROCESO</span><h2>De la primera consulta al nuevo hogar</h2><div class="shelter-mini-timeline"><div v-for="(step,index) in ['Solicitud','Evaluación','Comunicación','Encuentro','Seguimiento']" :key="step"><span>{{ index+1 }}</span><small>{{ step }}</small></div></div><button type="button" class="button button-outline" @click="go('Seguimiento')">Ver seguimiento <Icon name="arrow-right" /></button></section>
        </div>
        <section class="shelter-panel"><div class="shelter-panel-title"><div><span class="eyebrow">TUS PUBLICACIONES</span><h2>Mascotas recientes</h2></div><button type="button" class="shelter-text-action" @click="go('Mis mascotas')">Ver todas <Icon name="arrow-up-right" /></button></div><div class="shelter-pet-grid"><article v-for="pet in recentPets" :key="pet.id" class="shelter-pet-card" tabindex="0" role="button" :aria-label="'Administrar publicación de ' + pet.name" @click="openPet(pet)" @keydown.enter="openPet(pet)" @keydown.space.prevent="openPet(pet)"><ImageWithSkeleton frame-class="shelter-pet-photo" :src="shelterPetPhoto(pet)" :alt="`Fotografía de ${pet.name}`" image-class="shelter-pet-image" role="img"><span v-if="!shelterPetPhoto(pet)" class="shelter-pet-no-photo" aria-hidden="true"><Icon name="paw-print" /><small>Sin fotografía</small></span></ImageWithSkeleton><div class="shelter-pet-card-body"><div class="shelter-card-line"><span class="shelter-status">{{ pet.status }}</span><small>{{ pet.days || 0 }} días publicada</small></div><h3>{{ pet.name }}</h3><p>{{ pet.species }} · {{ pet.age }} · {{ pet.gender }}</p><div class="shelter-card-line shelter-pet-request-count"><span><Icon name="inbox" /> {{ pet.requests || 0 }} solicitudes</span></div></div></article><div v-if="!recentPets.length" class="shelter-empty"><Icon name="paw-print" /><strong>Sin publicaciones</strong><p>Publica una mascota para comenzar a organizar sus solicitudes.</p></div></div></section>
      </template>
      <template v-else-if="section === 'Mis mascotas' || section === 'Completadas'"><div class="shelter-section-header"><div><span class="eyebrow">TUS PUBLICACIONES</span><h1>{{ section }}</h1><p>Consulta su estado, las solicitudes recibidas y el tiempo de publicación.</p></div><button class="button button-primary" @click="go('Publicar mascota')"><Icon name="plus" /> Publicar mascota</button></div><div v-if="section==='Mis mascotas'" class="shelter-filter-pills"><button v-for="filter in ['Todas','Publicada','En revisión','En proceso de adopción','Adoptada','Pausada']" :key="filter" :class="{ 'is-selected': petFilter===filter }" @click="petFilter=filter">{{ filter }}</button></div><div class="shelter-pet-grid"><article v-for="pet in (section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets)" :key="pet.id" class="shelter-pet-card" tabindex="0" role="button" :aria-label="`Ver detalles de ${pet.name}`" @click="openPet(pet)" @keydown.enter="openPet(pet)" @keydown.space.prevent="openPet(pet)"><ImageWithSkeleton frame-class="shelter-pet-photo" :src="shelterPetPhoto(pet)" :alt="`Fotografía de ${pet.name}`" image-class="shelter-pet-image" role="img"><span v-if="!shelterPetPhoto(pet)" class="shelter-pet-no-photo" aria-hidden="true"><Icon name="paw-print" /><small>Sin fotografía</small></span></ImageWithSkeleton><div class="shelter-pet-card-body"><span class="shelter-status">{{ pet.status }}</span><h3>{{ pet.name }}</h3><p>{{ pet.species }} · {{ pet.age }} · {{ pet.gender }}</p><p>{{ pet.days || 0 }} días publicada · {{ pet.requests || 0 }} solicitudes</p></div></article><p v-if="!(section==='Completadas' ? state.pets.filter(item=>item.status==='Adoptada') : visiblePets).length" class="shelter-empty">{{ section==='Completadas' ? 'Aquí veremos las adopciones completadas.' : 'No hay mascotas en este estado. Prueba otro filtro o publica una mascota.' }}</p></div></template>
      <template v-else-if="section === 'Publicar mascota'">
        <div class="shelter-section-header"><div><span class="eyebrow">NUEVA PUBLICACIÓN</span><h1>Publicar mascota</h1><p>Completa la información paso a paso para presentar su perfil de forma clara.</p></div></div>
        <section class="shelter-panel shelter-publish-panel">
          <div class="shelter-steps"><button v-for="(label,index) in steps" :key="label" type="button" class="shelter-step" :class="{ 'is-current':state.step===index,'is-done':index<state.step }" :disabled="index>state.step" @click="state.step=index"><span><Icon v-if="index<state.step" name="check" /><template v-else>{{ String(index+1).padStart(2,'0') }}</template></span><small>{{ label }}</small></button></div>
          <div class="shelter-progress"><span :style="{ width: `${completion}%` }"></span></div>
          <form class="shelter-form-grid" @submit.prevent="advancePublish">
            <h2>{{ steps[state.step] }}</h2><p class="shelter-step-hint">Paso {{ state.step+1 }} de {{ steps.length }}</p>
            <template v-if="state.step===6">
              <div class="shelter-review-card"><div class="shelter-review-progress"><div><strong>Progreso del proceso: {{ completion }}%</strong><p>La barra refleja el paso actual; el último paso representa el 100%.</p></div></div><div class="shelter-review-pet"><img v-if="state.draft.photos?.[0]" :src="state.draft.photos[0]" :alt="`Fotografía de ${state.draft.name}`"><div><h3>{{ state.draft.name || 'Nombre de la mascota' }}</h3><p>{{ state.draft.species || 'Especie por agregar' }} · {{ state.draft.lifeStage || 'Etapa por confirmar' }} · {{ state.draft.location || 'Ubicación por confirmar' }}</p><p>Revisa los datos antes de publicar. Podrás actualizarlos después.</p></div></div></div>
            </template>
            <template v-else>
              <template v-for="field in stepFields[state.step]" :key="field[0]">
                <label v-if="field[2]==='text' || field[2]==='number'" class="shelter-field">{{ field[1] }}<input v-model="state.draft[field[0]]" :type="field[2]" :placeholder="field[3]" :required="['name'].includes(field[0])" :min="field[2]==='number' ? '0.01' : undefined" step="any"></label>
                <label v-else-if="field[2]==='select'" class="shelter-field">{{ field[1] }}<select v-model="state.draft[field[0]]" :required="['species','lifeStage','location'].includes(field[0])"><option value="">Selecciona</option><option v-for="option in field[3].split('|')" :key="option">{{ option }}</option></select></label>
                <label v-else-if="field[2]==='textarea'" class="shelter-field" :class="{ 'shelter-form-wide': field[0]==='description' || field[0]==='story' || field[0]==='background' }">{{ field[1] }}<textarea v-model="state.draft[field[0]]" :placeholder="field[3]" rows="3" :required="field[0]==='description'"></textarea></label>
                <fieldset v-else-if="field[2]==='checks'" class="shelter-field shelter-checks shelter-form-wide"><legend>{{ field[1] }}</legend><label v-for="option in field[3].split('|')" :key="option" class="shelter-check-option" :class="{ 'is-selected': state.draft[field[0]].includes(option) }"><input v-model="state.draft[field[0]]" type="checkbox" :value="option"><span>{{ option }}</span></label></fieldset>
                <div v-else-if="field[2]==='file'" class="shelter-field shelter-photo-field shelter-form-wide"><label for="publish-photos">Fotografías</label><input id="publish-photos" class="shelter-photo-input" type="file" accept="image/*" multiple @change="processPhotos"><small>Hasta 6 fotografías optimizadas y guardadas en PetHop (máximo 10 MB cada una).</small><div v-if="state.draft.photos.length" class="shelter-photo-preview"><article v-for="(photo,index) in state.draft.photos" :key="state.draft.photoKeys[index] || index" class="shelter-draft-photo"><img :src="photo" :alt="`Fotografía seleccionada ${index+1}`"><button type="button" :aria-label="`Quitar fotografía ${index+1}`" @click="removeDraftPhoto(index)"><Icon name="x" /> Quitar</button></article></div></div>
              </template>
            </template>
            <label v-if="state.step===6" class="shelter-ack"><input name="reviewed" type="checkbox" required> Confirmo que la información es correcta y puedo responder las solicitudes de adopción.</label>
            <div class="shelter-form-actions"><button v-if="state.step>0" type="button" class="button button-outline" @click="state.step--;persist()"><Icon name="arrow-left" /> Anterior</button><span v-else></span><button type="submit" class="button button-primary"><template v-if="state.step<6">Continuar <Icon name="arrow-right" /></template><template v-else>Publicar mascota</template></button></div>
          </form>
        </section>
      </template>
      <template v-else-if="['Solicitudes','En proceso','Seguimiento'].includes(section)"><div class="shelter-section-header"><div><span class="eyebrow">{{ section==='Seguimiento'?'ACOMPAÑAMIENTO':'EVALUACIÓN RESPONSABLE' }}</span><h1>{{ section === 'Solicitudes' ? 'Solicitudes de adopción' : section==='Seguimiento'?'Seguimiento de adopción':section }}</h1><p>{{ section==='Seguimiento'?'Adoptante y persona que da en adopción comparten las etapas del mismo proceso.':'Conoce a cada familia y continúa el proceso con calma.' }}</p></div></div><template v-if="section==='Solicitudes'"><div class="shelter-filter-pills"><button v-for="filter in ['Todas','Nuevas','En revisión','Aceptadas','Rechazadas']" :key="filter" :class="{ 'is-selected':requestFilter===filter }" @click="requestFilter=filter">{{ filter }}<small>{{ filter==='Todas'?state.requests.length:state.requests.filter(item=>item.status===({Nuevas:'Nueva',Aceptadas:'Aceptada'}[filter]||filter)).length }}</small></button></div><section class="shelter-panel"><button v-for="item in visibleRequests" :key="item.id" class="shelter-request-row" @click="selectedRequest=item"><span class="shelter-request-avatar">{{ item.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ item.adopter }}</strong><small>{{ item.pet }} · {{ new Date(item.date).toLocaleDateString('es-MX') }}</small></span><span class="shelter-status">{{ item.status }}</span><Icon name="chevron-right" /></button><div v-if="!visibleRequests.length" class="shelter-empty"><Icon name="clipboard-check" /><strong>No hay solicitudes</strong><p>Las solicitudes aparecerán aquí al llegar.</p></div></section></template><section v-else-if="section==='En proceso'" class="shelter-panel shelter-pipeline-list"><article v-for="item in visibleRequests" :key="item.id" class="shelter-pipeline-card"><div><span class="shelter-status">{{ item.status==='Aceptada'?'Evaluación aceptada':'En proceso' }}</span><h3>{{ item.pet }} · {{ item.adopter }}</h3><p>Próximo paso: acordar el encuentro y preparar la adopción.</p></div><button class="button button-outline" @click="selectedRequest=item">Ver solicitud <Icon name="arrow-right" /></button></article><div v-if="!visibleRequests.length" class="shelter-empty"><Icon name="refresh-cw" /><strong>Aún no hay adopciones en curso</strong><p>Al aceptar una solicitud, podrás coordinar la evaluación y el encuentro.</p></div></section><section v-else class="shelter-panel"><article v-for="item in visibleRequests" :key="item.id" class="shelter-track-card"><div class="shelter-track-heading"><div><span class="shelter-status">{{ item.status }}</span><h2>{{ item.pet }} · {{ item.adopter }}</h2></div><button type="button" class="shelter-text-action" @click="selectedRequest=item">Abrir solicitud <Icon name="arrow-right" /></button></div><ol class="shelter-timeline"><li v-for="(step,index) in trackingSteps" :key="step" :class="{ 'is-complete':index <= (item.status==='Aceptada'?2:3) }"><span><Icon v-if="index <= (item.status==='Aceptada'?2:3)" name="check" /><span v-else>{{ index+1 }}</span></span><div><strong>{{ step }}</strong><small v-if="index===0">{{ new Date(item.date).toLocaleDateString('es-MX') }}</small></div></li></ol></article><div v-if="!visibleRequests.length" class="shelter-empty"><Icon name="route" /><strong>Sin seguimientos activos</strong><p>Los seguimientos de solicitudes aceptadas se organizarán aquí.</p></div></section></template>
      <template v-else-if="section==='Mensajes'"><div class="shelter-section-header"><div><span class="eyebrow">COMUNICACIÓN</span><h1>Mensajes</h1><p>Conversaciones sencillas vinculadas a una mascota y su solicitud.</p></div></div><div class="shelter-messages-layout"><section class="shelter-panel shelter-conversation-list"><h2>Conversaciones</h2><button v-for="thread in conversations" :key="thread.id" class="shelter-conversation" :class="{ 'is-selected':(activeConversation?.id===thread.id) }" @click="selectedMessageId=state.activeMessage=thread.id;persist()"><span class="shelter-request-avatar">{{ thread.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><span><strong>{{ thread.adopter }}</strong><small>{{ thread.pet }} · {{ thread.status }}</small></span></button><div v-if="!conversations.length" class="shelter-empty"><Icon name="messages-square" /><strong>Sin conversaciones</strong><p>Al contactar a una familia podrás continuar la conversación aquí.</p></div></section><section class="shelter-panel shelter-chat-panel"><template v-if="activeConversation"><div class="shelter-chat-head"><span class="shelter-request-avatar">{{ activeConversation.adopter.split(' ').map(part=>part[0]).slice(0,2).join('') }}</span><div><strong>{{ activeConversation.adopter }}</strong><small>Sobre: {{ activeConversation.pet }}</small><small v-if="activeConversation.petLocation">Ubicación: {{ activeConversation.petLocation }}</small></div></div><div class="shelter-chat-transcript" aria-live="polite"><p v-for="(message,index) in activeConversation.messages" :key="index" :class="{ 'is-outgoing':message.from==='shelter' }">{{ message.text }}<small>{{ new Date(message.date).toLocaleString('es-MX') }}</small></p><p v-if="!activeConversation.messages.length" class="shelter-chat-note">Inicia la conversación con {{ activeConversation.adopter }} sobre {{ activeConversation.pet }}.</p></div><form class="shelter-chat-form" @submit.prevent="saveMessage"><label class="sr-only" for="shelter-message">Escribe un mensaje</label><textarea id="shelter-message" name="message" rows="2" placeholder="Escribe un mensaje..." required></textarea><button class="button button-primary">Enviar</button></form></template><div v-else class="shelter-chat-empty"><Icon name="messages-square" /><h3>Comunicación clara, adopciones responsables</h3><p>Selecciona una conversación para enviar un mensaje sobre la mascota.</p></div></section></div></template>
      <template v-else-if="section==='Perfil / Organización'">
        <div class="shelter-section-header"><div><span class="eyebrow">IDENTIDAD Y CONFIANZA</span><h1>{{ state.profile.name || 'Perfil de organización' }}</h1><p>Información de la organización, su equipo y actividad en PetHop.</p></div></div>
        <div class="shelter-profile-layout">
          <section class="shelter-panel shelter-profile-card">
            <div class="shelter-org-identity"><span class="shelter-org-avatar"><Icon name="building-2" /></span><div><span class="eyebrow">ORGANIZACIÓN</span><h2>{{ state.profile.name || 'Nombre pendiente' }}</h2><p>{{ [state.profile.city,state.profile.region].filter(Boolean).join(', ') || 'Ubicación pendiente' }}</p></div><span class="shelter-org-verification" :class="{ 'is-verified':state.profile.verified }"><Icon :name="state.profile.verified?'badge-check':'shield-check'" />{{ state.profile.verified?'Verificada':state.profile.verificationStatus==='REJECTED'?'Verificación rechazada':'Pendiente de verificación' }}</span><button type="button" class="button button-outline shelter-profile-logout" @click="signOut"><Icon name="log-out" />Cerrar sesión</button></div>
            <section class="shelter-profile-group"><div class="shelter-profile-group-heading"><div><h3>Identidad</h3><p>Nombre público y presentación del refugio.</p></div><button v-if="profileEditor!=='identity'" type="button" class="shelter-profile-edit" aria-label="Editar identidad de la organización" @click="editProfileGroup('identity')"><Icon name="pencil" /></button></div><form v-if="profileEditor==='identity'" class="shelter-form-grid shelter-profile-edit-grid" @submit.prevent="saveProfile('identity')"><label class="shelter-field">Nombre de la organización<input v-model="profileDraft.name" required maxlength="180"></label><label class="shelter-field">Descripción<textarea v-model="profileDraft.description" rows="3" maxlength="4000"></textarea></label><div class="shelter-form-actions"><button class="button button-outline" type="button" @click="cancelProfileEdit">Cancelar</button><button class="button button-primary" type="submit" :disabled="savingProfile">{{ savingProfile?'Guardando…':'Guardar' }}</button></div></form><dl v-else class="shelter-profile-data"><div><dt>Nombre</dt><dd>{{ state.profile.name || 'No proporcionado' }}</dd></div><div class="shelter-profile-data-wide"><dt>Descripción</dt><dd>{{ state.profile.description || 'Sin descripción proporcionada.' }}</dd></div></dl></section>
            <section class="shelter-profile-group"><div class="shelter-profile-group-heading"><div><h3>Contacto personal</h3><p>Datos de la cuenta que administra la organización.</p></div><button v-if="profileEditor!=='personal'" type="button" class="shelter-profile-edit" aria-label="Editar contacto personal" @click="editProfileGroup('personal')"><Icon name="pencil" /></button></div><form v-if="profileEditor==='personal'" class="shelter-form-grid shelter-profile-edit-grid" @submit.prevent="saveProfile('personal')"><label class="shelter-field">Persona de contacto<input v-model="profileDraft.contact" required maxlength="160"></label><label class="shelter-field">Teléfono personal<input v-model="profileDraft.personalPhone" maxlength="32"></label><label class="shelter-field">Correo electrónico<input :value="state.profile.email" disabled readonly></label><div class="shelter-form-actions"><button class="button button-outline" type="button" @click="cancelProfileEdit">Cancelar</button><button class="button button-primary" type="submit" :disabled="savingProfile">{{ savingProfile?'Guardando…':'Guardar' }}</button></div></form><dl v-else class="shelter-profile-data"><div><dt>Persona de contacto</dt><dd>{{ state.profile.contact || 'No proporcionada' }}</dd></div><div><dt>Teléfono personal</dt><dd>{{ state.profile.personalPhone || 'No proporcionado' }}</dd></div><div><dt>Correo electrónico</dt><dd>{{ state.profile.email || 'No disponible' }} <small>Dato de cuenta de solo lectura</small></dd></div></dl></section>
            <section class="shelter-profile-group shelter-password-group"><div class="shelter-profile-group-heading"><div><h3>Cambiar contraseña</h3><p>Usa tu contraseña actual para establecer una nueva.</p></div></div><form class="shelter-password-form" @submit.prevent="changePassword"><label class="shelter-field">Contraseña actual<input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" required></label><label class="shelter-field">Nueva contraseña<input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label><label class="shelter-field">Confirmar contraseña<input v-model="passwordForm.confirm" type="password" autocomplete="new-password" minlength="12" maxlength="72" required></label><div class="shelter-form-actions"><button class="button button-primary" type="submit" :disabled="passwordSaving">{{ passwordSaving?'Actualizando…':'Actualizar contraseña' }}</button></div><p v-if="passwordFeedback" class="shelter-password-feedback" role="status">{{ passwordFeedback }}</p></form></section>
            <section class="shelter-profile-group"><div class="shelter-profile-group-heading"><div><h3>Contacto de la organización</h3><p>Canal público para las familias adoptantes.</p></div><button v-if="profileEditor!=='orgContact'" type="button" class="shelter-profile-edit" aria-label="Editar teléfono de contacto de la organización" @click="editProfileGroup('orgContact')"><Icon name="pencil" /></button></div><form v-if="profileEditor==='orgContact'" class="shelter-form-grid shelter-profile-edit-grid" @submit.prevent="saveProfile('orgContact')"><label class="shelter-field">Teléfono de contacto<input v-model="profileDraft.contactPhone" maxlength="32"></label><div class="shelter-form-actions"><button class="button button-outline" type="button" @click="cancelProfileEdit">Cancelar</button><button class="button button-primary" type="submit" :disabled="savingProfile">{{ savingProfile?'Guardando…':'Guardar' }}</button></div></form><dl v-else class="shelter-profile-data"><div><dt>Teléfono de la organización</dt><dd>{{ state.profile.contactPhone || 'No proporcionado' }}</dd></div></dl></section>
            <section class="shelter-profile-group"><div class="shelter-profile-group-heading"><div><h3>Ubicación</h3><p>Dirección registrada para la organización.</p></div><button v-if="profileEditor!=='location'" type="button" class="shelter-profile-edit" aria-label="Editar ubicación" @click="editProfileGroup('location')"><Icon name="pencil" /></button></div><form v-if="profileEditor==='location'" class="shelter-form-grid shelter-profile-edit-grid" @submit.prevent="saveProfile('location')"><label class="shelter-field">Ciudad<input v-model="profileDraft.city" maxlength="100"></label><label class="shelter-field">Estado<input v-model="profileDraft.region" maxlength="100"></label><label class="shelter-field">Dirección<input v-model="profileDraft.address" maxlength="240"></label><label class="shelter-field">Código postal<input v-model="profileDraft.postalCode" maxlength="20"></label><div class="shelter-form-actions"><button class="button button-outline" type="button" @click="cancelProfileEdit">Cancelar</button><button class="button button-primary" type="submit" :disabled="savingProfile">{{ savingProfile?'Guardando…':'Guardar' }}</button></div></form><dl v-else class="shelter-profile-data"><div><dt>Ciudad</dt><dd>{{ state.profile.city || 'No proporcionada' }}</dd></div><div><dt>Estado</dt><dd>{{ state.profile.region || 'No proporcionado' }}</dd></div><div><dt>Dirección</dt><dd>{{ state.profile.address || 'No proporcionada' }}</dd></div><div><dt>Código postal</dt><dd>{{ state.profile.postalCode || 'No proporcionado' }}</dd></div></dl></section>
          </section>
          <aside class="shelter-profile-aside-column"><section class="shelter-panel shelter-profile-aside"><span class="shelter-aside-icon"><Icon name="shield-check" /></span><h3>Confianza para las familias</h3><p>El estado de verificación se administra desde PetHop y no puede editarse desde este perfil.</p><p v-if="state.profile.reviewStatus">Tu perfil está {{ state.profile.reviewStatus.toLocaleLowerCase('es-MX') }}.</p></section><section class="shelter-panel shelter-org-activity"><span class="eyebrow">ACTIVIDAD</span><div><strong>{{ state.profile.metrics?.pets ?? 0 }}</strong><span>Mascotas publicadas</span></div><div><strong>{{ state.profile.metrics?.applications ?? 0 }}</strong><span>Solicitudes recibidas</span></div><div><strong>{{ state.profile.metrics?.adoptions ?? 0 }}</strong><span>Adopciones formalizadas</span></div></section></aside>
        </div>
      </template>
      <template v-else-if="section==='Configuración'"><div class="shelter-section-header"><div><span class="eyebrow">PREFERENCIAS</span><h1>Configuración</h1><p>Ajusta la experiencia de tu espacio de adopciones.</p></div></div><section class="shelter-panel shelter-settings"><label class="shelter-setting-row"><span><strong>Notificaciones de nuevas solicitudes</strong><small>Preferencia de visualización guardada en este navegador.</small></span><input v-model="state.notify" type="checkbox" @change="persist()"></label><p class="shelter-data-note">Mascotas, solicitudes y perfiles se guardan en PostgreSQL a través de la API.</p></section></template>
      <template v-else><div class="shelter-section-header"><div><span class="eyebrow">CENTRO DE AYUDA</span><h1>¿Cómo podemos <span>ayudarte?</span></h1><p>Guía rápida para organizar una adopción responsable.</p></div></div><div class="shelter-help-grid"><article v-for="[number,title,copy] in [['01','Publica con claridad','Completa salud, personalidad, historia y requisitos para orientar a las familias.'],['02','Evalúa cada solicitud','Revisa las condiciones del hogar y conversa con el adoptante antes de decidir.'],['03','Coordina el encuentro','Usa el seguimiento para preparar la entrega y continuar en contacto después.'],['04','Cuida tus datos','Los registros de mascotas, solicitudes y mensajes se consultan desde el servidor.']]" :key="number" class="shelter-help-card"><span>{{ number }}</span><h2>{{ title }}</h2><p>{{ copy }}</p></article></div></template>
    </section></div>
   </main>
   <div v-if="selectedPet" class="shelter-modal-backdrop shelter-pet-edit-backdrop" @click.self="selectedPet=null"><section class="shelter-request-modal shelter-pet-details-modal" role="dialog" aria-modal="true" aria-labelledby="pet-details-title"><button type="button" class="shelter-modal-close" aria-label="Cerrar" @click="selectedPet=null; editingPet=false"><Icon name="x" /></button><span class="eyebrow">{{ editingPet ? 'EDITAR PUBLICACIÓN' : 'DETALLES DE PUBLICACIÓN' }}</span><div class="shelter-pet-modal-title"><h2 id="pet-details-title">{{ editingPet ? 'Editar información' : selectedPet.name }}</h2><span class="shelter-status" :class="{ 'shelter-status-review': editingPet || selectedPet.status === 'En revisión' }">{{ editingPet ? 'En revisión al guardar' : selectedPet.status }}</span></div><p class="shelter-request-lead">{{ editingPet ? `Actualiza el perfil de ${selectedPet.name}.` : `${selectedPet.species} · ${selectedPet.age} · ${selectedPet.gender} · ${selectedPet.location || 'Ubicación por confirmar'}` }}</p>
     <template v-if="editingPet">
       <form class="shelter-pet-edit-form" @submit.prevent="savePetInformation">
         <div class="shelter-pet-edit-grid">
           <label class="shelter-field">Nombre<input v-model="petEditForm.name" required></label>
           <label class="shelter-field">Especie<select v-model="petEditForm.species" required><option>Perro</option><option>Gato</option><option>Otro</option></select></label>
           <label class="shelter-field">Etapa de vida<select v-model="petEditForm.lifeStage"><option value="">Por confirmar</option><option>Cachorro</option><option>Joven</option><option>Adulto</option><option>Senior</option></select></label>
           <label class="shelter-field">Sexo<select v-model="petEditForm.gender"><option value="">Sin especificar</option><option>Hembra</option><option>Macho</option><option>Sin especificar</option></select></label>
           <label class="shelter-field">Tamaño<select v-model="petEditForm.size"><option value="">Por confirmar</option><option>Pequeño</option><option>Mediano</option><option>Grande</option></select></label>
           <label class="shelter-field">Raza<input v-model="petEditForm.breed" placeholder="Raza o mezcla, si se conoce"></label>
           <label class="shelter-field">Peso (kg)<input v-model="petEditForm.weight" type="number" min="0.01" step="any" placeholder="Ej. 8.5"></label>
           <label class="shelter-field">Ubicación<select v-model="petEditForm.location" required><option value="" disabled>Selecciona una ubicación</option><option v-for="location in petLocationOptions" :key="location" :value="location">{{ location }}</option></select></label>
           <label class="shelter-field">Vacunación<select v-model="petEditForm.vaccination"><option>Pendiente de confirmar</option><option>Al día</option><option>No vacunada</option><option>No</option></select></label>
           <label class="shelter-field">Desparasitación<select v-model="petEditForm.deworming"><option>Pendiente de confirmar</option><option>Al día</option><option>No</option></select></label>
           <label class="shelter-field">Esterilización<select v-model="petEditForm.sterilization"><option>Pendiente de confirmar</option><option>Sí</option><option>No</option></select></label>
           <label class="shelter-field">Condición médica<input v-model="petEditForm.healthCondition" placeholder="Diagnóstico o condición conocida"></label>
           <label class="shelter-field shelter-pet-edit-wide">Observaciones de salud<textarea v-model="petEditForm.healthSummary" rows="2" placeholder="Tratamientos o notas relevantes"></textarea></label>
           <fieldset class="shelter-field shelter-checks shelter-pet-edit-wide"><legend>Personalidad y convivencia</legend><label v-for="trait in PERSONALITY_OPTIONS" :key="trait" class="shelter-check-option" :class="{ 'is-selected': petEditForm.personalityTraits.includes(trait) }"><input v-model="petEditForm.personalityTraits" type="checkbox" :value="trait"><span>{{ trait }}</span></label></fieldset>
           <label class="shelter-field shelter-pet-edit-wide">Descripción<textarea v-model="petEditForm.description" rows="3" placeholder="Describe a la mascota"></textarea></label>
           <label class="shelter-field shelter-pet-edit-wide">¿Cómo llegó a la adopción?<textarea v-model="petEditForm.story" rows="3" placeholder="Cuenta cómo llegó a estar en adopción"></textarea></label>
           <label class="shelter-field shelter-pet-edit-wide">Antecedentes relevantes<textarea v-model="petEditForm.background" rows="3" placeholder="Información conocida sobre su historia"></textarea></label>
           <label class="shelter-field">Tipo de hogar<input v-model="petEditForm.homeType" placeholder="Casa, departamento u otro"></label>
           <label class="shelter-field">Experiencia recomendada<input v-model="petEditForm.experience" placeholder="Experiencia útil para su cuidado"></label>
           <label class="shelter-field shelter-pet-edit-wide">Condiciones necesarias<textarea v-model="petEditForm.conditions" rows="2" placeholder="Espacio, rutina o cuidados que requiere"></textarea></label>
           <label class="shelter-field shelter-pet-edit-wide">Otros requisitos<textarea v-model="petEditForm.requirements" rows="2" placeholder="Requisitos adicionales de adopción"></textarea></label>
         </div>
         <section class="shelter-edit-photos shelter-pet-edit-wide" aria-label="Fotografías de la mascota"><div class="shelter-edit-photos-heading"><strong>Fotografías</strong><span>{{ petEditForm.photosToKeep.length + petEditForm.newPhotos.length }} de 6</span></div><p class="shelter-modal-note">Agrega imágenes o marca las existentes para quitarlas. Los cambios se aplicarán al guardar.</p><div v-if="petEditForm.photosToKeep.length || petEditForm.newPhotos.length" class="shelter-edit-photo-grid"><article v-for="photo in petEditForm.photosToKeep" :key="photo.id" class="shelter-edit-photo"><img :src="photo.displayUrl" :alt="`Fotografía de ${selectedPet.name}`"><button type="button" aria-label="Quitar fotografía" @click="removeEditPhoto(photo.id)"><Icon name="x" /> Quitar</button></article><article v-for="(photo,index) in petEditForm.newPhotos" :key="`new-${index}`" class="shelter-edit-photo"><img :src="photo.dataUrl" :alt="`Fotografía nueva ${index+1} de ${selectedPet.name}`"><button type="button" aria-label="Quitar foto nueva" @click="removeNewEditPhoto(index)"><Icon name="x" /> Quitar</button></article></div><label class="shelter-field shelter-edit-photo-input">Agregar fotos<input type="file" accept="image/*" multiple :disabled="petEditForm.photosToKeep.length + petEditForm.newPhotos.length >= 6" @change="addEditPhotos"><small>Hasta 6 fotos en total; se optimizan antes de subir.</small></label></section>
         <div class="shelter-modal-actions shelter-pet-edit-actions"><button type="button" class="button button-outline" :disabled="savingPetChanges" @click="editingPet=false">Cancelar</button><button type="submit" class="button button-primary" :disabled="savingPetChanges"><Icon name="check" /> {{ savingPetChanges ? 'Guardando…' : 'Guardar cambios' }}</button></div>
         <p class="shelter-modal-note shelter-pet-review-note"><Icon name="shield-check" /> Al guardar, la publicación pasará a En revisión mientras se verifican los cambios.</p>
       </form>
     </template>
     <template v-else>
       <div class="shelter-applicant-grid"><article><small>Estado de publicación</small><p>{{ selectedPet.status }}</p></article><article><small>Actividad</small><p>{{ selectedPet.requests || 0 }} solicitudes</p></article></div><label class="shelter-field">Actualizar estado<select v-model="selectedPetStatus"><option v-for="status in ['Publicada','Borrador','Pausada']" :key="status">{{ status }}</option></select></label><p class="shelter-modal-note">El proceso y el cierre de adopción se actualizan al revisar la solicitud y formalizar el acuerdo.</p><div class="shelter-modal-actions shelter-pet-details-actions"><RouterLink class="button button-outline" :to="{ path: '/salud.html', query: { pet: selectedPet.id } }" @click="selectedPet=null"><Icon name="clipboard-list" /> Expediente sanitario</RouterLink><button type="button" class="button button-danger-outline" @click="deletePet(selectedPet)"><Icon name="x" /> Eliminar mascota</button><button type="button" class="button button-outline" @click="selectedPet=null">Cerrar</button><button type="button" class="button button-outline" @click="editPetInformation">Editar información</button><button type="button" class="button button-primary" @click="savePetStatus">Guardar estado</button></div>
     </template>
   </section></div>
   <div v-if="selectedRequest" class="shelter-modal-backdrop" @click.self="selectedRequest=null"><section class="shelter-request-modal" role="dialog" aria-modal="true" aria-labelledby="request-title"><button class="shelter-modal-close" aria-label="Cerrar" @click="selectedRequest=null"><Icon name="x" /></button><span class="eyebrow">SOLICITUD DE ADOPCIÓN</span><h2 id="request-title">{{ selectedRequest.adopter }} <span class="shelter-status">{{ selectedRequest.status }}</span></h2><p class="shelter-request-lead">Solicitud para <strong>{{ selectedRequest.pet }}</strong> · {{ new Date(selectedRequest.date).toLocaleDateString('es-MX') }}</p><div class="shelter-applicant-grid"><article v-for="[label,value] in [['Tipo de vivienda',selectedRequest.home],['Experiencia con mascotas',selectedRequest.experience],['Personas en el hogar',selectedRequest.household],['Motivo de adopción',selectedRequest.reason],['Condiciones del hogar',selectedRequest.conditions],['Información relevante',selectedRequest.notes]]" :key="label"><small>{{ label }}</small><p>{{ value || 'No proporcionado' }}</p></article></div><div class="shelter-modal-actions"><button class="button button-outline" @click="requestAction('contact')">Contactar</button><button class="button button-outline" @click="requestAction('review')">Continuar revisión</button><button class="button button-primary" @click="requestAction('accept')">Aceptar solicitud</button><button v-if="selectedRequest.status === 'Aceptada'" class="button button-primary" @click="formalizeAdoption(selectedRequest)">Formalizar adopción</button><button class="button button-danger" @click="requestAction('reject')">Rechazar</button></div><p class="shelter-modal-note">Aceptar inicia la coordinación; la adopción se completa después del encuentro y seguimiento.</p></section></div>
   <div v-if="notice" class="shelter-toast is-visible" role="status">{{ notice }}</div>
 </div>
</template>

<style scoped>
.shelter-review-banner { display:flex; align-items:center; gap:14px; margin:0 0 20px; padding:17px 19px; border:1px solid #b8ded4; border-radius:16px; background:#effaf5; box-shadow:0 5px 16px rgba(18,63,75,.05); }
.shelter-review-banner-icon { display:grid; width:42px; height:42px; flex:0 0 42px; place-items:center; border-radius:13px; background:#d9f1e8; color:var(--primary-strong); }
.shelter-review-banner > div { min-width:0; flex:1; }
.shelter-review-banner strong { color:var(--text); font:700 .88rem/1.4 "Poppins",sans-serif; }
.shelter-review-banner p { margin:3px 0 0; color:var(--text-soft); font-size:.75rem; line-height:1.5; }
.shelter-review-banner-status { flex:0 0 auto; padding:7px 10px; border:1px solid #c6e5d8; border-radius:999px; background:#fff; color:#37695b; font-size:.68rem; font-weight:800; }
.shelter-pet-details-modal { width:min(820px,100%); padding:34px; border:1px solid #dcebed; border-radius:24px; }
.shelter-pet-edit-backdrop { display:flex; align-items:flex-start; justify-content:center; overflow-y:auto; padding:20px; }
.shelter-pet-edit-backdrop .shelter-pet-details-modal { flex:0 0 auto; max-height:calc(100dvh - 40px); margin:0 auto; }
.shelter-pet-modal-title { display:flex; align-items:center; justify-content:space-between; gap:14px; margin:9px 42px 2px 0; }
.shelter-pet-modal-title h2 { margin:0; }
.shelter-pet-modal-title .shelter-status { flex:0 0 auto; }
.shelter-pet-details-modal .shelter-request-lead { margin-top:5px; }
.shelter-pet-details-modal .shelter-applicant-grid { margin:20px 0; }
.shelter-pet-details-modal .shelter-applicant-grid article { padding:15px; border-color:#dcebed; background:#f7fcfc; }
.shelter-pet-details-modal .shelter-field { gap:7px; }
.shelter-pet-details-modal .shelter-field :deep(input),.shelter-pet-details-modal .shelter-field :deep(select),.shelter-pet-details-modal .shelter-field :deep(textarea) { min-height:44px; border-color:#c5e1e4; background:#fbfefe; color:var(--text); font:500 .88rem "Montserrat",Arial,sans-serif; }
.shelter-pet-edit-form { margin-top:19px; }
.shelter-pet-edit-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
.shelter-pet-edit-wide { grid-column:1/-1; }
.shelter-pet-edit-grid textarea { resize:vertical; }
.shelter-edit-photos { display:grid;gap:9px;margin-top:5px; }
.shelter-password-form { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px 16px; }
.shelter-password-form .shelter-form-actions,.shelter-password-feedback { grid-column:1/-1; }
.shelter-password-feedback { margin:0; color:var(--primary-strong); font-size:.76rem; }
.shelter-edit-photos-heading { display:flex;align-items:center;justify-content:space-between;gap:12px;color:var(--text);font-size:.9rem;font-weight:700; }
.shelter-edit-photos-heading span { color:var(--text-soft);font-size:.78rem;font-weight:600; }
.shelter-edit-photos > .shelter-modal-note { margin:0; }
.shelter-edit-photo-grid { display:grid;grid-template-columns:repeat(auto-fill,minmax(105px,1fr));gap:10px; }
.shelter-edit-photo { position:relative;overflow:hidden;aspect-ratio:1;border:1px solid var(--border);border-radius:12px;background:#eaf5f4; }
.shelter-edit-photo img { width:100%;height:100%;object-fit:cover; }
.shelter-edit-photo button { position:absolute;right:5px;bottom:5px;display:flex;align-items:center;gap:4px;padding:5px 8px;border:0;border-radius:20px;background:rgba(24,55,61,.9);color:white;font-size:.68rem;font-weight:700;cursor:pointer; }
.shelter-edit-photo button :deep(svg) { width:13px;height:13px; }
.shelter-edit-photo-input { margin-top:3px; }
.shelter-edit-photo-input :deep(input[type="file"]) { width:100%;min-height:48px;padding:6px;border:1px solid var(--border);border-radius:11px;background:#f7fbfb;color:var(--text-soft);font:500 .8rem "Montserrat",Arial,sans-serif; }
.shelter-edit-photo-input :deep(input[type="file"]::file-selector-button) { margin-right:12px;padding:9px 14px;border:0;border-radius:8px;background:var(--primary-strong);color:#fff;font:700 .78rem "Montserrat",Arial,sans-serif;cursor:pointer;transition:background .18s ease,transform .18s ease; }
.shelter-edit-photo-input :deep(input[type="file"]::file-selector-button:hover) { background:var(--primary);transform:translateY(-1px); }
.shelter-edit-photo-input :deep(input[type="file"]:focus-visible) { outline:3px solid color-mix(in srgb,var(--primary) 28%,transparent);outline-offset:2px; }
.shelter-edit-photo-input :deep(input[type="file"]:disabled) { opacity:.55;cursor:not-allowed; }
.shelter-pet-edit-actions { justify-content:flex-end; margin-top:18px; }
.shelter-pet-details-actions { align-items:center; margin-top:18px; }
.shelter-pet-details-actions .button:nth-child(2) { margin-right:auto; }
.shelter-pet-details-actions .button-danger-outline { border-color:#d88b8b;color:#a53c3c;background:#fff; }
.shelter-pet-details-actions .button-danger-outline:hover { border-color:#a53c3c;background:#fff4f4; }
.shelter-pet-review-note { display:flex; align-items:flex-start; gap:7px; margin-top:12px; }
.shelter-pet-review-note :deep(svg) { flex:0 0 17px; width:17px; height:17px; color:var(--primary-strong); }
.shelter-profile-layout { grid-template-columns:minmax(0,1fr) 270px; }
.shelter-profile-card { padding:0!important; overflow:hidden; }
.shelter-org-identity { display:flex; align-items:center; gap:14px; padding:22px; border-bottom:1px solid var(--border); background:linear-gradient(110deg,#edf9f8, var(--white) 72%); }
.shelter-org-avatar { display:grid; width:58px; height:58px; flex:0 0 58px; place-items:center; border-radius:16px; background:var(--primary-strong); color:#fff; }
.shelter-org-avatar :deep(svg) { width:27px; height:27px; }
.shelter-org-identity > div { min-width:0; flex:1; }
.shelter-org-identity .eyebrow { margin:0 0 3px; color:var(--primary-strong); font-size:.62rem; }
.shelter-org-identity h2 { overflow-wrap:anywhere; margin:0; color:var(--text); font:750 1.22rem "Poppins",sans-serif; }
.shelter-org-identity p { margin:4px 0 0; color:var(--text-soft); font-size:.76rem; }
.shelter-profile-logout { flex:0 0 auto; }
.shelter-org-verification { display:inline-flex; flex:0 0 auto; align-items:center; gap:6px; padding:7px 10px; border:1px solid #e7d5a4; border-radius:999px; background:#fff8e5; color:#705719; font-size:.67rem; font-weight:800; }
.shelter-org-verification.is-verified { border-color:#b9ddc8; background:#eaf7ef; color:#287451; }
.shelter-org-verification :deep(svg) { width:15px; height:15px; }
.shelter-profile-group { padding:19px 22px; border-bottom:1px solid var(--border); }
.shelter-profile-group:last-child { border-bottom:0; }
.shelter-profile-group-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:13px; }
.shelter-profile-group-heading h3 { margin:0; color:var(--text); font:700 .96rem "Poppins",sans-serif; }
.shelter-profile-group-heading p { margin:3px 0 0; color:var(--text-soft); font-size:.7rem; }
.shelter-profile-edit { display:grid; width:35px; height:35px; flex:0 0 35px; place-items:center; border:1px solid var(--border); border-radius:10px; background:var(--white); color:var(--primary-strong); cursor:pointer; }
.shelter-profile-edit:hover { background:var(--background); }
.shelter-profile-edit :deep(svg) { width:16px; height:16px; }
.shelter-profile-data { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px 20px; margin:0; }
.shelter-profile-data div { min-width:0; }
.shelter-profile-data dt { margin-bottom:3px; color:var(--text-soft); font-size:.67rem; font-weight:700; }
.shelter-profile-data dd { overflow-wrap:anywhere; margin:0; color:var(--text); font-size:.8rem; }
.shelter-profile-data dd small { display:block; margin-top:3px; color:var(--text-soft); font-size:.63rem; }
.shelter-profile-data-wide { grid-column:1/-1; }
.shelter-profile-edit-grid { grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
.shelter-profile-edit-grid .shelter-form-actions { grid-column:1/-1; justify-content:flex-end; margin-top:3px; }
.shelter-profile-aside-column { display:grid; gap:14px; }
.shelter-profile-aside-column .shelter-profile-aside { position:static; }
.shelter-profile-aside-column .shelter-profile-aside > p:last-child { margin-bottom:0; }
.shelter-org-activity { display:grid; gap:0; padding:18px; }
.shelter-org-activity > .eyebrow { margin:0 0 8px; color:var(--primary-strong); font-size:.63rem; }
.shelter-org-activity > div { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:11px 0; border-top:1px solid var(--border); }
.shelter-org-activity > div strong { color:var(--primary-strong); font:750 1.35rem "Poppins",sans-serif; }
.shelter-org-activity > div span { color:var(--text-soft); font-size:.72rem; text-align:right; }
.shelter-profile-setup { display:grid; gap:15px; max-width:900px; padding:24px; }
.shelter-profile-setup > .shelter-form-grid { grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
.shelter-profile-setup-note { display:flex; align-items:flex-start; gap:8px; margin:0; color:var(--text-soft); font-size:.75rem; line-height:1.5; }
.shelter-profile-setup-note :deep(svg) { width:17px; flex:0 0 17px; color:var(--primary-strong); }
.shelter-profile-setup .shelter-form-actions { justify-content:flex-end; }
@media(max-width:1100px) { .shelter-profile-layout { grid-template-columns:1fr; } }
@media(max-width:600px) {
  .shelter-review-banner { align-items:flex-start; flex-wrap:wrap; gap:11px; padding:14px; }
  .shelter-review-banner > div { flex-basis:calc(100% - 56px); }
  .shelter-review-banner-status { margin-left:53px; }
  .shelter-pet-details-modal { max-height:calc(100dvh - 24px); padding:26px 18px 20px; border-radius:18px; }
  .shelter-pet-modal-title { align-items:flex-start; flex-direction:column; gap:7px; margin-right:38px; }
  .shelter-pet-edit-grid { grid-template-columns:1fr; gap:11px; }
  .shelter-pet-edit-wide { grid-column:auto; }
  .shelter-pet-edit-actions { display:grid; grid-template-columns:1fr 1fr; }
  .shelter-pet-edit-actions .button { width:100%; justify-content:center; }
  .shelter-pet-details-actions { display:grid; grid-template-columns:1fr 1fr; }
  .shelter-pet-details-actions .button { width:100%; justify-content:center; }
  .shelter-pet-details-actions .button:nth-child(2) { margin-right:0; }
  .shelter-pet-details-actions .button:last-child { grid-column:1/-1; }
  .shelter-org-identity { align-items:flex-start; flex-wrap:wrap; padding:18px; }
  .shelter-profile-logout { margin-left:auto; }
  .shelter-org-verification { margin-left:72px; }
  .shelter-profile-group { padding:17px 16px; }
  .shelter-profile-data,.shelter-profile-edit-grid { grid-template-columns:1fr; }
  .shelter-password-form { grid-template-columns:1fr; }
  .shelter-password-form .shelter-form-actions,.shelter-password-feedback { grid-column:auto; }
  .shelter-profile-data-wide,.shelter-profile-edit-grid .shelter-form-actions { grid-column:auto; }
  .shelter-profile-setup { padding:18px; }
  .shelter-profile-setup > .shelter-form-grid { grid-template-columns:1fr; }
}
</style>
