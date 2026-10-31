<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { login as loginApi, register } from '../services/auth.js';
import { api } from '../services/api.js';
import { sessionStorageService } from '../services/storage.js';

const router = useRouter(); const route = useRoute();
const shelterDraft = sessionStorageService.read('pethop_shelter_registration_draft', {}) || {};
sessionStorageService.remove('pethop_shelter_registration_draft');
const mode = ref('login');
const loginForm = ref({ email: '', password: '' });
const registration = ref({ profile: route.query.role === 'provider' ? 'provider' : '', providerKind: route.query.role === 'provider' ? 'organization' : '', fullName: String(shelterDraft.fullName || ''), organizationName: String(shelterDraft.organizationName || ''), description: String(shelterDraft.description || ''), phone: String(shelterDraft.phone || ''), email: String(shelterDraft.email || ''), password: '', confirm: '' });
const showLoginPassword = ref(false);
const showRegisterPassword = ref(false);
const feedback = ref({ login: '', register: '', registerError: false });
const isRegistering = computed(() => mode.value === 'register');
if (route.query.mode === 'register') mode.value = 'register';

watch(isRegistering, registering => {
  document.title = `${registering ? 'Crear cuenta' : 'Ingresar'} | PetHop`;
  feedback.value = { login: '', register: '', registerError: false };
}, { immediate: true });

async function submitLogin(event) {
  if (!event.currentTarget.reportValidity()) return;
  feedback.value.login = '';
  try { await loginApi({ email: loginForm.value.email.trim(), password: loginForm.value.password }); await router.push(String(route.query.redirect || '/')); }
  catch (error) { feedback.value.login = error.message || 'No se pudo iniciar sesión. Revisa tus datos e inténtalo de nuevo.'; }
}

async function submitRegistration(event) {
  feedback.value.register = '';
  feedback.value.registerError = false;
  if (registration.value.password !== registration.value.confirm) {
    feedback.value.register = 'Las contraseñas no coinciden.';
    feedback.value.registerError = true;
    return;
  }

  if (!event.currentTarget.reportValidity()) return;

  const role = registration.value.profile === 'provider' ? 'SHELTER' : 'ADOPTER';
  try {
    const user = await register({ fullName: registration.value.fullName.trim(), email: registration.value.email.trim(), password: registration.value.password, phone: registration.value.phone.trim() || null, role });
    await loginApi({ email: registration.value.email.trim(), password: registration.value.password });
    if (role === 'SHELTER') {
      await api('/shelters', { method: 'POST', body: { organizationName: registration.value.providerKind === 'organization' ? registration.value.organizationName.trim() : `Rescatista · ${user.fullName}`, description: registration.value.description || '', contactPhone: registration.value.phone.trim() || '' } });
    }
    await router.push(role === 'SHELTER' ? '/proveedor.html' : String(route.query.redirect || '/'));
  } catch (error) {
    feedback.value.register = error.message || 'No se pudo completar el registro.';
    feedback.value.registerError = true;
  }
}
</script>

<template>
  <div class="auth-page">
    <header class="auth-header">
      <RouterLink class="brand" to="/" aria-label="PetHop, volver al inicio">
        <img class="auth-logo-color" src="/img/image1.png" alt="">
        <img class="auth-logo-mono" src="/img/Logomonocr.png" alt="">
      </RouterLink>
    </header>

    <main class="auth-layout">
      <section class="auth-panel" aria-labelledby="auth-title">
        <div class="auth-panel-heading">
          <span class="eyebrow">TU ESPACIO EN PETHOP</span>
          <h1 id="auth-title"><template v-if="isRegistering">Crea tu <span>cuenta.</span></template><template v-else>Qué bueno <span>verte.</span></template></h1>
          <p>{{ isRegistering ? 'Cuéntanos cómo quieres participar en PetHop.' : 'Ingresa para continuar con tu proceso de adopción.' }}</p>
        </div>

        <form v-if="!isRegistering" class="auth-form" novalidate @submit.prevent="submitLogin">
          <label for="login-email">Correo electrónico</label>
          <input id="login-email" v-model="loginForm.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required>
          <label for="login-password">Contraseña</label>
          <div class="auth-password-field">
            <input id="login-password" v-model="loginForm.password" name="password" :type="showLoginPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Tu contraseña" minlength="12" required>
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

          <label for="register-full-name">Nombre completo</label>
          <input id="register-full-name" v-model="registration.fullName" name="fullName" autocomplete="name" maxlength="160" placeholder="Tu nombre" required>
          <template v-if="registration.profile === 'provider' && registration.providerKind === 'organization'">
            <label for="register-organization-name">Nombre de la organización</label>
            <input id="register-organization-name" v-model="registration.organizationName" name="organizationName" maxlength="180" placeholder="Nombre del refugio u organización" required>
          </template>
          <label for="register-phone">Teléfono <span>(opcional)</span></label>
          <input id="register-phone" v-model="registration.phone" name="phone" type="tel" autocomplete="tel" placeholder="Número de contacto">
          <label for="register-email">Correo electrónico</label>
          <input id="register-email" v-model="registration.email" name="email" type="email" autocomplete="email" placeholder="nombre@correo.com" required>
          <label for="register-password">Contraseña</label>
          <div class="auth-password-field">
            <input id="register-password" v-model="registration.password" name="password" :type="showRegisterPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="Al menos 12 caracteres" minlength="12" maxlength="72" required>
            <button class="auth-password-toggle" type="button" :aria-pressed="showRegisterPassword" @click="showRegisterPassword = !showRegisterPassword">{{ showRegisterPassword ? 'Ocultar' : 'Mostrar' }}</button>
          </div>
          <label for="register-confirm">Confirmar contraseña</label>
          <input id="register-confirm" v-model="registration.confirm" name="confirm" type="password" autocomplete="new-password" placeholder="Repite tu contraseña" minlength="12" maxlength="72" required>
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
