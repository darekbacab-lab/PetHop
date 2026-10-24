<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import Icon from './Icon.vue';

const isOpen = ref(false);
const draft = ref('');
const messages = ref([]);
const composer = ref(null);
const panel = ref(null);
const transcript = ref(null);
const launcher = ref(null);
const quickQuestions = [
  '¿Cómo puedo adoptar una mascota?',
  '¿Qué debo considerar antes de adoptar?',
  '¿Cómo puedo conocer el estado de salud de una mascota?'
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

function sendMessage() {
  addMessage(draft.value);
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
    v-if="!isOpen"
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
            <button v-for="question in quickQuestions" :key="question" type="button" @click="addMessage(question)">{{ question }}</button>
          </div>
          <article v-for="message in messages" :key="message.id" class="assistant-user-message"><p>{{ message.text }}</p></article>
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
.assistant-launcher { position:fixed; z-index:1200; right:24px; bottom:24px; display:grid; width:58px; height:58px; place-items:center; border:1px solid rgba(255,255,255,.65); border-radius:50%; background:var(--primary-strong); color:#fff; box-shadow:0 10px 28px rgba(18,63,75,.24); cursor:pointer; transition:transform .2s ease,background .2s ease,box-shadow .2s ease; }
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
  .assistant-launcher { right:16px; bottom:16px; width:54px; height:54px; }
  .assistant-panel { width:100%; border-left:0; }
  .assistant-header { min-height:72px; }
  .assistant-transcript { padding:18px 14px; }
}
@media(prefers-reduced-motion:reduce) {
  .assistant-launcher,.assistant-close,.assistant-quick-questions button,.assistant-composer button,.assistant-panel-enter-active,.assistant-panel-leave-active,.assistant-panel-enter-active .assistant-panel,.assistant-panel-leave-active .assistant-panel { transition:none; }
}
</style>
