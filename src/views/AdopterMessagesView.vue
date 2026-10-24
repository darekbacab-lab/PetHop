<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import '../../js/pethop-data.js';
import SiteHeader from '../components/SiteHeader.vue';
import SiteFooter from '../components/SiteFooter.vue';
import Icon from '../components/Icon.vue';
import { getPet } from '../services/pets.js';
import { storageService } from '../services/storage.js';

const revision = ref(0);
const route = useRoute();
const messageText = ref('');
const searchText = ref('');
const selectedThreadId = ref('');
const mobileChatOpen = ref(false);
const sendError = ref('');
const transcriptElement = ref(null);
const currentUser = computed(() => storageService.read('pethop_user', {}) || {});
const ownerId = computed(() => String(currentUser.value.email || 'visitante').trim().toLowerCase());
const allConversations = computed(() => {
  revision.value;
  return globalThis.PetHopData.messages(storageService)
    .filter(thread => String(thread.owner || 'visitante').trim().toLowerCase() === ownerId.value)
    .map(thread => ({ ...thread, pet: getPet(thread.petId), petName: thread.petName || getPet(thread.petId)?.name || 'Mascota' }))
    .sort((a, b) => new Date(b.updatedAt || b.messages.at(-1)?.createdAt || 0) - new Date(a.updatedAt || a.messages.at(-1)?.createdAt || 0));
});
const conversations = computed(() => {
  const search = searchText.value.trim().toLocaleLowerCase('es-MX');
  return allConversations.value.filter(thread => !search || `${thread.petName} ${thread.pet?.location || ''}`.toLocaleLowerCase('es-MX').includes(search));
});
const activeConversation = computed(() => conversations.value.find(thread => thread.id === selectedThreadId.value) || conversations.value[0] || null);
const threadPhoto = pet => {
  const source = pet?.photos?.[0] || pet?.image;
  if (!source) return '';
  return source.startsWith('data:') ? source : `/img/${source}`;
};
function chooseConversation(thread) {
  selectedThreadId.value = thread.id;
  mobileChatOpen.value = true;
  sendError.value = '';
  scrollTranscriptToEnd();
}
function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
}
function formatConversationDate(thread) {
  const last = thread.messages.at(-1);
  return formatTime(thread.updatedAt || last?.createdAt || last?.date);
}
function scrollTranscriptToEnd() {
  nextTick(() => {
    if (transcriptElement.value) transcriptElement.value.scrollTo({ top: transcriptElement.value.scrollHeight, behavior: 'smooth' });
  });
}
function sendMessage() {
  const text = messageText.value.trim();
  const thread = activeConversation.value;
  if (!text || !thread) return;
  const createdAt = new Date().toISOString();
  if (!globalThis.PetHopData.appendMessage(storageService, thread.id, { sender: 'adopter', text, createdAt })) {
    sendError.value = 'No se pudo guardar el mensaje en este dispositivo.';
    return;
  }
  messageText.value = '';
  sendError.value = '';
  revision.value += 1;
  scrollTranscriptToEnd();
}
function refreshMessages(event) {
  if (!event || event.key === globalThis.PetHopData.MESSAGES_KEY) revision.value += 1;
}
watch(() => activeConversation.value?.id, () => scrollTranscriptToEnd());
watch([() => route.query.pet, allConversations], ([petId, threads]) => {
  if (!petId) return;
  const thread = threads.find(item => String(item.petId) === String(petId));
  if (thread) {
    selectedThreadId.value = thread.id;
    mobileChatOpen.value = true;
  }
}, { immediate: true });
onMounted(() => window.addEventListener('storage', refreshMessages));
onUnmounted(() => window.removeEventListener('storage', refreshMessages));
</script>

<template>
  <div class="adopter-messages-page">
    <SiteHeader />
    <main class="adopter-messages-main">
      <div class="adopter-messages-heading">
        <div><span class="eyebrow">TUS CONVERSACIONES</span><h1>Mensajes</h1><p>Continúa las conversaciones sobre las mascotas que te interesan.</p></div>
      </div>

      <section v-if="!allConversations.length" class="messages-empty-state">
        <span class="messages-empty-icon"><Icon name="messages-square" :size="31" /></span>
        <h2>Aún no tienes conversaciones</h2>
        <p>Cuando preguntes por una mascota, podrás continuar aquí el chat con su refugio o responsable.</p>
      </section>

      <section v-else class="adopter-message-layout" :class="{ 'mobile-chat-open': mobileChatOpen }" aria-label="Bandeja de mensajes del adoptante">
        <aside class="adopter-conversation-sidebar">
          <div class="adopter-inbox-heading"><div><h2>Conversaciones</h2><p>{{ conversations.length }} {{ conversations.length === 1 ? 'chat' : 'chats' }}</p></div><span class="adopter-inbox-icon"><Icon name="messages-square" /></span></div>
          <label class="adopter-search"><Icon name="search" :size="18" /><span class="sr-only">Buscar conversaciones</span><input v-model="searchText" type="search" placeholder="Buscar por mascota" /></label>
          <div class="adopter-conversation-list">
            <button v-for="thread in conversations" :key="thread.id" type="button" class="adopter-conversation-item" :class="{ 'is-selected': activeConversation?.id === thread.id }" @click="chooseConversation(thread)">
              <span class="adopter-pet-avatar"><img v-if="threadPhoto(thread.pet)" :src="threadPhoto(thread.pet)" :alt="''" /><Icon v-else name="paw-print" /></span>
              <span class="adopter-conversation-copy"><span class="adopter-conversation-top"><strong>{{ thread.petName }}</strong><time>{{ formatConversationDate(thread) }}</time></span><span class="adopter-conversation-bottom"><span>{{ thread.messages.at(-1)?.text || 'Inicia la conversación' }}</span><Icon name="chevron-right" :size="16" /></span></span>
            </button>
            <p v-if="!conversations.length" class="adopter-search-empty">No hay conversaciones que coincidan.</p>
          </div>
        </aside>

        <section v-if="activeConversation" class="adopter-chat-window" aria-label="Conversación activa">
          <header class="adopter-chat-header">
            <button type="button" class="adopter-chat-back" aria-label="Volver a conversaciones" @click="mobileChatOpen=false"><Icon name="arrow-left" /></button>
            <span class="adopter-pet-avatar adopter-chat-avatar"><img v-if="threadPhoto(activeConversation.pet)" :src="threadPhoto(activeConversation.pet)" :alt="''" /><Icon v-else name="paw-print" /></span>
            <div class="adopter-chat-contact"><strong>{{ activeConversation.petName }}</strong><span>{{ activeConversation.pet?.species || 'Mascota' }}<template v-if="activeConversation.pet?.location"> · {{ activeConversation.pet.location }}</template></span></div>
            <RouterLink v-if="activeConversation.pet" class="adopter-profile-link" :to="{ path: '/mascota.html', query: { pet: activeConversation.petId } }">Ver perfil</RouterLink>
          </header>
          <div ref="transcriptElement" class="adopter-chat-transcript" aria-live="polite" aria-label="Mensajes">
            <div class="adopter-chat-date">Conversación de adopción · PetHop</div>
            <article v-for="(message,index) in activeConversation.messages" :key="`${message.createdAt || message.date || index}-${index}`" class="adopter-chat-bubble" :class="message.sender === 'adopter' || message.from === 'adopter' ? 'is-outgoing' : 'is-incoming'">
              <p>{{ message.text }}</p><time>{{ formatTime(message.createdAt || message.date) }}</time>
            </article>
            <p v-if="!activeConversation.messages.length" class="adopter-chat-first-message">Escribe para iniciar tu conversación sobre {{ activeConversation.petName }}.</p>
          </div>
          <form class="adopter-chat-composer" @submit.prevent="sendMessage">
            <label class="sr-only" for="adopter-message">Escribe un mensaje</label>
            <textarea id="adopter-message" v-model="messageText" rows="1" maxlength="1000" placeholder="Escribe un mensaje" :disabled="!activeConversation" @keydown.enter.exact.prevent="sendMessage" />
            <button type="submit" aria-label="Enviar mensaje" :disabled="!messageText.trim()"><Icon name="send" :size="19" /></button>
            <p v-if="sendError" class="adopter-chat-error" role="alert">{{ sendError }}</p>
          </form>
        </section>
        <section v-else class="adopter-no-conversation"><Icon name="search" :size="28" /><p>No encontramos conversaciones con ese nombre.</p></section>
      </section>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.adopter-messages-page { min-height:100vh; background:var(--background); }
.adopter-messages-main { width:min(var(--container-width),calc(100% - 48px)); min-height:calc(100vh - var(--header-height)); margin:0 auto; padding:48px 0 56px; }
.adopter-messages-heading { display:flex; align-items:flex-end; justify-content:space-between; gap:22px; margin-bottom:24px; }
.adopter-messages-heading .eyebrow { color:var(--primary-strong); font-size:.72rem; font-weight:800; letter-spacing:.1em; }
.adopter-messages-heading h1 { margin:4px 0 2px; color:var(--hero-title-dark); font:800 clamp(2rem,4vw,2.8rem)/1.1 "Poppins",sans-serif; letter-spacing:-.035em; }
.adopter-messages-heading p { color:var(--text-soft); font-size:.91rem; }
.adopter-message-layout { display:grid; grid-template-columns:minmax(270px,.72fr) minmax(0,1.5fr); height:min(700px,calc(100vh - 250px)); min-height:500px; overflow:hidden; border:1px solid var(--border); border-radius:20px; background:#fff; box-shadow:var(--shadow-md); }
.adopter-conversation-sidebar { display:flex; min-width:0; flex-direction:column; border-right:1px solid #dcebed; background:#fff; }
.adopter-inbox-heading { display:flex; align-items:center; justify-content:space-between; padding:20px 19px 15px; }
.adopter-inbox-heading h2 { color:var(--text); font:700 1.08rem/1.3 "Poppins",sans-serif; }
.adopter-inbox-heading p { margin-top:2px; color:var(--text-soft); font-size:.74rem; }
.adopter-inbox-icon { display:grid; width:37px; height:37px; place-items:center; border-radius:12px; background:#e7f6f5; color:var(--primary-strong); }
.adopter-search { display:flex; height:40px; align-items:center; gap:9px; margin:0 14px 12px; padding:0 11px; border:1px solid #dcebed; border-radius:11px; background:#f8fcfc; color:var(--text-soft); }
.adopter-search input { width:100%; min-width:0; border:0; outline:0; background:transparent; color:var(--text); font:500 .78rem "Montserrat",sans-serif; }
.adopter-search input::placeholder { color:#7e8b8c; }
.adopter-conversation-list { overflow-y:auto; }
.adopter-conversation-item { display:flex; width:100%; align-items:center; gap:11px; padding:13px 15px; border:0; border-bottom:1px solid #edf3f3; background:#fff; color:var(--text); cursor:pointer; text-align:left; transition:background .18s ease; }
.adopter-conversation-item:hover { background:#f6fbfb; }
.adopter-conversation-item.is-selected { background:#eaf7f6; }
.adopter-pet-avatar { display:grid; width:48px; height:48px; flex:0 0 48px; overflow:hidden; place-items:center; border:1px solid #d9ebec; border-radius:50%; background:#e7f6f5; color:var(--primary-strong); }
.adopter-pet-avatar img { width:100%; height:100%; object-fit:cover; }
.adopter-conversation-copy { display:flex; min-width:0; flex:1; flex-direction:column; gap:5px; }
.adopter-conversation-top,.adopter-conversation-bottom { display:flex; min-width:0; align-items:center; justify-content:space-between; gap:9px; }
.adopter-conversation-top strong { overflow:hidden; font-size:.82rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-conversation-top time { flex:0 0 auto; color:var(--text-soft); font-size:.64rem; }
.adopter-conversation-bottom > span { overflow:hidden; color:var(--text-soft); font-size:.72rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-conversation-bottom :deep(svg) { flex:0 0 auto; color:#819091; }
.adopter-search-empty { padding:20px 15px; color:var(--text-soft); font-size:.78rem; text-align:center; }
.adopter-chat-window { display:flex; min-width:0; flex-direction:column; background-color:#eaf4f2; background-image:radial-gradient(rgba(22,127,135,.06) .7px,transparent .7px); background-size:18px 18px; }
.adopter-chat-header { display:flex; min-height:76px; align-items:center; gap:12px; padding:12px 18px; border-bottom:1px solid #dcebed; background:#fff; }
.adopter-chat-avatar { width:46px; height:46px; flex-basis:46px; }
.adopter-chat-contact { display:flex; min-width:0; flex:1; flex-direction:column; gap:3px; }
.adopter-chat-contact strong { overflow:hidden; color:var(--text); font-size:.89rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-chat-contact span { overflow:hidden; color:var(--text-soft); font-size:.72rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-profile-link { display:inline-flex; min-height:36px; align-items:center; padding:0 12px; border:1px solid #cfe4e5; border-radius:9px; color:var(--primary-strong); font-size:.72rem; font-weight:800; text-decoration:none; }
.adopter-profile-link:hover { background:#eaf7f6; }
.adopter-chat-back { display:none; }
.adopter-chat-transcript { display:flex; min-height:0; flex:1; flex-direction:column; gap:10px; overflow-y:auto; padding:20px clamp(14px,4vw,34px); scroll-behavior:smooth; }
.adopter-chat-date { align-self:center; margin:0 0 7px; padding:6px 10px; border:1px solid rgba(180,211,210,.65); border-radius:20px; background:rgba(255,255,255,.85); color:#627373; font-size:.66rem; }
.adopter-chat-bubble { display:flex; max-width:min(76%,520px); flex-direction:column; gap:4px; padding:10px 12px 7px; border:1px solid rgba(180,211,210,.45); background:#fff; box-shadow:0 1px 1px rgba(35,70,70,.05); }
.adopter-chat-bubble.is-incoming { align-self:flex-start; border-radius:13px 13px 13px 4px; background:#fff; }
.adopter-chat-bubble.is-outgoing { align-self:flex-end; border-color:var(--primary-strong); border-radius:13px 13px 4px 13px; background:var(--primary-strong); }
.adopter-chat-bubble p { color:var(--text); font-size:.82rem; line-height:1.5; overflow-wrap:anywhere; white-space:pre-wrap; }
.adopter-chat-bubble.is-outgoing p { color:#fff; }
.adopter-chat-bubble time { align-self:flex-end; color:var(--text-soft); font-size:.62rem; }
.adopter-chat-bubble.is-outgoing time { color:rgba(255,255,255,.78); }
.adopter-chat-first-message { align-self:center; margin:auto; padding:10px 13px; border-radius:20px; background:rgba(255,255,255,.8); color:var(--text-soft); font-size:.76rem; text-align:center; }
.adopter-chat-composer { display:flex; align-items:flex-end; gap:10px; padding:12px 15px; background:#f7fbfa; }
.adopter-chat-composer textarea { min-height:43px; max-height:110px; flex:1; resize:vertical; padding:11px 14px; border:1px solid #d7e8e7; border-radius:16px; outline:0; background:#fff; color:var(--text); font:500 .82rem/1.4 "Montserrat",sans-serif; }
.adopter-chat-composer textarea:focus { border-color:var(--primary-strong); box-shadow:0 0 0 3px rgba(22,127,135,.1); }
.adopter-chat-composer button { display:grid; width:43px; height:43px; flex:0 0 43px; place-items:center; border:0; border-radius:50%; background:var(--primary-strong); color:#fff; cursor:pointer; transition:background .18s ease,transform .18s ease; }
.adopter-chat-composer button:hover:not(:disabled) { transform:translateY(-1px); background:var(--primary-dark); }
.adopter-chat-composer button:disabled { cursor:not-allowed; opacity:.5; }
.adopter-chat-error { position:absolute; margin-top:-28px; color:var(--danger); font-size:.72rem; }
.messages-empty-state { display:grid; min-height:400px; align-content:center; justify-items:center; padding:35px; border:1px solid var(--border); border-radius:20px; background:#fff; box-shadow:var(--shadow-sm); text-align:center; }
.adopter-no-conversation { display:grid; align-content:center; justify-items:center; gap:10px; color:var(--text-soft); text-align:center; }
.adopter-no-conversation :deep(svg) { color:var(--primary-strong); }
.messages-empty-icon { display:grid; width:68px; height:68px; place-items:center; border-radius:22px; background:#e7f6f5; color:var(--primary-strong); }
.messages-empty-state h2 { margin-top:18px; color:var(--text); font:700 1.25rem "Poppins",sans-serif; }
.messages-empty-state p { max-width:450px; margin:7px 0 19px; color:var(--text-soft); font-size:.85rem; }
@media(max-width:760px) {
  .adopter-messages-main { width:calc(100% - 30px); padding:30px 0 40px; }
  .adopter-messages-heading { align-items:flex-start; flex-direction:column; gap:14px; }
  .adopter-message-layout { position:relative; display:block; height:calc(100dvh - 235px); min-height:430px; }
  .adopter-conversation-sidebar,.adopter-chat-window { position:absolute; inset:0; }
  .adopter-chat-window { display:none; }
  .adopter-message-layout.mobile-chat-open .adopter-conversation-sidebar { display:none; }
  .adopter-message-layout.mobile-chat-open .adopter-chat-window { display:flex; }
  .adopter-chat-back { display:grid; width:36px; height:36px; flex:0 0 36px; place-items:center; border:0; border-radius:50%; background:transparent; color:var(--primary-strong); }
  .adopter-chat-header { gap:8px; padding:10px; }
  .adopter-profile-link { min-height:34px; padding:0 9px; }
  .adopter-chat-transcript { padding:16px 12px; }
}
@media(max-width:420px) {
  .adopter-messages-heading p { font-size:.82rem; }
  .adopter-pet-avatar { width:44px; height:44px; flex-basis:44px; }
  .adopter-chat-avatar { width:40px; height:40px; flex-basis:40px; }
  .adopter-chat-contact strong { font-size:.8rem; }
  .adopter-profile-link { font-size:.65rem; }
  .adopter-chat-bubble { max-width:88%; }
}
</style>
