import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';
import { isVeterinaryQuestion } from '../src/services/assistantIntent.js';

const assistant = fs.readFileSync(new URL('../src/components/AssistantChat.vue', import.meta.url), 'utf8');
const petView = fs.readFileSync(new URL('../src/views/PetView.vue', import.meta.url), 'utf8');
const app = fs.readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8');
const security = fs.readFileSync(new URL('../backend/src/main/java/com/pethop/config/SecurityConfig.java', import.meta.url), 'utf8');
const controller = fs.readFileSync(new URL('../backend/src/main/java/com/pethop/veterinary/GeneralVeterinaryAssistantController.java', import.meta.url), 'utf8');

test('Paw keeps its single global launcher and calls veterinary AI from the existing panel', () => {
  assert.equal((app.match(/<AssistantChat\b/g) || []).length, 1);
  assert.equal((assistant.match(/class="assistant-launcher"/g) || []).length, 1);
  assert.match(assistant, /isAuthRoute = computed\(\(\) => route\.path === '\/acceso\.html'\)/);
  assert.match(assistant, /v-if="!isOpen && !isAuthRoute"/);
  assert.match(assistant, /aria-label="Abrir Asistente PetHop"/);
  assert.match(assistant, /@click="openChat"/);
  assert.match(assistant, /id="pethop-assistant-panel"/);
  assert.match(assistant, /veterinary-assistant\/messages/);
  assert.match(assistant, /petId: currentPetId\.value/);
});

test('Paw provides veterinary guidance with or without a selected pet', () => {
  assert.match(assistant, /currentPetId = computed/);
  assert.match(assistant, /route\.path === '\/mascota\.html'/);
  assert.match(assistant, /isVeterinaryQuestion/);
  assert.match(assistant, /urgencyLabel/);
  assert.match(assistant, /role="alert"/);
  assert.match(assistant, /no se pudo obtener orientación veterinaria/i);
  assert.match(controller, /@RequestMapping\("\/api\/v1\/veterinary-assistant"\)/);
  assert.match(security, /veterinary-assistant\/\*\*"\)\.permitAll\(\)/);
});

test('the independent pet-detail veterinary interface is no longer rendered', () => {
  assert.doesNotMatch(petView, /VeterinaryAssistant/);
  assert.doesNotMatch(petView, /Consultar orientación veterinaria/);
  assert.doesNotMatch(assistant, /assistant-launcher.*veterinary|paw-print.*veterinary/i);
});

test('Paw preserves general assistant suggestions and composer behavior', () => {
  assert.match(assistant, /¿Cómo puedo adoptar una mascota\?/);
  assert.match(assistant, /¿Qué debo considerar antes de adoptar\?/);
  assert.match(assistant, /@submit\.prevent="sendMessage"/);
  assert.match(assistant, /maxlength="1000"/);
  assert.match(assistant, /generalAnswer\(text\)/);
  assert.match(assistant, /assistant-general-message/);
  assert.match(assistant, /recordUser: false/);
  assert.match(assistant, /isVeterinaryQuestion\(text\)/);
  assert.doesNotMatch(assistant, /Inicia PostgreSQL|Reinicia Spring Boot|backend esté iniciado/i);
});

test('Paw routes veterinary language variants to the provider', () => {
  assert.equal(isVeterinaryQuestion('¿Qué debo hacer para bañar a mi perro?'), true);
  assert.equal(isVeterinaryQuestion('Mi perro no está comiendo bien'), true);
  assert.equal(isVeterinaryQuestion('Mi gato tiene diarrea y no quiere comer'), true);
  assert.equal(isVeterinaryQuestion('¿Cómo adopto esta mascota?'), false);
  assert.equal(isVeterinaryQuestion('¿Cómo funciona PetHop?'), false);
});
