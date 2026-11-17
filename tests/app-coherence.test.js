import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { createStorageService, sessionStorageService } from '../src/services/storage.js';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

class MemoryStorage {
    #items = new Map();
    getItem(key) { return this.#items.has(key) ? this.#items.get(key) : null; }
    setItem(key, value) { this.#items.set(key, String(value)); }
    removeItem(key) { this.#items.delete(key); }
}


test('the application storage adapter preserves existing JSON keys and handles blocked storage', () => {
    const driver = new MemoryStorage();
    const service = createStorageService(() => driver);
    assert.equal(service.write('pethop_favorites', ['chino']), true);
    assert.deepEqual(service.read('pethop_favorites', []), ['chino']);
    assert.equal(service.remove('pethop_favorites'), true);
    assert.deepEqual(service.read('pethop_favorites', []), []);

    const blocked = createStorageService(() => { throw new Error('blocked'); });
    assert.deepEqual(blocked.read('pethop_user', null), null);
    assert.equal(blocked.write('pethop_user', { name: 'Prueba' }), false);
});

test('temporary selected-pet state is routed through the session storage adapter', () => {
    const prior = globalThis.sessionStorage;
    const driver = new MemoryStorage();
    globalThis.sessionStorage = driver;
    try {
        assert.equal(sessionStorageService.write('pethop_selected_pet', { id: 'chino' }), true);
        assert.deepEqual(sessionStorageService.read('pethop_selected_pet'), { id: 'chino' });
    } finally {
        if (prior === undefined) delete globalThis.sessionStorage;
        else globalThis.sessionStorage = prior;
    }
});

test('the active views own declarative templates and the legacy implementation is removed', () => {
    const pages = {
        'HomeView.vue': 'HomeView.vue',
        'AccessView.vue': 'AccessView.vue',
        'ExploreView.vue': 'ExploreView.vue',
        'PetView.vue': 'PetView.vue',
        'AdoptionView.vue': 'AdoptionView.vue',
        'FollowUpView.vue': 'FollowUpView.vue',
        'ShelterView.vue': 'ShelterView.vue'
    };
    assert.equal(fs.existsSync(path.join(projectRoot, 'src', 'legacy', 'pages')), false, 'archived legacy pages are removed');
    for (const view of Object.values(pages)) {
        const viewSource = fs.readFileSync(path.join(projectRoot, 'src', 'views', view), 'utf8');
        assert.ok(viewSource.includes('<template'), `${view} has a Vue template`);
        assert.ok(!viewSource.includes('LegacyPageRenderer'), `${view} does not use the legacy renderer`);
        assert.ok(!viewSource.includes('v-html'), `${view} does not inject markup`);
        assert.ok(!viewSource.includes('../legacy/pages/'), `${view} does not import archived HTML`);
    }
});

test('former global controllers and navigation scripts are removed', () => {
    for (const file of ['app.js', 'js/acceso.js', 'js/proveedor.js', 'js/navigation.js', 'js/site-navigation.js']) {
        assert.equal(fs.existsSync(path.join(projectRoot, file)), false, `${file} is removed`);
    }
});

test('active HTML entry points load only the Vue/Vite bootstrap', () => {
    for (const page of ['index.html', 'acceso.html', 'explorar.html', 'mascota.html', 'refugio.html', 'seguimiento.html', 'solicitud.html', 'perfil.html']) {
        const html = fs.readFileSync(path.join(projectRoot, page), 'utf8');
        assert.ok(html.includes('src="/src/main.js"'), `${page} loads the Vue application`);
        assert.ok(!html.includes('app.js'), `${page} does not load the former global controller`);
        assert.ok(!html.includes('js/proveedor.js'), `${page} does not load the former shelter controller`);
        assert.ok(!html.includes('js/acceso.js'), `${page} does not load the former access controller`);
    }
    for (const view of ['HomeView.vue','AccessView.vue','ExploreView.vue','PetView.vue','AdoptionView.vue','FollowUpView.vue','ShelterView.vue','AdopterProfileView.vue']) {
        const source = fs.readFileSync(path.join(projectRoot, 'src', 'views', view), 'utf8');
        assert.ok(!source.includes('legacyRuntime'), `${view} does not import legacy runtime`);
    }
});

test('all HTML entry points load the Vue application and route manifest covers every existing page', () => {
    const expectedRoutes = ['/acceso.html', '/explorar.html', '/mascota.html', '/solicitud.html', '/seguimiento.html', '/mensajes.html', '/perfil.html', '/refugio.html'];
    const router = fs.readFileSync(path.join(projectRoot, 'src', 'router', 'index.js'), 'utf8');
    for (const route of expectedRoutes) assert.ok(router.includes(`path: '${route}'`), `router registers ${route}`);

    for (const page of ['index.html', ...expectedRoutes.map(route => route.slice(1))]) {
        const html = fs.readFileSync(path.join(projectRoot, page), 'utf8');
        assert.ok(html.includes('/src/main.js'), `${page} starts Vue/Vite`);
    }
});

test('account navigation and profiles use the authenticated session and persisted profile endpoints', () => {
    const auth = fs.readFileSync(path.join(projectRoot, 'src/services/auth.js'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const adopter = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');

    assert.match(auth, /currentUser\s*=\s*ref\(null\)/);
    assert.match(auth, /api\('\/auth\/me'\)/);
    assert.match(auth, /api\('\/auth\/logout'/);
    assert.match(header, /role !== 'SHELTER'/);
    assert.match(header, /:to="accountRoute"/);
    assert.match(header, /accountRoute = computed\(\(\) => '\/perfil\.html'\)/);
    assert.match(header, /account-trigger[\s\S]*notification-trigger/);
    assert.match(header, /notification-trigger[^}]*width:48px; height:48px/);
    assert.doesNotMatch(header, /account-dropdown|accountOpen/);
    assert.match(header, /account-nav-signout/);
    assert.match(header, /accountDisplayName/);
    assert.match(adopter, /Cerrar sesión/);
    assert.doesNotMatch(adopter, /adopter-inline-edit/);
    assert.match(adopter, /api\('\/users\/me'\)/);
    assert.match(adopter, /\/adoption-applications\/mine/);
    assert.match(adopter, /\/adoptions\/mine/);
    assert.match(adopter, /listFavorites\(\)/);
    assert.match(adopter, /api\('\/users\/me\/role', \{ method: 'PATCH'/);
    assert.match(adopter, /value="SHELTER">Refugio o rescatista/);
    assert.match(adopter, /!\['ADOPTER', 'SHELTER', 'ADMIN'\]/);
    assert.match(adopter, /passwordOpen = ref\(false\)/);
    assert.match(adopter, /v-if="!passwordOpen"[^>]*>Cambiar contraseña/);
    assert.match(shelter, /api\('\/users\/me'\)/);
    assert.match(shelter, /api\('\/shelters\/me'\)/);
    assert.match(shelter, /api\('\/shelters',\s*\{\s*method:\s*'POST'/);
});

test('edit profile controls have a registered pencil icon', () => {
    const icons = fs.readFileSync(path.join(projectRoot, 'src/icons/lucide-icons.js'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    assert.match(shelter, /class="shelter-profile-edit"[\s\S]*?<Icon name="pencil"/);
    assert.match(icons, /"pencil":\[\["path"/);
});

test('pet profile and both chats show the real conversation contact, pet and location', () => {
    const petResponse = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/pet/dto/PetResponse.java'), 'utf8');
    const petProfile = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const adopterChat = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    const shelterChat = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    assert.match(petResponse, /String shelterName/);
    assert.match(petProfile, /Publicado por <strong>{{ pet\.shelterName }}/);
    assert.match(adopterChat, /pet\?\.shelterName/);
    assert.match(adopterChat, /<strong>{{ publisherName\(activeConversation\)/);
    assert.match(adopterChat, /Sobre: {{ activeConversation\.petName }}/);
    assert.match(adopterChat, /Ubicación: {{ activeConversation\.pet\.location }}/);
    assert.match(shelterChat, /petLocation: pet\?\.location/);
    assert.match(shelterChat, /<strong>{{ activeConversation\.adopter }}/);
    assert.match(shelterChat, /Sobre: {{ activeConversation\.pet }}/);
    assert.match(shelterChat, /Ubicación: {{ activeConversation\.petLocation }}/);
});

test('conversation actions use per-user persisted pin and hide endpoints', () => {
    const adopterChat = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    const controller = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/messaging/MessagingController.java'), 'utf8');
    const service = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/messaging/MessagingService.java'), 'utf8');
    const migration = fs.readFileSync(path.join(projectRoot, 'backend/src/main/resources/db/migration/V10__conversation_participant_preferences.sql'), 'utf8');
    assert.match(adopterChat, /Fijar conversación/);
    assert.match(adopterChat, /Eliminar conversación/);
    assert.match(adopterChat, /method: 'PATCH', body: \{ pinned \}/);
    assert.match(adopterChat, /method: 'DELETE'/);
    assert.match(controller, /@PatchMapping\("\/conversations\/\{id\}\/pin"\)/);
    assert.match(controller, /@DeleteMapping\("\/conversations\/\{id\}"\)/);
    assert.match(service, /findAllByUserIdAndHiddenAtIsNullOrderByPinnedDescJoinedAtDesc/);
    assert.match(migration, /pinned BOOLEAN NOT NULL DEFAULT FALSE/);
    assert.match(migration, /hidden_at TIMESTAMPTZ/);
});

test('main stylesheet has balanced rule blocks', () => {
    const css = fs.readFileSync(path.resolve(projectRoot, 'css/styles.css'), 'utf8')
        .replace(/\/\*[\s\S]*?\*\//g, '')
        .replace(/"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/g, '');
    let depth = 0;
    for (const character of css) {
        if (character === '{') depth += 1;
        if (character === '}') depth -= 1;
        assert.ok(depth >= 0, 'CSS contains an unexpected closing brace');
    }
    assert.equal(depth, 0, 'CSS has unclosed rule blocks');
});

test('the sidebar stays above its backdrop and can close through Vue controls', () => {
    const css = fs.readFileSync(path.resolve(projectRoot, 'css/styles.css'), 'utf8');
    const header = css.match(/\.site-header\s*\{[^}]*z-index:\s*(\d+)/);
    const backdrop = css.match(/\.menu-backdrop\s*\{[^}]*z-index:\s*(\d+)/);
    const shelterHeader = css.match(/\.shelter-page\s+\.site-header\s*\{[^}]*z-index:\s*(\d+)/);
    assert.ok(header && backdrop && shelterHeader, 'header, backdrop and shelter header define stacking levels');
    assert.ok(Number(header[1]) > Number(backdrop[1]), 'header stacking context remains above the backdrop');
    assert.ok(Number(shelterHeader[1]) > Number(backdrop[1]), 'shelter entry header remains above the backdrop');

    const source = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    assert.match(source, /:class="\{ open \}"/);
    assert.match(source, /:inert="!open"/);
    assert.match(source, /function closeMenu\(returnFocus = false\)/);
    assert.match(source, /menuToggle\.value\?\.focus\(\)/);
    assert.match(source, /closeMenu\(true\)/);
    assert.doesNotMatch(source, /:aria-hidden="!open"/);
    assert.match(source, /@click="open = !open"/);
    for (const label of ['Inicio', 'Explorar', 'Dar en adopción', 'Seguimiento', 'Favoritos']) assert.ok(source.includes(label));
});

test('adopter favorites modal reads and removes favorites through the API', () => {
    const source = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    assert.match(source, /from '\.\.\/services\/petApi\.js'/);
    assert.match(source, /favorites\.value = await listFavorites\(\)/);
    assert.match(source, /await deleteFavorite\(id\)/);
    assert.match(source, /function openFavorites\(\)/);
    assert.doesNotMatch(source, /getFavorites|getPet|toggleFavorite/);
    assert.match(source, /https\?:\\\/\\\//);
    assert.match(source, /favoritesLoading/);
    assert.match(source, /favoritesError/);
});

test('catalog cards retain information hierarchy and link directly to the persisted pet profile', () => {
    const source = fs.readFileSync(path.join(projectRoot, 'src/components/PetCard.vue'), 'utf8');
    for (const marker of ['catalog-card-image', 'catalog-card-content', 'catalog-card-top', 'catalog-details', 'catalog-location']) {
        assert.ok(source.includes(marker), `Vue card keeps ${marker}`);
    }
    assert.match(source, /<RouterLink class="pet-card-hit-area"/);
    assert.match(source, /path: '\/mascota\.html', query: \{ pet: pet\.id \}/);
    assert.match(source, /aria-label="`Abrir perfil de \$\{pet\.name\}`"/);
    assert.match(source, /@click\.stop="emit\('favorite', pet\.id\)"/);
    assert.doesNotMatch(source, /pethop_selected_pet/);
    assert.doesNotMatch(source, /Conocer más/);
});

test('organization registration uses one account form then creates and edits its separate shelter profile', () => {
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    assert.match(home, /Registrar refugio/);
    assert.match(home, /mode:'register', role:'shelter'/);
    assert.doesNotMatch(home, /organization-registration-modal|registerOrganization|pethop_shelter_registration_draft/);
    assert.match(access, /register\(\{ fullName:/);
    assert.match(access, /Te enviamos un correo de confirmación/);
    assert.match(access, /mode\.value = 'login'/);
    assert.doesNotMatch(access, /await loginApi\(\{ email: registration\.value\.email/);
    assert.doesNotMatch(access, /api\('\/shelters'/);
    assert.match(shelter, /function createOrganizationProfile\(\)/);
    assert.match(shelter, /api\('\/shelters', \{ method: 'POST'/);
    assert.match(shelter, /api\('\/users\/me', \{ method: 'PATCH'/);
    assert.match(shelter, /api\('\/shelters\/me', \{ method: 'PATCH'/);
});

test('how-it-works carousel shows exactly three cards with dots instead of a text range', () => {
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    // Tres tarjetas completas llenan el carrusel en escritorio, sin vecinas asomando.
    // El ancho usa unidades de viewport a propósito: un porcentaje se resolvería
    // contra la caja de contenido del propio carril y colapsaría las tarjetas.
    assert.match(home, /--process-slide-width:calc\(\(min\(var\(--container-width\), 100vw - 56px\) - 56px\) \/ 3\)/);
    assert.doesNotMatch(home, /--process-slide-width:min\(361px/);
    assert.doesNotMatch(home, /--process-slide-width:calc\(\(100% - 36px\) \/ 3\)/);
    // Cada tarjeta flota por su cuenta: separación ancha entre tarjetas y hueco
    // debajo del carril para que la sombra de cada una se vea entera y no se
    // funda con la de la vecina.
    assert.match(home, /@media \(min-width: 901px\) \{[^}]*\.process-carousel-track \{[^}]*gap:28px;/);
    assert.match(home, /\.process-carousel-track \{[^}]*padding:22px max\(calc\(\(100% - min\(680px, 74vw\)\) \/ 2\), 14px\) 44px;/);
    assert.match(home, /const processPosition = ref\(2\)/);
    assert.match(home, /let processNavigationTarget = 2;/);
    assert.match(home, /centerProcessSlide\(2, 'auto'\)/);
    // El indicador es solo puntos, sin texto de rango.
    assert.match(home, /class="process-carousel-dots"/);
    assert.match(home, /const processActiveStep = computed\(\(\) => \{/);
    assert.match(home, /function goToProcessSlide\(position\)/);
    assert.match(home, /@click="goToProcessSlide\(index \+ 1\)"/);
    assert.doesNotMatch(home, /processPositionLabel|processVisibleRange|Mostrando pasos/);
    // La tarjeta sigue el diseño de Figma: proporción 426×585, base turquesa del
    // 32% que sube en onda sobre la foto y sombra Drop shadow 0 8 11 negro 25%.
    assert.match(home, /aspect-ratio:426 \/ 585/);
    assert.match(home, /grid-template-rows:68% 32%; grid-template-areas:"art" "copy"/);
    assert.doesNotMatch(home, /grid-template-rows:70% 30%/);
    assert.doesNotMatch(home, /grid-template-rows:64% 36%/);
    assert.match(home, /\.process-steps \.process-step \{[^}]*border-radius:24px;/);
    assert.match(home, /\.process-steps \.process-step \{[^}]*box-shadow:var\(--shadow-sm\);/);
    assert.match(home, /\.process-step-copy::before \{[^}]*data:image\/svg\+xml/);
    assert.doesNotMatch(home, /linear-gradient\(180deg, #17848c/);
    assert.match(home, /\.process-steps \.process-step-copy \{[^}]*background:var\(--button-primary-bg\);/);
});

test('home pet carousel keeps its arrows without shadow and its card shadows are not clipped', () => {
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    // Las flechas vuelven: se quita solo su sombra, no las flechas.
    assert.match(home, /home-pets-arrow home-pets-arrow-previous/);
    assert.match(home, /home-pets-arrow home-pets-arrow-next/);
    assert.doesNotMatch(home, /\.home-pets-arrow \{[^}]*box-shadow:/);
    assert.match(home, /\.home-pets-carousel \{[^}]*margin-inline: 42px;/);
    // El viewport deja hueco para que la sombra de las tarjetas no se corte.
    assert.match(home, /\.home-pets-viewport \{[^}]*padding: 14px 14px 22px;/);
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    // Sombra de las tarjetas de mascota más ligera que la de los botones.
    assert.match(css, /\.pet-card \{[^}]*box-shadow: 0 8px 14px rgba\(0, 0, 0, 0\.12\);/);
    assert.match(css, /\.pet-card:hover \{[^}]*box-shadow: 0 12px 22px rgba\(0, 0, 0, 0\.16\);/);
});

test('flat home sections keep only the floating shadow and most buttons share it', () => {
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    // Sin degradados: secciones planas, solo sombra flotante por elemento.
    assert.match(css, /\.process-section \{\s*padding: 95px 0;\s*background: var\(--background\);\s*\}/);
    assert.doesNotMatch(css, /\.organization-container \{[^}]*linear-gradient/);
    assert.doesNotMatch(css, /\.organization-container \{[^}]*box-shadow/);
    assert.doesNotMatch(css, /\.organization-container \{[^}]*background:/);
    // La mayoría de botones comparte la sombra flotante (sombra suave, más fuerte al pasar el mouse).
    assert.match(css, /\.btn-primary,\s*\.primary-button,\s*\.search-button,\s*\.filter-button,\s*\.catalog-apply,\s*\.login-button,\s*\.pet-apply-button,\s*\.application-submit,\s*\.organization-button,\s*\.message-shelter-button,\s*\.dynamic-modal button\[type="submit"\] \{[^}]*box-shadow: var\(--shadow-sm\);/);
    assert.match(css, /\.secondary-button \{[^}]*box-shadow: var\(--shadow-sm\);/);
    assert.match(css, /\.button \{[^}]*box-shadow:var\(--shadow-sm\);/);
    assert.match(css, /\.organization-button \{[^}]*min-width: 300px;/);
    assert.match(css, /\.organization-button \{[^}]*min-height: 54px;/);
    assert.match(css, /\.organization-button \{[^}]*padding: 0 40px;/);
});

test('reference login aesthetic is the site-wide design language', () => {
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    // Los colores y fondos se quedan como estaban: blanco, nada azulado.
    assert.match(css, /--background: #F3FEFF;/);
    assert.match(css, /--text: #313434;/);
    assert.match(css, /--text-soft: #4A5556;/);
    assert.match(css, /--primary-soft: #F3FEFF;/);
    assert.match(css, /--primary-dark: #2C5357;/);
    assert.match(css, /--hero-title-dark: #12515A;/);
    assert.doesNotMatch(css, /--background: #EDF4F6;/);
    // Bordes casi invisibles. La sombra es la de Figma (Drop shadow 0 8 11 negro
    // 25%): la misma para botones, tarjetas y superficies, más fuerte al hover.
    assert.match(css, /--border: #DDE8EB;/);
    assert.match(css, /--shadow-sm: 0 8px 11px rgba\(0, 0, 0, 0\.25\);/);
    assert.match(css, /--shadow-md: 0 12px 20px rgba\(0, 0, 0, 0\.30\);/);
    assert.match(css, /--shadow-lg: 0 24px 48px rgba\(0, 0, 0, 0\.28\);/);
    assert.doesNotMatch(css, /--shadow-sm: 0 6px 20px rgba\(23, 61, 73/);
    // Las tarjetas de acción flotan también en reposo, no solo al pasar el mouse.
    assert.match(css, /\.action-card \{[^}]*box-shadow: var\(--shadow-sm\);/);
    assert.match(css, /\.action-card:hover \{[^}]*box-shadow: 0 12px 20px rgba\(0, 0, 0, 0\.30\);/);
    assert.doesNotMatch(css, /box-shadow: 0 18px 35px rgba\(18, 63, 75/);
    // Radios generosos como en la referencia.
    assert.match(css, /--radius-sm: 12px;/);
    assert.match(css, /--radius-md: 18px;/);
    assert.match(css, /--radius-lg: 26px;/);
    // La sombra es por botón y por tarjeta, nunca por bloque: la sección de
    // refugios vuelve a ser solo layout, sin fondo, borde ni sombra propios.
    assert.match(css, /\.organization-container \{\s*width: min\(var\(--max-width\), calc\(100% - 40px\)\);\s*margin: 0 auto;\s*display: grid;\s*grid-template-columns: 1fr 0\.8fr;\s*gap: 55px;\s*align-items: center;\s*\}/);
    assert.match(css, /\.button \{[^}]*box-shadow:var\(--shadow-sm\);/);
    const access = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    assert.match(access, /\.auth-card \{[^}]*border-radius:26px;/);
    assert.match(access, /\.auth-card \{[^}]*box-shadow:var\(--shadow-lg\);/);
    assert.match(access, /\.auth-submit \{[^}]*box-shadow:0 8px 11px rgba\(0,0,0,\.25\);/);
    assert.match(access, /\.auth-input-wrap input \{[^}]*border-radius:12px;/);
    assert.doesNotMatch(access, /box-shadow:0 2px 3px rgba\(18,63,75,\.12\)/);
    // Las vistas usan los tokens, no hex sueltos, para bordes y superficies pálidas.
    for (const view of ['HomeView.vue', 'ShelterView.vue', 'AdopterMessagesView.vue', 'AdopterProfileView.vue', 'HealthRecordsView.vue']) {
        const source = fs.readFileSync(path.join(projectRoot, 'src/views', view), 'utf8');
        assert.doesNotMatch(source, /#dcebed|#e8f6f5|#e7f6f5|#eaf7f6|#dff2f1/);
    }
});

test('auth redesign keeps real session flows, role payloads, validation and responsive layout', () => {
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    assert.match(access, /loginApi\(\{ email: loginForm\.value\.email\.trim\(\), password: loginForm\.value\.password \}\)/);
    assert.match(access, /register\(\{ fullName:/);
    assert.match(access, /role = registration\.value\.profile === 'shelter' \? 'SHELTER' : 'ADOPTER'/);
    assert.match(access, /registration\.value\.password !== registration\.value\.confirm/);
    assert.match(access, /reportValidity\(\)/);
    assert.match(access, /showLoginPassword/);
    assert.match(access, /showRegisterConfirm/);
    assert.match(access, /requestPasswordReset/);
    assert.match(access, /confirmPasswordReset/);
    assert.match(access, /¿Olvidaste tu contraseña\?/);
    assert.match(access, /Cambiar contraseña/);
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    assert.match(shelter, /api\('\/users\/me\/password'/);
    assert.match(shelter, /Cambiar contraseña/);
    assert.match(access, /:disabled="loading"/);
    assert.match(access, /mascotas-hero-v2\.png/);
    assert.doesNotMatch(access, /google|facebook|apple/i);
    assert.match(css, /grid-template-columns/);
    assert.match(css, /@media \(max-width:780px\)/);
    assert.match(css, /@media \(max-width:430px\)/);
});

test('password recovery uses expiring one-time tokens and does not store raw tokens', () => {
    const service = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/PasswordResetService.java'), 'utf8');
    const migration = fs.readFileSync(path.join(projectRoot, 'backend/src/main/resources/db/migration/V15__create_password_reset_tokens.sql'), 'utf8');
    const typeFixMigration = fs.readFileSync(path.join(projectRoot, 'backend/src/main/resources/db/migration/V16__align_password_reset_token_hash_type.sql'), 'utf8');
    const auth = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/AuthController.java'), 'utf8');
    assert.match(service, /SecureRandom/);
    assert.match(service, /MessageDigest\.getInstance\("SHA-256"\)/);
    assert.match(service, /invalidateForUser/);
    assert.match(service, /findActive/);
    assert.match(service, /token\.setUsedAt/);
    assert.match(service, /!properties\.isEnabled\(\) \|\| !properties\.isMailEnabled\(\)/);
    assert.match(auth, /password-reset\/request/);
    assert.match(auth, /password-reset\/confirm/);
    assert.match(migration, /expires_at TIMESTAMPTZ NOT NULL/);
    assert.match(migration, /used_at TIMESTAMPTZ/);
    assert.match(typeFixMigration, /ALTER COLUMN token_hash TYPE VARCHAR\(64\)/);
});

test('email delivery uses the Brevo HTTP API and verification can be resent', () => {
    const sender = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/BrevoEmailSender.java'), 'utf8');
    const brevoProperties = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/BrevoProperties.java'), 'utf8');
    const service = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/EmailVerificationService.java'), 'utf8');
    const auth = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/AuthController.java'), 'utf8');
    const filter = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/RateLimitFilter.java'), 'utf8');
    const client = fs.readFileSync(path.join(projectRoot, 'src/services/auth.js'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    assert.match(sender, /api-key/);
    assert.match(sender, /htmlContent/);
    assert.match(sender, /\\"name\\":\\"PetHop\\"/);
    assert.match(brevoProperties, /api\.brevo\.com\/v3\/smtp\/email/);
    assert.doesNotMatch(sender, /JavaMailSender|SimpleMailMessage/);
    assert.match(service, /void resend\(String email\)/);
    assert.match(auth, /email-verification\/resend/);
    assert.match(filter, /email-verification-resend/);
    assert.match(client, /email-verification\/resend/);
    assert.match(access, /Reenviar correo de verificación/);
    assert.match(access, /onMounted/);
    assert.match(access, /submitEmailVerification\(\);/);
    assert.match(access, /Cuenta creada\. Te enviamos un correo de confirmación/);
    assert.match(access, /carpeta de spam/);
});

test('follow-up dashboard uses real applications, history, adoptions, reports and media endpoints', () => {
    const source = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    assert.match(source, /api\('\/adoption-applications\/mine\?page=0&size=50'\)/);
    assert.match(source, /api\(`\/adoption-applications\/\$\{id\}\/history`\)/);
    assert.match(source, /api\('\/adoptions\/mine\?page=0&size=50'\)/);
    assert.match(source, /api\(`\/adoptions\/\$\{currentAdoption\.value\.id\}\/follow-ups`/);
    assert.match(source, /api\(`\/follow-ups\/\$\{report\.id\}\/media`/);
    assert.match(source, /applications\.value\.filter\(item => item\.status === 'UNDER_REVIEW'\)/);
    assert.match(source, /currentApplication\.value\?\.status === 'APPROVED'/);
    assert.match(source, /La organización recibió tu solicitud/);
    assert.match(source, /reportMediaSources/);
    assert.doesNotMatch(source, /backend|localhost:8080/i);
    assert.doesNotMatch(source, /Math\.random|fake|mock|demo/i);
});

test('Vue icons use Lucide paths from the locally vendored library with original navigation names', () => {
    const icon = fs.readFileSync(path.join(projectRoot, 'src/components/Icon.vue'), 'utf8');
    const iconNodes = fs.readFileSync(path.join(projectRoot, 'src/icons/lucide-icons.js'), 'utf8');
    const originalVendor = fs.readFileSync(path.join(projectRoot, 'js/vendor/lucide.min.js'), 'utf8');
    const packageJson = JSON.parse(fs.readFileSync(path.join(projectRoot, 'package.json'), 'utf8'));
    assert.ok(originalVendor.includes('lucide v1.52.0'));
    assert.ok(originalVendor.includes('createIcons'));
    assert.ok(icon.includes("'stroke-width': props.strokeWidth") || icon.includes(':stroke-width="strokeWidth"'));
    for (const name of ['house','search','hand-heart','paw-print','heart','layout-dashboard','clipboard-list','messages-square','route','settings-2','circle-help','building-2','user-round']) {
        assert.ok(iconNodes.includes(`"${name}":[`), `local Lucide data includes ${name}`);
    }
    assert.ok(!packageJson.dependencies['lucide-vue-next'], 'the app does not add a second icon dependency');
});

test('favorite states use the requested teal Lucide heart and cannot bubble into pet navigation', () => {
    const card = fs.readFileSync(path.join(projectRoot, 'src/components/PetCard.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    assert.match(card, /:class="\{ active: favorite \}"/);
    assert.match(card, /:filled="favorite"/);
    assert.match(card, /@click\.stop="emit\('favorite', pet\.id\)"/);
    assert.match(home, /:class="\{ active: favorites\.includes/);
    assert.match(pet, /:class="\{ active:favorites\.includes\(pet\.id\) \}"/);
    assert.match(css, /\.favorite\.active,[\s\S]*?color:\s*#3EB2BA/);
    assert.match(css, /\.favorite:not\(\.active\),[\s\S]*?color:\s*#647174/);
});

test('shelter dashboard restores the original overview composition and accessible responsive navigation', () => {
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    for (const label of ['Inicio','Mis mascotas','Publicar mascota','Solicitudes','En proceso','Completadas','Mensajes','Seguimiento','Perfil / Organización','Configuración','Ayuda']) assert.ok(shelter.includes(`label: '${label}'`));
    for (const marker of ['overviewStats','recentRequests','shelter-home-grid','shelter-journey','recentPets','shelter-sidebar-backdrop','aria-controls="shelter-sidebar"','Volver a PetHop']) assert.ok(shelter.includes(marker), `shelter view includes ${marker}`);
    assert.match(shelter, /item\.icon/);
    assert.match(home, /Registrar refugio/);
    assert.match(css, /\.shelter-sidebar-backdrop/);
    assert.match(css, /white-space:\s*nowrap/);
    assert.doesNotMatch(css, /shelter-dashboard-active \.shelter-sidebar nav\s*\{\s*display:\s*flex/);
});

test('audit batch 1: failed images keep their name, focus is solid, links are underlined and admin fields are labelled', () => {
    const skeleton = fs.readFileSync(path.join(projectRoot, 'src/components/ImageWithSkeleton.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const footer = fs.readFileSync(path.join(projectRoot, 'src/components/SiteFooter.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    assert.match(skeleton, /:role="role \|\| \(failed \? 'img' : undefined\)"/);
    assert.match(skeleton, /:aria-label="role === 'img' \|\| failed \? alt : undefined"/);
    assert.match(css, /outline: 3px solid #167F87;/);
    assert.match(css, /\[tabindex\]:focus-visible/);
    assert.doesNotMatch(css, /outline: ?0;/);
    assert.match(css, /p a,[\s\S]*?\.footer-column a \{[\s\S]*?text-decoration: underline;/);
    assert.match(home, /<strong>Registrar refugio<\/strong>/);
    assert.doesNotMatch(home, /Conocer más/);
    assert.match(home, /<ol[\s\S]*?ref="processTrack"/);
    assert.match(home, /<li[\s\S]*?v-for="\(slide, slidePosition\) in processCarouselSlides"/);
    assert.match(footer, /Escríbenos: pethop13@gmail\.com/);
    assert.doesNotMatch(footer, />Contacto</);
    assert.match(admin, /<label class="admin-search-label">Buscar usuario/);
    for (const label of ['Nombre del comercio \\*', 'Categoría \\*', 'Ubicación', 'Sitio web', 'Beneficio', 'Código', 'Descripción']) {
        assert.match(admin, new RegExp(`<label[^>]*>${label}<`));
    }
});
