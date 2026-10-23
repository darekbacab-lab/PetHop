<script setup>
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storageService } from '../services/storage.js';

const router = useRouter();
const mode = ref('login');
const login = ref({ email: '', password: '' });
const registration = ref({ profile: '', providerKind: '', email: '', password: '', confirm: '' });
const showLoginPassword = ref(false);
const showRegisterPassword = ref(false);
const feedback = ref({ login: '', register: '', registerError: false });
const isRegistering = computed(() => mode.value === 'register');

watch(isRegistering, registering => {
  document.title = `${registering ? 'Crear cuenta' : 'Ingresar'} | PetHop`;
  feedback.value = { login: '', register: '', registerError: false };
}, { immediate: true });

function submitLogin(event) {
  if (!event.currentTarget.reportValidity()) return;
  // This prototype only validates the form; it has no server authentication.
  router.push('/');
}

function submitRegistration(event) {
  feedback.value.register = '';
  feedback.value.registerError = false;
  if (registration.value.password !== registration.value.confirm) {
    feedback.value.register = 'Las contraseñas no coinciden.';
    feedback.value.registerError = true;
    return;
  }

  if (!event.currentTarget.reportValidity()) return;

  if (registration.value.profile === 'provider') {
    const state = storageService.read('pethop_provider_workspace', {}) || {};
    state.role = registration.value.providerKind;
    state.section = 'Inicio';
    state.profile = { ...(state.profile || {}), email: registration.value.email.trim() };
    if (!storageService.write('pethop_provider_workspace', state)) {
      feedback.value.register = 'No se pudo guardar el perfil en este dispositivo. Revisa el almacenamiento del navegador e inténtalo de nuevo.';
      feedback.value.registerError = true;
      return;
    }
    router.push('/proveedor.html');
    return;
  }

  router.push('/');
}
</script>

<template>
  <div class="auth-page">
    <header class="auth-header">
      <RouterLink class="brand" to="/" aria-label="PetHop, volver al inicio">
        <img src="/img/image1.png" alt="PetHop">
      </RouterLink>
    </header>

    <main class="auth-layout">
      <section class="auth-panel" aria-labelledby="auth-title">
        <div class="auth-panel-heading">
          <span class="eyebrow">TU ESPACIO EN PETHOP</span>
          <h1 id="auth-title">{{ isRegistering ? 'Crea tu cuenta.' : 'Qué bueno verte.' }}</h1>
          <p>{{ isRegistering ? 'Cuéntanos cómo quieres participar en PetHop.' : 'Ingresa para continuar con tu proceso de adopción.' }}</p>
        </div>

        <form v-if="!isRegistering" class="auth-form" novalidate @submit.prevent="submitLogin">
          <label for="login-email">Correo electrónico</label>
          <input id="login-email" v-model="login.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required>
          <label for="login-password">Contraseña</label>
          <div class="auth-password-field">
            <input id="login-password" v-model="login.password" name="password" :type="showLoginPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Tu contraseña" minlength="8" required>
            <button class="auth-password-toggle" type="button" :aria-pressed="showLoginPassword" @click="showLoginPassword = !showLoginPassword">{{ showLoginPassword ? 'Ocultar' : 'Mostrar' }}</button>
          </div>
          <button class="login-button auth-submit" type="submit">Iniciar sesión</button>
          <p class="auth-feedback" role="status" aria-live="polite">{{ feedback.login }}</p>
          <p class="auth-switch-copy">¿Aún no tienes cuenta? <button type="button" class="auth-text-button" @click="mode = 'register'">Crear cuenta</button></p>
        </form>

        <form v-else class="auth-form" novalidate @submit.prevent="submitRegistration">
          <fieldset class="auth-profile-choice">
            <legend>¿Qué quieres hacer en PetHop?</legend>
            <label class="auth-choice-card">
              <input v-model="registration.profile" type="radio" name="profile" value="adopter" required>
              <span><strong>Quiero adoptar</strong><small>Explorar mascotas y dar seguimiento a mis solicitudes.</small></span>
            </label>
            <label class="auth-choice-card">
              <input v-model="registration.profile" type="radio" name="profile" value="provider" required>
              <span><strong>Quiero dar en adopción</strong><small>Compartir una mascota y gestionar solicitudes.</small></span>
            </label>
          </fieldset>

          <fieldset v-if="registration.profile === 'provider'" class="auth-provider-kind">
            <legend>¿Cómo darás en adopción?</legend>
            <label><input v-model="registration.providerKind" type="radio" name="provider-kind" value="particular" required> Particular</label>
            <label><input v-model="registration.providerKind" type="radio" name="provider-kind" value="organization" required> Organización o refugio</label>
            <small>Elegir organización no significa que el refugio esté verificado.</small>
          </fieldset>

          <label for="register-email">Correo electrónico</label>
          <input id="register-email" v-model="registration.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required>
          <label for="register-password">Contraseña</label>
          <div class="auth-password-field">
            <input id="register-password" v-model="registration.password" name="password" :type="showRegisterPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="Al menos 8 caracteres" minlength="8" required>
            <button class="auth-password-toggle" type="button" :aria-pressed="showRegisterPassword" @click="showRegisterPassword = !showRegisterPassword">{{ showRegisterPassword ? 'Ocultar' : 'Mostrar' }}</button>
          </div>
          <label for="register-confirm">Confirmar contraseña</label>
          <input id="register-confirm" v-model="registration.confirm" name="confirm" type="password" autocomplete="new-password" placeholder="Repite tu contraseña" minlength="8" required>
          <button class="organization-button auth-submit" type="submit">Crear cuenta</button>
          <p class="auth-feedback" :class="{ 'is-error': feedback.registerError }" role="status" aria-live="polite">{{ feedback.register }}</p>
          <p class="auth-switch-copy">¿Ya tienes cuenta? <button type="button" class="auth-text-button" @click="mode = 'login'">Iniciar sesión</button></p>
        </form>
      </section>

      <aside class="auth-aside" aria-label="Adopción responsable en PetHop">
        <span class="auth-aside-mark" aria-hidden="true">♡</span>
        <span class="eyebrow">ADOPCIÓN RESPONSABLE</span>
        <h2>Un buen encuentro empieza con <span>información clara.</span></h2>
        <p>PetHop reúne perfiles de mascotas, solicitudes y seguimiento para acompañar cada adopción con responsabilidad.</p>
      </aside>
    </main>
  </div>
</template>
