<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import Icon from './Icon.vue';
import { api } from '../services/api.js';
import { isVeterinaryQuestion } from '../services/assistantIntent.js';

const route = useRoute();
const isOpen = ref(false);
const draft = ref('');
const messages = ref([]);
const isSending = ref(false);
const error = ref('');
const lastVeterinaryQuestion = ref('');
const composer = ref(null);
const panel = ref(null);
const transcript = ref(null);
const launcher = ref(null);
const quickQuestions = [
  '¿Cómo puedo adoptar una mascota?',
  '¿Qué debo considerar antes de adoptar?',
  '¿Cómo puedo conocer el estado de salud de una mascota?'
];
const currentPetId = computed(() => route.path === '/mascota.html' && route.query.pet ? String(route.query.pet) : null);
const isAuthRoute = computed(() => route.path === '/acceso.html');
const generalAnswers = [
  { match: /adoptar|adopción|adopcion/i, text: 'Para adoptar, explora las mascotas disponibles, revisa su información y envía una solicitud desde el perfil de la mascota. El refugio revisará tus datos y podrá contactarte para continuar el proceso.' },
  { match: /antes de adoptar|considerar/i, text: 'Antes de adoptar, considera el tiempo, espacio, presupuesto y cuidados que podrás ofrecer. También revisa la edad, tamaño, personalidad y requisitos de la mascota para encontrar una buena compatibilidad.' },
  { match: /hola|ayuda|puedes/i, text: 'Puedo orientarte sobre adopción, preparación del hogar, bienestar y cuidados responsables. Escribe tu pregunta y te ayudaré.' }
];
let previousOverflow = '';

function openChat() {
  isOpen.value = true;
}

function closeChat() {
  isOpen.value = false;
}

function addMessage(value) {
  const text = value.trim();
  if (!text) return;
  messages.value.push({ id: `${Date.now()}-${messages.value.length}`, text });
  draft.value = '';
  scrollToLatest();
}

function generalAnswer(text) {
  return generalAnswers.find(answer => answer.match.test(text))?.text
    || 'Puedo ayudarte con el proceso de adopción, el cuidado responsable y dudas veterinarias generales. Cuéntame qué necesitas saber.';
}

async function submitMessage(value, { recordUser = true } = {}) {
  const text = value.trim();
  if (!text || isSending.value) return;
  if (!isVeterinaryQuestion(text)) {
    messages.value.push({ id: `${Date.now()}-${messages.value.length}`, role: 'user', text });
    messages.value.push({ id: `${Date.now()}-${messages.value.length + 1}`, role: 'assistant', text: generalAnswer(text) });
    draft.value = '';
    error.value = '';
    scrollToLatest();
    return;
  }
  if (recordUser) messages.value.push({ id: `${Date.now()}-${messages.value.length}`, role: 'user', text });
  if (recordUser) draft.value = '';
  error.value = '';
  lastVeterinaryQuestion.value = text;
  isSending.value = true;
  scrollToLatest();
  try {
    const response = await api('/veterinary-assistant/messages', {
      method: 'POST',
      body: { message: text, petId: currentPetId.value }
    });
    messages.value.push({ id: `${Date.now()}-${messages.value.length}`, role: 'assistant', response });
  } catch (requestError) {
    error.value = requestError.message || 'No se pudo obtener orientación veterinaria.';
  } finally {
    isSending.value = false;
    scrollToLatest();
  }
}

function sendMessage() {
  submitMessage(draft.value);
}

function urgencyLabel(value) {
  return ({ GREEN: 'Orientación preventiva', YELLOW: 'Consulta recomendada', RED: 'Atención urgente' })[value] || 'Orientación veterinaria';
}

function scrollToLatest() {
  nextTick(() => {
    if (transcript.value) transcript.value.scrollTo({ top: transcript.value.scrollHeight, behavior: 'smooth' });
  });
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault();
    closeChat();
    return;
  }
  if (event.key !== 'Tab' || !panel.value) return;
  const focusable = [...panel.value.querySelectorAll('button:not(:disabled), textarea:not(:disabled)')];
  const first = focusable[0];
  const last = focusable.at(-1);
  if (!first || !last) return;
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

watch(isOpen, async open => {
  if (open) {
    previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    await nextTick();
    composer.value?.focus();
  } else {
    document.body.style.overflow = previousOverflow;
    await nextTick();
    launcher.value?.focus();
  }
});

onBeforeUnmount(() => { document.body.style.overflow = previousOverflow; });
</script>

<template>
  <button
    v-if="!isOpen && !isAuthRoute"
    ref="launcher"
    class="assistant-launcher"
    type="button"
    aria-label="Abrir Asistente PetHop"
    aria-controls="pethop-assistant-panel"
    :aria-expanded="isOpen"
    @click="openChat"
  >
    <Icon name="paw-print" :size="25" />
  </button>

  <Transition name="assistant-panel">
    <div v-if="isOpen" class="assistant-overlay" @click.self="closeChat">
      <section
        id="pethop-assistant-panel"
        ref="panel"
        class="assistant-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="assistant-title"
        @keydown="handleKeydown"
      >
        <header class="assistant-header">
          <span class="assistant-avatar" aria-hidden="true"><Icon name="paw-print" :size="20" /></span>
          <div class="assistant-heading"><h2 id="assistant-title">Asistente PetHop</h2><p>Estoy aquí para orientarte</p></div>
          <button class="assistant-close" type="button" aria-label="Cerrar asistente" @click="closeChat"><Icon name="x" :size="20" /></button>
        </header>

        <div ref="transcript" class="assistant-transcript" aria-live="polite" aria-label="Conversación con el asistente">
          <div class="assistant-welcome">
            <span class="assistant-welcome-icon" aria-hidden="true"><Icon name="messages-square" :size="18" /></span>
            <p>¡Hola! Puedo ayudarte con tus dudas sobre adopción y cuidado responsable.</p>
          </div>
          <div v-if="!messages.length" class="assistant-quick-questions" aria-label="Preguntas frecuentes">
            <p>¿Qué te gustaría saber?</p>
            <button v-for="question in quickQuestions" :key="question" type="button" @click="submitMessage(question)">{{ question }}</button>
          </div>
          <article v-for="message in messages" :key="message.id" :class="message.role === 'assistant' && message.response ? 'assistant-ai-message' : message.role === 'assistant' ? 'assistant-general-message' : 'assistant-user-message'">
            <p v-if="message.role === 'user' || !message.response">{{ message.text }}</p>
            <template v-else>
              <div :class="['assistant-urgency', `urgency-${message.response.urgency.toLowerCase()}`]"><strong>{{ urgencyLabel(message.response.urgency) }}</strong><span>{{ message.response.urgency }}</span></div>
              <p class="assistant-ai-summary">{{ message.response.summary }}</p>
              <div v-if="message.response.reasons?.length"><strong>Por qué</strong><ul><li v-for="item in message.response.reasons" :key="item">{{ item }}</li></ul></div>
              <div v-if="message.response.warningSigns?.length"><strong>Señales de alarma</strong><ul><li v-for="item in message.response.warningSigns" :key="item">{{ item }}</li></ul></div>
              <div v-if="message.response.guidance?.length"><strong>Qué hacer</strong><ul><li v-for="item in message.response.guidance" :key="item">{{ item }}</li></ul></div>
              <small class="assistant-ai-disclaimer">{{ message.response.disclaimer }}</small>
            </template>
          </article>
          <p v-if="isSending" class="assistant-status" role="status">Estoy revisando la orientación veterinaria…</p>
          <p v-if="error" class="assistant-error" role="alert">{{ error }} <button type="button" @click="submitMessage(lastVeterinaryQuestion, { recordUser: false })">Reintentar</button></p>
        </div>

        <form class="assistant-composer" @submit.prevent="sendMessage">
          <label class="sr-only" for="assistant-message">Escribe tu mensaje</label>
          <textarea id="assistant-message" ref="composer" v-model="draft" rows="1" maxlength="1000" placeholder="Escribe tu mensaje…" @keydown.enter.exact.prevent="sendMessage" />
          <button type="submit" aria-label="Enviar mensaje" :disabled="!draft.trim()"><Icon name="send" :size="19" /></button>
        </form>
      </section>
    </div>
  </Transition>
</template>

<style scoped>
.assistant-launcher { position:fixed; z-index:1200; right:24px; bottom:80px; display:grid; width:58px; height:58px; place-items:center; border:0; border-radius:50%; background:var(--primary-strong); color:#fff; box-shadow:0 10px 28px rgba(18,63,75,.24); cursor:pointer; transition:transform .2s ease,background .2s ease,box-shadow .2s ease; }
.assistant-launcher:hover { transform:translateY(-3px); background:var(--primary-dark); box-shadow:0 14px 32px rgba(18,63,75,.3); }
.assistant-launcher:focus-visible,.assistant-close:focus-visible,.assistant-composer button:focus-visible,.assistant-quick-questions button:focus-visible { outline:3px solid #75cbd0; outline-offset:3px; }
.assistant-overlay { position:fixed; z-index:1199; inset:0; display:flex; justify-content:flex-end; background:rgba(17,45,51,.2); backdrop-filter:blur(2px); }
.assistant-panel { display:flex; width:min(410px,100%); height:100%; min-height:0; flex-direction:column; border-left:1px solid var(--border); background:#fff; box-shadow:-18px 0 50px rgba(18,63,75,.18); }
.assistant-header { display:flex; min-height:82px; align-items:center; gap:12px; padding:15px 18px; border-bottom:1px solid #dcebed; background:#fff; }
.assistant-avatar,.assistant-welcome-icon { display:grid; width:42px; height:42px; flex:0 0 42px; place-items:center; border-radius:14px; background:#e4f5f4; color:var(--primary-strong); }
.assistant-heading { min-width:0; flex:1; }
.assistant-heading h2 { color:var(--text); font:700 1rem/1.3 "Poppins",sans-serif; }
.assistant-heading p { margin-top:3px; color:var(--text-soft); font-size:.72rem; }
.assistant-close { display:grid; width:38px; height:38px; flex:0 0 38px; place-items:center; border:1px solid var(--border); border-radius:50%; background:#fff; color:var(--text-soft); cursor:pointer; transition:background .18s ease,color .18s ease; }
.assistant-close:hover { background:#e7f6f5; color:var(--primary-strong); }
.assistant-transcript { display:flex; min-height:0; flex:1; flex-direction:column; gap:18px; overflow-y:auto; padding:22px 18px; background-color:#f3feff; background-image:radial-gradient(rgba(22,127,135,.055) .7px,transparent .7px); background-size:18px 18px; }
.assistant-welcome { display:flex; max-width:92%; align-items:flex-start; gap:10px; align-self:flex-start; }
.assistant-welcome-icon { width:34px; height:34px; flex-basis:34px; border-radius:12px; }
.assistant-welcome p { padding:11px 13px; border:1px solid #d6ecee; border-radius:4px 14px 14px 14px; background:#fff; color:var(--text); font-size:.8rem; line-height:1.55; box-shadow:0 3px 10px rgba(18,63,75,.04); }
.assistant-quick-questions { display:flex; flex-direction:column; gap:9px; padding-left:44px; }
.assistant-quick-questions > p { margin-bottom:1px; color:var(--text-soft); font-size:.71rem; font-weight:700; }
.assistant-quick-questions button { padding:10px 12px; border:1px solid #a9dfe3; border-radius:12px; background:#fff; color:var(--primary-strong); cursor:pointer; font:600 .74rem/1.4 "Montserrat",sans-serif; text-align:left; transition:border-color .18s ease,background .18s ease,transform .18s ease; }
.assistant-quick-questions button:hover { transform:translateX(2px); border-color:var(--primary-strong); background:#e7f6f5; }
.assistant-user-message { max-width:84%; align-self:flex-end; padding:10px 13px; border:1px solid var(--primary-strong); border-radius:14px 14px 4px 14px; background:var(--primary-strong); box-shadow:0 3px 10px rgba(18,63,75,.08); }
.assistant-user-message p { color:#fff; font-size:.8rem; line-height:1.5; overflow-wrap:anywhere; white-space:pre-wrap; }
.assistant-general-message { max-width:92%; align-self:flex-start; padding:11px 13px; border:1px solid #d6ecee; border-radius:4px 14px 14px 14px; background:#fff; box-shadow:0 3px 10px rgba(18,63,75,.04); }
.assistant-general-message p { color:var(--text); font-size:.8rem; line-height:1.5; }
.assistant-ai-message { max-width:92%; align-self:flex-start; padding:11px 13px; border:1px solid #d6ecee; border-radius:4px 14px 14px 14px; background:#fff; color:var(--text); font-size:.78rem; line-height:1.5; box-shadow:0 3px 10px rgba(18,63,75,.04); }
.assistant-ai-message strong { font-size:.72rem; }.assistant-ai-message ul { margin:5px 0 10px 18px; }.assistant-ai-message li { margin:3px 0; }.assistant-ai-summary { margin:10px 0; }.assistant-ai-disclaimer { display:block; margin-top:10px; color:var(--text-soft); font-size:.66rem; line-height:1.45; }
.assistant-urgency { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:7px 9px; border-radius:9px; font-size:.7rem; }.assistant-urgency span { font-weight:800; }.urgency-green { background:#e5f6e9; color:#216b39; }.urgency-yellow { background:#fff4d9; color:#825e09; }.urgency-red { background:#ffe3e3; color:#9c2525; }
.assistant-status,.assistant-error { align-self:flex-start; max-width:92%; color:var(--text-soft); font-size:.74rem; }.assistant-error { color:#9c2525; }.assistant-error button { margin-left:5px; border:0; background:transparent; color:inherit; cursor:pointer; text-decoration:underline; }
.assistant-composer { display:flex; align-items:flex-end; gap:10px; padding:14px; border-top:1px solid #dcebed; background:#fff; }
.assistant-composer textarea { min-height:44px; max-height:120px; flex:1; resize:vertical; padding:12px 13px; border:1px solid #b7dfe2; border-radius:14px; outline:0; background:#f8fcfc; color:var(--text); font:500 .8rem/1.4 "Montserrat",sans-serif; }
.assistant-composer textarea:focus { border-color:var(--primary-strong); box-shadow:0 0 0 3px rgba(22,127,135,.1); }
.assistant-composer button { display:grid; width:44px; height:44px; flex:0 0 44px; place-items:center; border:0; border-radius:50%; background:var(--primary-strong); color:#fff; cursor:pointer; transition:background .18s ease,transform .18s ease,opacity .18s ease; }
.assistant-composer button:hover:not(:disabled) { transform:translateY(-1px); background:var(--primary-dark); }
.assistant-composer button:disabled { cursor:not-allowed; opacity:.45; }
.assistant-panel-enter-active,.assistant-panel-leave-active { transition:opacity .22s ease; }
.assistant-panel-enter-active .assistant-panel,.assistant-panel-leave-active .assistant-panel { transition:transform .28s cubic-bezier(.2,.75,.25,1); }
.assistant-panel-enter-from,.assistant-panel-leave-to { opacity:0; }
.assistant-panel-enter-from .assistant-panel,.assistant-panel-leave-to .assistant-panel { transform:translateX(100%); }
@media(max-width:600px) {
  .assistant-launcher { right:16px; bottom:80px; width:54px; height:54px; }
  .assistant-panel { width:100%; border-left:0; }
  .assistant-header { min-height:72px; }
  .assistant-transcript { padding:18px 14px; }
}
@media(prefers-reduced-motion:reduce) {
  .assistant-launcher,.assistant-close,.assistant-quick-questions button,.assistant-composer button,.assistant-panel-enter-active,.assistant-panel-leave-active,.assistant-panel-enter-active .assistant-panel,.assistant-panel-leave-active .assistant-panel { transition:none; }
}
</style>
