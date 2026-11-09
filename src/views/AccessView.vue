<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Icon from '../components/Icon.vue';
import { login as loginApi, register } from '../services/auth.js';

const router = useRouter();
const route = useRoute();
const mode = ref(route.query.mode === 'register' ? 'register' : 'login');
const loginForm = ref({ email: '', password: '' });
const registration = ref({ profile: route.query.role === 'provider' ? 'provider' : '', fullName: '', phone: '', email: '', password: '', confirm: '' });
const showLoginPassword = ref(false);
const showRegisterPassword = ref(false);
const showRegisterConfirm = ref(false);
const loading = ref(false);
const feedback = ref({ login: '', register: '', registerError: false });
const isRegistering = computed(() => mode.value === 'register');

watch(isRegistering, registering => {
  document.title = `${registering ? 'Crear cuenta' : 'Ingresar'} | PetHop`;
  feedback.value = { login: '', register: '', registerError: false };
}, { immediate: true });

function switchMode(nextMode) {
  mode.value = nextMode;
  feedback.value = { login: '', register: '', registerError: false };
}

function validatePhone(phone) { return !phone || /^[+()0-9 .-]{7,32}$/.test(phone); }

async function submitLogin(event) {
  feedback.value.login = '';
  if (!event.currentTarget.reportValidity()) return;
  loading.value = true;
  try {
    await loginApi({ email: loginForm.value.email.trim(), password: loginForm.value.password });
    await router.push(String(route.query.redirect || '/'));
  } catch (error) {
    feedback.value.login = error.message || 'No se pudo iniciar sesión. Revisa tus datos e inténtalo de nuevo.';
  } finally { loading.value = false; }
}

async function submitRegistration(event) {
  feedback.value.register = '';
  feedback.value.registerError = false;
  const form = event.currentTarget;
  if (!registration.value.profile) { feedback.value.register = 'Selecciona cómo quieres participar en PetHop.'; feedback.value.registerError = true; return; }
  if (registration.value.password !== registration.value.confirm) { feedback.value.register = 'Las contraseñas no coinciden.'; feedback.value.registerError = true; return; }
  if (!validatePhone(registration.value.phone.trim())) { feedback.value.register = 'El teléfono no tiene un formato válido.'; feedback.value.registerError = true; return; }
  if (!form.reportValidity()) return;
  loading.value = true;
  const role = registration.value.profile === 'provider' ? 'SHELTER' : 'ADOPTER';
  try {
    await register({ fullName: registration.value.fullName.trim(), email: registration.value.email.trim(), password: registration.value.password, phone: registration.value.phone.trim() || null, role });
    await loginApi({ email: registration.value.email.trim(), password: registration.value.password });
    await router.push(role === 'SHELTER' ? '/proveedor.html?section=perfil' : String(route.query.redirect || '/'));
  } catch (error) {
    feedback.value.register = error.message || 'No se pudo completar el registro.';
    feedback.value.registerError = true;
  } finally { loading.value = false; }
}
</script>

<template>
  <div class="auth-page">
    <main class="auth-card" :class="{ 'is-register': isRegistering }">
      <section class="auth-panel" aria-labelledby="auth-title">
        <RouterLink class="auth-brand" to="/" aria-label="PetHop, volver al inicio"><img src="/img/image1.png" alt="PetHop"></RouterLink>
        <div class="auth-panel-heading">
          <h1 id="auth-title"><template v-if="isRegistering">Crea tu <span>cuenta.</span></template><template v-else>¡Hola de <span>nuevo!</span></template></h1>
          <p>{{ isRegistering ? 'Únete a una comunidad que conecta mascotas con hogares responsables.' : 'Ingresa para continuar con tu proceso de adopción.' }}</p>
        </div>

        <form v-if="!isRegistering" class="auth-form" novalidate @submit.prevent="submitLogin">
          <label for="login-email">Correo electrónico</label>
          <div class="auth-input-wrap"><Icon name="message-circle" /><input id="login-email" v-model="loginForm.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required></div>
          <label for="login-password">Contraseña</label>
          <div class="auth-input-wrap"><Icon name="shield-check" /><input id="login-password" v-model="loginForm.password" name="password" :type="showLoginPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Tu contraseña" maxlength="72" required><button class="auth-password-toggle" type="button" :aria-label="showLoginPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'" :aria-pressed="showLoginPassword" @click="showLoginPassword = !showLoginPassword"><svg class="auth-eye-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/><path v-if="showLoginPassword" d="m4 4 16 16"/></svg></button></div>
          <button class="auth-submit" type="submit" :disabled="loading"><span v-if="loading" class="auth-spinner" aria-hidden="true"></span>{{ loading ? 'Ingresando…' : 'Ingresar' }}</button>
          <p class="auth-feedback" :class="{ 'is-error': feedback.login }" role="alert" aria-live="polite">{{ feedback.login }}</p>
          <p class="auth-switch-copy">¿Aún no tienes una cuenta? <button type="button" class="auth-text-button" @click="switchMode('register')">Crear cuenta</button></p>
        </form>

        <form v-else class="auth-form auth-register-form" novalidate @submit.prevent="submitRegistration">
          <fieldset class="auth-profile-choice"><legend>Quiero…</legend><label class="auth-choice-pill" :class="{ 'is-selected': registration.profile === 'adopter' }"><input v-model="registration.profile" type="radio" name="profile" value="adopter"><span>Adoptar</span></label><label class="auth-choice-pill" :class="{ 'is-selected': registration.profile === 'provider' }"><input v-model="registration.profile" type="radio" name="profile" value="provider"><span>Dar en adopción</span></label></fieldset>
          <label for="register-full-name">Nombre completo</label><div class="auth-input-wrap"><Icon name="user-round" /><input id="register-full-name" v-model="registration.fullName" name="fullName" autocomplete="name" maxlength="160" placeholder="Tu nombre" required></div>
          <label for="register-phone">Teléfono <span class="auth-optional">(opcional)</span></label><div class="auth-input-wrap"><Icon name="contact-round" /><input id="register-phone" v-model="registration.phone" name="phone" type="tel" autocomplete="tel" placeholder="Número de contacto" pattern="^[+()0-9 .-]{7,32}$"></div>
          <label for="register-email">Correo electrónico</label><div class="auth-input-wrap"><Icon name="message-circle" /><input id="register-email" v-model="registration.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required></div>
          <label for="register-password">Contraseña</label><div class="auth-input-wrap"><Icon name="shield-check" /><input id="register-password" v-model="registration.password" name="password" :type="showRegisterPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="Al menos 12 caracteres" minlength="12" maxlength="72" required><button class="auth-password-toggle" type="button" :aria-label="showRegisterPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'" :aria-pressed="showRegisterPassword" @click="showRegisterPassword = !showRegisterPassword"><svg class="auth-eye-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/><path v-if="showRegisterPassword" d="m4 4 16 16"/></svg></button></div>
          <label for="register-confirm">Confirmar contraseña</label><div class="auth-input-wrap"><Icon name="shield-check" /><input id="register-confirm" v-model="registration.confirm" name="confirm" :type="showRegisterConfirm ? 'text' : 'password'" autocomplete="new-password" placeholder="Repite tu contraseña" minlength="12" maxlength="72" required><button class="auth-password-toggle" type="button" :aria-label="showRegisterConfirm ? 'Ocultar confirmación' : 'Mostrar confirmación'" :aria-pressed="showRegisterConfirm" @click="showRegisterConfirm = !showRegisterConfirm"><svg class="auth-eye-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/><path v-if="showRegisterConfirm" d="m4 4 16 16"/></svg></button></div>
          <p class="auth-requirement">Tu contraseña debe tener entre 12 y 72 caracteres.</p>
          <button class="auth-submit" type="submit" :disabled="loading"><span v-if="loading" class="auth-spinner" aria-hidden="true"></span>{{ loading ? 'Creando cuenta…' : 'Crear cuenta' }}</button>
          <p class="auth-feedback" :class="{ 'is-error': feedback.registerError }" role="alert" aria-live="polite">{{ feedback.register }}</p>
          <p class="auth-switch-copy">¿Ya tienes una cuenta? <button type="button" class="auth-text-button" @click="switchMode('login')">Iniciar sesión</button></p>
        </form>
      </section>
      <aside class="auth-visual" aria-label="Mascotas que esperan un hogar responsable">
        <div class="auth-visual-image"><img src="/img/mascotas-hero-v2.png" alt="Perro y gato juntos"></div>
        <template v-if="isRegistering"><h2>Un buen encuentro empieza con <span>información clara.</span></h2><p>PetHop reúne perfiles de mascotas, solicitudes y seguimiento para acompañar cada adopción con responsabilidad.</p></template>
      </aside>
    </main>
  </div>
</template>
