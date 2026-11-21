<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import SiteHeader from '../components/SiteHeader.vue';
import Icon from '../components/Icon.vue';
import { getPetById } from '../services/petApi.js';
import { api } from '../services/api.js';

const route = useRoute();
const messageText = ref('');
const searchText = ref('');
const selectedThreadId = ref('');
const mobileChatOpen = ref(false);
const sendError = ref('');
const transcriptElement = ref(null);
const currentUser = ref(null);
const allConversations = ref([]);
const inboxError = ref('');
const conversationActionError = ref('');
const openConversationMenuId = ref('');
const conversationMenuPosition = ref(null);
const openConversationMenu = computed(() => allConversations.value.find(thread => thread.id === openConversationMenuId.value) || null);
const wideLayout = ref(true);
function syncLayout() { wideLayout.value = window.matchMedia('(min-width: 761px)').matches; }
const conversations = computed(() => {
  const search = searchText.value.trim().toLocaleLowerCase('es-MX');
  return allConversations.value.filter(thread => !search || `${thread.petName} ${thread.pet?.location || ''} ${thread.pet?.shelterName || ''} ${thread.otherParticipant || ''}`.toLocaleLowerCase('es-MX').includes(search)).toSorted((a, b) => Number(Boolean(b.pinned)) - Number(Boolean(a.pinned)));
});
const activeConversation = computed(() => conversations.value.find(thread => thread.id === selectedThreadId.value) || (wideLayout.value ? conversations.value[0] : null) || null);
const groupedMessages = computed(() => {
  const groups = [];
  for (const message of activeConversation.value?.messages || []) {
    const day = dayLabel(message.createdAt || message.date);
    const last = groups.at(-1);
    if (last && last.day === day) last.messages.push(message);
    else groups.push({ day, messages: [message] });
  }
  return groups;
});
const threadPhoto = pet => pet?.photos?.[0] || pet?.image || '';
const publisherName = thread => currentUser.value?.role === 'SHELTER' ? thread?.otherParticipant : (thread?.pet?.shelterName || thread?.otherParticipant);
async function loadThreads() {
  try {
    currentUser.value = await api('/auth/me');
    const list = await api('/conversations');
    allConversations.value = await Promise.all(list.map(async thread => {
      let pet = null;
      try { pet = await getPetById(thread.petId); } catch { /* Keep conversation even if its pet is no longer listed. */ }
      return { ...thread, pet, petName: thread.petName || pet?.name || 'Mascota', messages: [] };
    }));
    const selected = allConversations.value.find(thread => thread.id === route.query.conversation)
      || allConversations.value.find(thread => String(thread.petId) === String(route.query.pet))
      || (wideLayout.value ? allConversations.value[0] : undefined);
    if (selected) await chooseConversation(selected);
  } catch (error) { inboxError.value = error.message || 'Inicia sesión para consultar tus conversaciones.'; }
}
async function chooseConversation(thread) {
  selectedThreadId.value = thread.id;
  mobileChatOpen.value = true;
  sendError.value = '';
  try {
    const page = await api('/conversations/' + thread.id + '/messages?page=0&size=100');
    thread.messages = (page.content || []).map(item => ({ ...item, text: item.content, sender: item.senderId === currentUser.value?.id ? 'adopter' : 'shelter' }));
    thread.unreadCount = 0;
  } catch (error) { sendError.value = error.message || 'No se pudieron cargar los mensajes.'; }
  scrollTranscriptToEnd();
}
function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
}
function formatFullDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
}
function dayLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Sin fecha';
  const today = new Date(); const yesterday = new Date(); yesterday.setDate(today.getDate() - 1);
  const sameDay = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  if (sameDay(date, today)) return 'Hoy';
  if (sameDay(date, yesterday)) return 'Ayer';
  return date.toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' });
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
async function sendMessage() {
  const text = messageText.value.trim();
  const thread = activeConversation.value;
  if (!text || !thread) return;
  try { await api('/conversations/' + thread.id + '/messages', { method: 'POST', body: { content: text } }); await chooseConversation(thread); }
  catch (error) { sendError.value = error.message || 'No se pudo enviar el mensaje.'; return; }
  messageText.value = '';
  sendError.value = '';
  scrollTranscriptToEnd();
}
function closeConversationMenuOnOutsideClick(event) {
  if (!event.target.closest('.adopter-conversation-actions, .adopter-conversation-menu')) closeConversationMenu();
}
function closeConversationMenu() { openConversationMenuId.value = ''; conversationMenuPosition.value = null; }
function toggleConversationMenu(thread, event) {
  if (openConversationMenuId.value === thread.id) { closeConversationMenu(); return; }
  const rect = event.currentTarget.getBoundingClientRect();
  const menuHeight = 96;
  const opensBelow = window.innerHeight - rect.bottom >= menuHeight + 12;
  const top = opensBelow ? rect.bottom + 6 : Math.max(8, rect.top - menuHeight - 6);
  const right = Math.max(8, Math.min(window.innerWidth - 216, window.innerWidth - rect.right));
  conversationMenuPosition.value = { top: `${top}px`, right: `${right}px` };
  openConversationMenuId.value = thread.id;
}
async function toggleConversationPin(thread) {
  const pinned = !thread.pinned;
  try {
    await api(`/conversations/${thread.id}/pin`, { method: 'PATCH', body: { pinned } });
    thread.pinned = pinned;
    closeConversationMenu();
  } catch (error) { conversationActionError.value = error.message || 'No se pudo actualizar la conversación.'; }
}
async function deleteConversation(thread) {
  if (!window.confirm(`¿Eliminar la conversación sobre ${thread.petName}? Solo dejará de aparecer en tu bandeja.`)) return;
  try {
    await api(`/conversations/${thread.id}`, { method: 'DELETE' });
    allConversations.value = allConversations.value.filter(item => item.id !== thread.id);
    closeConversationMenu();
    if (selectedThreadId.value === thread.id) {
      selectedThreadId.value = '';
      mobileChatOpen.value = false;
      if (wideLayout.value && conversations.value[0]) await chooseConversation(conversations.value[0]);
    }
  } catch (error) {
    conversationActionError.value = error.status === 404
      ? 'No fue posible completar la eliminación de la conversación. Inténtalo de nuevo.'
      : error.message || 'No se pudo eliminar la conversación.';
  }
}
watch(() => activeConversation.value?.id, () => { if (!activeConversation.value) mobileChatOpen.value = false; scrollTranscriptToEnd(); });
onMounted(() => { syncLayout(); window.addEventListener('resize', syncLayout); loadThreads(); document.addEventListener('click', closeConversationMenuOnOutsideClick); });
onUnmounted(() => { window.removeEventListener('resize', syncLayout); document.removeEventListener('click', closeConversationMenuOnOutsideClick); });
</script>

<template>
  <div class="adopter-messages-page">
    <SiteHeader />
    <main id="main-content" tabindex="-1" class="adopter-messages-main">
      <h1 class="sr-only">Tus Mensajes</h1>

      <section v-if="inboxError && !allConversations.length" class="messages-empty-state" role="alert">
        <span class="messages-empty-icon"><Icon name="messages-square" :size="31" /></span>
        <h2>No se pudo abrir la bandeja</h2>
        <p>{{ inboxError }}</p>
        <RouterLink class="messages-login-link" to="/acceso.html">Ingresar</RouterLink>
      </section>
      <section v-else-if="!allConversations.length" class="messages-empty-state">
        <span class="messages-empty-icon"><Icon name="messages-square" :size="31" /></span>
        <h2>Aún no tienes conversaciones</h2>
        <p>Cuando preguntes por una mascota, podrás continuar aquí el chat con su refugio o responsable.</p>
      </section>

      <section v-else class="adopter-message-layout" :class="{ 'mobile-chat-open': mobileChatOpen }" aria-label="Bandeja de mensajes del adoptante">
        <aside class="adopter-conversation-sidebar">
          <div class="adopter-inbox-heading"><div><h2>Conversaciones</h2><p>{{ conversations.length }} {{ conversations.length === 1 ? 'chat' : 'chats' }}</p></div><span class="adopter-inbox-icon"><Icon name="messages-square" /></span></div>
          <label class="adopter-search"><Icon name="search" :size="30" /><span class="sr-only">Buscar conversaciones</span><input v-model="searchText" type="search" placeholder="Buscar por mascota o refugio" /></label>
          <p v-if="conversationActionError" class="adopter-action-error" role="alert">{{ conversationActionError }}</p>
          <div class="adopter-conversation-list" @scroll="closeConversationMenu">
            <div v-for="thread in conversations" :key="thread.id" class="adopter-conversation-item" :class="{ 'is-selected': activeConversation?.id === thread.id, 'is-pinned': thread.pinned }">
              <button type="button" class="adopter-conversation-select" @click="chooseConversation(thread)">
                <span class="adopter-pet-avatar"><img v-if="threadPhoto(thread.pet)" :src="threadPhoto(thread.pet)" :alt="''" /><Icon v-else name="paw-print" /></span>
                <span class="adopter-conversation-copy"><span class="adopter-conversation-top"><strong>{{ thread.petName }}</strong><time>{{ formatConversationDate(thread) }}</time></span><span class="adopter-conversation-bottom"><span>{{ thread.lastMessage || thread.messages.at(-1)?.text || 'Inicia la conversación' }}</span><span v-if="thread.unreadCount" class="adopter-unread-badge" aria-label="Mensajes no leídos">{{ thread.unreadCount > 9 ? '9+' : thread.unreadCount }}</span><Icon name="chevron-right" :size="16" /></span></span>
              </button>
              <div class="adopter-conversation-actions"><button type="button" class="adopter-conversation-menu-trigger" :aria-label="`Opciones de conversación sobre ${thread.petName}`" aria-haspopup="true" :aria-expanded="openConversationMenuId===thread.id" @click.stop="toggleConversationMenu(thread,$event)" @keydown.esc.stop="closeConversationMenu"><span aria-hidden="true">•••</span></button></div>
            </div>
            <p v-if="!conversations.length" class="adopter-search-empty">No hay conversaciones que coincidan.</p>
          </div>
          <Teleport to="body"><div v-if="openConversationMenu" class="adopter-conversation-menu" role="group" aria-label="Acciones de conversación" :style="conversationMenuPosition"><button type="button" @keydown.esc="closeConversationMenu" @click="toggleConversationPin(openConversationMenu)">{{ openConversationMenu.pinned ? 'Quitar fijado' : 'Fijar conversación' }}</button><button type="button" class="is-danger" @keydown.esc="closeConversationMenu" @click="deleteConversation(openConversationMenu)">Eliminar conversación</button></div></Teleport>
        </aside>

        <section v-if="activeConversation" class="adopter-chat-window" aria-label="Conversación activa">
          <header class="adopter-chat-header">
            <button type="button" class="adopter-chat-back" aria-label="Volver a conversaciones" @click="mobileChatOpen=false"><Icon name="arrow-left" /></button>
            <span class="adopter-pet-avatar adopter-chat-avatar"><img v-if="threadPhoto(activeConversation.pet)" :src="threadPhoto(activeConversation.pet)" :alt="''" /><Icon v-else name="paw-print" /></span>
            <div class="adopter-chat-contact"><strong>{{ publisherName(activeConversation) || activeConversation.otherParticipant || 'Contacto de adopción' }}</strong><span>Sobre: {{ activeConversation.petName }}</span><small v-if="activeConversation.pet?.location" class="adopter-chat-location">Ubicación: {{ activeConversation.pet.location }}</small></div>
            <RouterLink v-if="activeConversation.pet" class="adopter-profile-link" :to="{ path: '/mascota.html', query: { pet: activeConversation.petId } }">Ver perfil</RouterLink>
          </header>
          <div ref="transcriptElement" class="adopter-chat-transcript" aria-live="polite" aria-label="Mensajes">
            <div class="adopter-chat-date">Conversación de adopción · PetHop</div>
            <template v-for="group in groupedMessages" :key="group.day">
              <div class="adopter-chat-date">{{ group.day }}</div>
              <article v-for="(message,index) in group.messages" :key="`${message.createdAt || message.date || index}-${index}`" class="adopter-chat-bubble" :class="message.sender === 'adopter' || message.from === 'adopter' ? 'is-outgoing' : 'is-incoming'">
                <p>{{ message.text }}</p><time :title="formatFullDate(message.createdAt || message.date)">{{ formatTime(message.createdAt || message.date) }}</time>
              </article>
            </template>
            <p v-if="!activeConversation.messages.length" class="adopter-chat-first-message">Escribe para iniciar tu conversación sobre {{ activeConversation.petName }}.</p>
          </div>
          <form class="adopter-chat-composer" @submit.prevent="sendMessage">
            <label class="sr-only" for="adopter-message">Escribe un mensaje</label>
            <textarea id="adopter-message" v-model="messageText" rows="1" maxlength="1000" placeholder="Escribe un mensaje" :disabled="!activeConversation" @keydown.enter.exact.prevent="sendMessage" />
            <button type="submit" aria-label="Enviar mensaje" :disabled="!messageText.trim()"><Icon name="send" :size="24" /></button>
            <p v-if="sendError" class="adopter-chat-error" role="alert">{{ sendError }}</p>
          </form>
        </section>
        <section v-else class="adopter-no-conversation"><Icon name="search" :size="28" /><p>No encontramos conversaciones con ese nombre.</p></section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.adopter-messages-page { display:flex; flex-direction:column; height:100vh; height:100dvh; overflow:hidden; background:var(--background); }
.adopter-messages-main { display:flex; flex-direction:column; width:100%; flex:1; min-height:0; margin:0; padding:0; }
.adopter-message-layout { display:grid; grid-template-columns:minmax(300px,478px) minmax(0,1fr); flex:1; min-height:0; width:100%; overflow:hidden; background:var(--white); }
.adopter-conversation-sidebar { display:flex; min-width:0; flex-direction:column; border-right:1px solid var(--border); background:var(--white); }
.adopter-inbox-heading { display:flex; align-items:center; justify-content:space-between; padding:20px 19px 15px; }
.adopter-inbox-heading h2 { color:var(--text); font:700 1.08rem/1.3 "Poppins",sans-serif; }
.adopter-inbox-heading p { margin-top:2px; color:var(--text-soft); font-size:.75rem; }
.adopter-inbox-icon { display:grid; width:37px; height:37px; place-items:center; border-radius: var(--radius-sm); border:1px solid var(--primary); background:var(--primary-soft); color:var(--primary-strong); }
.adopter-search { display:flex; height:53px; align-items:center; gap:9px; margin:0 14px 12px; padding:0 11px; border:1px solid var(--primary); border-radius:21px; background:var(--background); color:var(--text-soft); }
.adopter-search input { width:100%; min-width:0; border:0;  background:transparent; color:var(--text); font:500 1rem "Montserrat",sans-serif; }
.adopter-search input::placeholder { color:var(--text-soft); }
.adopter-conversation-list { overflow-y:auto; }
.adopter-action-error { margin:0; padding:8px 15px; color:var(--danger); font-size:.75rem; }
.adopter-conversation-item { position:relative; display:flex; width:100%; align-items:center; gap:5px; min-height:86px; padding:13px 8px 13px 15px; border-bottom:1px solid var(--tint); background:var(--white); color:var(--text); transition:background .18s ease; }
.adopter-conversation-item:hover { background:var(--background); }
.adopter-conversation-item.is-selected { background:var(--tint); }
.adopter-conversation-item.is-pinned .adopter-conversation-top strong::before { content:'Fijada · '; color:var(--primary-strong); font-size:.75em; }
.adopter-conversation-select { display:flex; min-width:0; flex:1; align-items:center; gap:11px; padding:8px 0; border:0; background:transparent; color:inherit; cursor:pointer; text-align:left; }
.adopter-conversation-select:focus-visible,.adopter-conversation-menu-trigger:focus-visible,.adopter-conversation-menu button:focus-visible { outline:2px solid var(--primary-strong); outline-offset:2px; }
.adopter-conversation-actions { position:relative; flex:0 0 auto; }
.adopter-conversation-menu-trigger { display:grid; width:34px; height:34px; place-items:center; border:1px solid transparent; border-radius: var(--radius-xs); background:transparent; color:var(--text-soft); cursor:pointer; }
.adopter-conversation-menu-trigger:hover,.adopter-conversation-menu-trigger[aria-expanded="true"] { border-color:var(--border); background:var(--white); color:var(--primary-strong); }
.adopter-conversation-menu-trigger span { font-size:1.12rem; font-weight:900; letter-spacing:1px; line-height:1; }
.adopter-conversation-menu { position:fixed; z-index:10000; display:grid; width:min(208px,calc(100vw - 16px)); padding:5px; border:1px solid var(--border); border-radius: var(--radius-sm); background:var(--white); box-shadow:var(--shadow-md); }
.adopter-conversation-menu button { padding:10px 11px; border:0; border-radius: var(--radius-xs); background:transparent; color:var(--text); cursor:pointer; font:600 .76rem "Montserrat",sans-serif; text-align:left; }
.adopter-conversation-menu button:hover { background:var(--background); }
.adopter-conversation-menu button.is-danger { color:var(--danger); }
.adopter-pet-avatar { display:grid; width:60px; height:60px; flex:0 0 60px; overflow:hidden; place-items:center; border:1px solid var(--primary); border-radius:50%; background:var(--primary-soft); color:var(--primary-strong); }
.adopter-pet-avatar img { width:100%; height:100%; object-fit:cover; }
.adopter-conversation-copy { display:flex; min-width:0; flex:1; flex-direction:column; gap:5px; }
.adopter-conversation-top,.adopter-conversation-bottom { display:flex; min-width:0; align-items:center; justify-content:space-between; gap:9px; }
.adopter-unread-badge { flex:0 0 auto; min-width:20px; height:20px; padding:0 6px; display:inline-grid; place-items:center; border-radius:999px; background:var(--primary-strong); color:var(--white); font-size:.75rem; font-weight:800; }
.adopter-conversation-top strong { overflow:hidden; font-size:1.25rem; font-weight:600; text-overflow:ellipsis; white-space:nowrap; }
.adopter-conversation-top time { flex:0 0 auto; color:var(--text-soft); font-size:.94rem; }
.adopter-conversation-bottom > span { overflow:hidden; color:var(--text-soft); font-size:.94rem; text-overflow:ellipsis; white-space:nowrap; }
.adopter-conversation-bottom :deep(svg) { flex:0 0 auto; color:#819091; }
.adopter-search-empty { padding:20px 15px; color:var(--text-soft); font-size:.78rem; text-align:center; }
.adopter-chat-window { display:flex; min-width:0; flex-direction:column; background:var(--tint); }
.adopter-chat-header { display:flex; min-height:107px; align-items:center; gap:12px; padding:12px 22px; border-bottom:1px solid var(--border); background:var(--white); }
.adopter-chat-avatar { width:60px; height:60px; flex-basis:60px; }
.adopter-chat-contact { display:flex; min-width:0; flex:1; flex-direction:column; gap:3px; }
.adopter-chat-contact strong { overflow:hidden; color:var(--text); font-size:1.25rem; font-weight:600; text-overflow:ellipsis; white-space:nowrap; }
.adopter-chat-contact span { overflow:hidden; color:var(--text-soft); font:300 .94rem/1.4 "Poppins",sans-serif; text-overflow:ellipsis; white-space:nowrap; }
.adopter-chat-contact .adopter-chat-location { overflow:hidden; color:var(--primary-strong); font:400 .94rem/1.4 "Poppins",sans-serif; text-overflow:ellipsis; white-space:nowrap; }
.adopter-profile-link { display:inline-flex; min-height:58px; align-items:center; padding:0 12px; border:1px solid var(--primary); border-radius:21px; color:var(--primary-strong); font-size:1.06rem; font-weight:700; text-decoration:none; }
.adopter-profile-link:hover { background:var(--primary-soft); }
.adopter-chat-back { display:none; }
.adopter-chat-transcript { display:flex; min-height:0; flex:1; flex-direction:column; gap:10px; overflow-y:auto; padding:20px clamp(14px,4vw,34px); scroll-behavior:smooth; }
.adopter-chat-date { align-self:center; margin:0 0 7px; padding:6px 10px; border:1px solid var(--primary); border-radius:21px; background:var(--white); color:var(--text-soft); font:500 .75rem "Poppins",sans-serif; }
.adopter-chat-bubble { display:flex; max-width:min(76%,520px); flex-direction:column; gap:4px; padding:10px 12px 7px; border:1px solid var(--primary-light); background:var(--white); box-shadow:0 1px 1px rgba(35,70,70,.05); }
.adopter-chat-bubble.is-incoming { align-self:flex-start; border-radius:13px 13px 13px 4px; background:var(--white); }
.adopter-chat-bubble.is-outgoing { align-self:flex-end; border-color:var(--primary-strong); border-radius:13px 13px 4px 13px; background:var(--primary-strong); }
.adopter-chat-bubble p { color:var(--text); font:400 1.25rem/1.5 "Poppins",sans-serif; overflow-wrap:anywhere; white-space:pre-wrap; }
.adopter-chat-bubble.is-outgoing p { color:var(--white); }
.adopter-chat-bubble time { align-self:flex-end; color:var(--text-soft); font-size:.94rem; }
.adopter-chat-bubble.is-outgoing time { color:rgba(255,255,255,.78); }
.adopter-chat-first-message { align-self:center; margin:auto; padding:10px 13px; border-radius: var(--radius-md); background:rgba(255,255,255,.8); color:var(--text-soft); font-size:.76rem; text-align:center; }
.adopter-chat-composer { display:flex; align-items:flex-end; gap:11px; padding:42px 22px 47px; background:var(--background); }
.adopter-chat-composer textarea { min-width:0; min-height:65px; max-height:110px; flex:1; resize:vertical; padding:11px 14px; border:1px solid var(--primary); border-radius:21px;  background:var(--white); color:var(--text); font:500 1rem/1.4 "Poppins",sans-serif; }
.adopter-chat-composer textarea::placeholder { color:var(--text-soft); }
.adopter-chat-composer textarea:focus { border-color:var(--primary-strong); box-shadow:0 0 0 3px rgba(22,127,135,.1); }
.adopter-chat-composer button { display:grid; width:58px; height:58px; flex:0 0 58px; place-items:center; border:0; border-radius:50%; background:var(--primary-strong); color:var(--white); cursor:pointer; transition:background .18s ease,transform .18s ease; }
.adopter-chat-composer button:hover:not(:disabled) { transform:translateY(-1px); background:var(--primary-dark); }
.adopter-chat-composer button:disabled { cursor:not-allowed; opacity:.5; }
.adopter-chat-error { position:absolute; margin-top:-28px; color:var(--danger); font-size:.75rem; }
.messages-empty-state { display:grid; min-height:400px; align-content:center; justify-items:center; width:min(720px,calc(100% - 48px)); margin:24px auto; padding:35px; border:1px solid var(--border); border-radius: var(--radius-md); background:var(--white); box-shadow:var(--shadow-sm); text-align:center; }
.adopter-no-conversation { display:grid; align-content:center; justify-items:center; gap:10px; color:var(--text-soft); text-align:center; }
.adopter-no-conversation :deep(svg) { color:var(--primary-strong); }
.messages-empty-icon { display:grid; width:68px; height:68px; place-items:center; border-radius: var(--radius-md); border:1px solid var(--primary); background:var(--primary-soft); color:var(--primary-strong); }
.messages-empty-state h2 { margin-top:18px; color:var(--text); font:700 1.25rem "Poppins",sans-serif; }
.messages-empty-state p { max-width:450px; margin:7px 0 19px; color:var(--text-soft); font-size:.85rem; }
.messages-login-link { display:inline-flex; min-height:42px; align-items:center; padding:0 16px; border-radius: var(--radius-sm); background:var(--primary-strong); color:var(--white); font-size:.78rem; font-weight:800; text-decoration:none; }
@media(max-width:760px) {
  .adopter-messages-main { padding:0; }
  .adopter-no-conversation { display:none; }
  .adopter-message-layout { position:relative; display:block; min-height:430px; }
  .adopter-conversation-sidebar,.adopter-chat-window { position:absolute; inset:0; }
  .adopter-chat-window { display:none; }
  .adopter-message-layout.mobile-chat-open .adopter-conversation-sidebar { display:none; }
  .adopter-message-layout.mobile-chat-open .adopter-chat-window { display:flex; }
  .adopter-chat-back { display:grid; width:36px; height:36px; flex:0 0 36px; place-items:center; border:0; border-radius:50%; background:transparent; color:var(--primary-strong); }
  .adopter-chat-header { min-height:76px; gap:8px; padding:10px; }
  .adopter-chat-avatar { width:46px; height:46px; flex-basis:46px; }
  .adopter-pet-avatar { width:48px; height:48px; flex-basis:48px; }
  .adopter-profile-link { min-height:34px; padding:0 9px; font-size:.82rem; }
  .adopter-chat-transcript { padding:16px 12px; }
  .adopter-chat-composer { padding:12px; }
  .adopter-chat-composer textarea { min-height:43px; }
  .adopter-chat-composer button { width:43px; height:43px; flex-basis:43px; }
}
@media(max-width:420px) {
  .adopter-pet-avatar { width:44px; height:44px; flex-basis:44px; }
  .adopter-chat-avatar { width:40px; height:40px; flex-basis:40px; }
  .adopter-chat-contact strong { font-size:.8rem; }
  .adopter-profile-link { font-size:.75rem; }
  .adopter-chat-bubble { max-width:88%; }
}
</style>
