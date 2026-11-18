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
    assert.match(home, /\.process-steps \.process-step \{[^}]*border-radius: var\(--radius-lg\);/);
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
    assert.match(css, /\.primary-button,\s*\.search-button,\s*\.filter-button,\s*\.catalog-apply,\s*\.login-button,\s*\.pet-apply-button,\s*\.application-submit,\s*\.organization-button,\s*\.message-shelter-button,\s*\.dynamic-modal button\[type="submit"\] \{[^}]*box-shadow: var\(--shadow-sm\);/);
    assert.match(css, /\.secondary-button \{[^}]*box-shadow: var\(--shadow-sm\);/);
    assert.match(css, /\.button \{[^}]*box-shadow:var\(--shadow-sm\);/);
    // El botón "Registrar refugio" mide 300 px mínimo en escritorio; a 320 px
    // no sobresale porque la media query de 700 px lo pone a ancho completo.
    assert.match(css, /\.organization-button \{[^}]*min-width: 300px;/);
    assert.match(css, /\.organization-button \{\s*min-width: 0;\s*width: 100%;\s*\}/);
    assert.match(css, /\.organization-button \{[^}]*min-height: 54px;/);
    assert.match(css, /\.organization-button \{[^}]*padding: 0 40px;/);
});

test('reference login aesthetic is the site-wide design language', () => {
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    // Los colores y fondos se quedan como estaban: blanco, nada azulado.
    assert.match(css, /--background: #FFFFFF;/);
    assert.match(css, /--text: #313434;/);
    assert.match(css, /--text-soft: #4A5556;/);
    assert.match(css, /--primary-soft: #FFFFFF;/);
    assert.match(css, /--primary-dark: #2C5357;/);
    assert.match(css, /--hero-title-dark: #2C5357;/);
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
    assert.match(access, /\.auth-card \{[^}]*border-radius: var\(--radius-lg\);/);
    assert.match(access, /\.auth-card \{[^}]*box-shadow:var\(--shadow-lg\);/);
    assert.match(access, /\.auth-submit \{[^}]*box-shadow:0 8px 11px rgba\(0,0,0,\.25\);/);
    assert.match(access, /\.auth-input-wrap input \{[^}]*border-radius: var\(--radius-sm\);/);
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
    assert.match(access, /mascotas-hero-v2\.webp/);
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
    assert.match(css, /\.favorite\.active,[\s\S]*?color:\s*var\(--primary-strong\)/);
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
    assert.match(css, /outline: 3px solid var\(--primary-strong\);/);
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

test('audit batch 2: filters live in the URL, pages have unique titles, skip link and focus trap', () => {
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const router = fs.readFileSync(path.join(projectRoot, 'src/router/index.js'), 'utf8');
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const trap = fs.readFileSync(path.join(projectRoot, 'src/services/dialogFocus.js'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    for (const view of ['AccessView', 'AdminView', 'AdopterMessagesView', 'AdopterProfileView', 'AdoptionView', 'AffiliatesView', 'ExploreView', 'FollowUpView', 'HealthRecordsView', 'HomeView', 'PetView', 'ShelterView']) {
        const source = fs.readFileSync(path.join(projectRoot, 'src/views', `${view}.vue`), 'utf8');
        assert.ok(source.includes('id="main-content"'), `${view} has a main-content landmark`);
    }
    assert.match(explore, /species: String\(route\.query\.species \|\| ''\)/);
    assert.match(explore, /function filterQuery\(\)/);
    assert.match(explore, /function syncFromQuery\(\)/);
    assert.match(explore, /watch\(\(\) => route\.query, \(\) => \{ if \(syncFromQuery\(\)\) loadPets\(\); \}\)/);
    assert.match(explore, /router\.replace\(\{ query: filterQuery\(\) \}\)/);
    assert.match(router, /title: 'Aliados \| PetHop'/);
    assert.doesNotMatch(router, /Aliados PetHop \| PetHop/);
    assert.match(router, /if \(to\.path !== from\.path\) \{[\s\S]*?main\.focus\(\{ preventScroll: true \}\)/);
    assert.match(pet, /document\.title = 'Cargando mascota \| PetHop'/);
    assert.match(pet, /document\.title = 'Mascota no encontrada \| PetHop'/);
    assert.match(trap, /export function useFocusTrap\(dialogRef, isOpen, onClose\)/);
    assert.match(trap, /event\.key === 'Escape'/);
    assert.match(trap, /event\.key !== 'Tab'/);
    assert.match(css, /\.skip-link \{[\s\S]*?position: absolute;/);
    assert.match(css, /\.skip-link:focus \{[\s\S]*?top: 10px;/);
    assert.match(header, /<a class="skip-link" href="#main-content">Saltar al contenido<\/a>/);
    assert.match(header, /useFocusTrap\(favoritesDialog, favoritesOpen/);
    assert.match(header, /useFocusTrap\(notificationsDialog, notificationsOpen/);
    assert.match(admin, /<a class="skip-link" href="#main-content">Saltar al contenido<\/a>/);
    assert.match(admin, /useFocusTrap\(reviewDialog, \(\) => !!selected\.value/);
    assert.match(shelter, /useFocusTrap\(petDialog, \(\) => !!selectedPet\.value/);
    assert.match(shelter, /useFocusTrap\(requestDialog, \(\) => !!selectedRequest\.value/);
    assert.match(profile, /useFocusTrap\(detailDialog, \(\) => !!selectedActivity\.value/);
    assert.match(access, /<a class="skip-link" href="#main-content">Saltar al contenido<\/a>/);
    for (const [file, title] of [['acceso.html', 'Ingresar | PetHop'], ['explorar.html', 'Explorar mascotas | PetHop'], ['inicio.html', 'PetHop | Adopta, conecta, cambia vidas'], ['mascota.html', 'Mascota | PetHop'], ['refugio.html', 'Dar en adopción | PetHop'], ['seguimiento.html', 'Seguimiento | PetHop'], ['solicitud.html', 'Solicitud de adopción | PetHop'], ['aliados.html', 'Aliados | PetHop']]) {
        const html = fs.readFileSync(path.join(projectRoot, file), 'utf8');
        assert.ok(html.includes(`<title>${title}</title>`), `${file} titles ${title}`);
    }
});

test('audit batch 3: verification gate, confirmations, visible visit errors and linked form errors', () => {
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const follow = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const confirm = fs.readFileSync(path.join(projectRoot, 'src/components/ConfirmDialog.vue'), 'utf8');
    const moderation = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/pet/PetModerationService.java'), 'utf8');
    const action = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/pet/ModerationAction.java'), 'utf8');
    assert.match(confirm, /useFocusTrap\(dialog, \(\) => props\.open/);
    assert.match(confirm, /@click="emit\('confirm'\)"/);
    assert.match(confirm, /@click="emit\('cancel'\)"/);
    assert.match(shelter, /shelter-verify-notice/);
    assert.match(shelter, /Tu perfil está pendiente de verificación/);
    assert.match(shelter, /:disabled="publishing \|\| \(state\.step === 6 && state\.profile\.verificationStatus !== 'VERIFIED'\)"/);
    assert.match(shelter, /if \(state\.profile\.verificationStatus !== 'VERIFIED'\) \{ toast\('Tu perfil debe estar verificado para publicar\.', true\); return; \}/);
    assert.match(shelter, /confirmRequest\.value = \{ action, request, title:/);
    assert.match(shelter, /async function runRequestAction\(action, request\)/);
    assert.match(shelter, /async function runFormalizeAdoption\(request\)/);
    assert.match(shelter, /<ConfirmDialog :open="!!confirmRequest"/);
    assert.match(admin, /function requestDecide\(action\)/);
    assert.match(admin, /@click="requestDecide\('APPROVE'\)"/);
    assert.match(admin, /<ConfirmDialog :open="confirmApprove"/);
    assert.match(admin, /@click="decide\('REOPEN'\)">Volver a poner en revisión/);
    assert.match(action, /REOPEN/);
    assert.match(moderation, /case REOPEN -> ModerationStatus\.PENDING;/);
    assert.match(moderation, /Solo se puede devolver a revisión una publicación ya decidida/);
    assert.match(follow, /const visitError = ref\(''\); const visitNotice = ref\(''\); const visitFieldError = ref\(false\)/);
    assert.match(follow, /<p v-if="visitError" id="visit-error" class="inline-error" role="alert">/);
    assert.match(follow, /@click="saveVisit">Reintentar<\/button>/);
    assert.match(follow, /:aria-invalid="visitFieldError && !visitForm\.date"/);
    assert.match(follow, /:aria-describedby="visitFieldError \? 'visit-error' : undefined"/);
    assert.match(follow, /id="report-error"/);
    assert.match(follow, /:aria-invalid="reportFieldError && !form\.status"/);
    assert.match(access, /id="login-feedback"/);
    assert.match(access, /id="register-feedback"/);
    assert.match(access, /:aria-invalid="feedback\.loginError"/);
    assert.match(access, /:aria-describedby="feedback\.loginError \? 'login-feedback' : undefined"/);
    assert.match(access, /:aria-invalid="feedback\.registerError && feedback\.register === 'Las contraseñas no coinciden\.'"/);
});

test('audit batch 4: withdraw, review context, last message, role-aware messages, notification targets, health edit', () => {
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const follow = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const messages = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const health = fs.readFileSync(path.join(projectRoot, 'src/views/HealthRecordsView.vue'), 'utf8');
    const conversation = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/messaging/dto/ConversationResponse.java'), 'utf8');
    const messageRepo = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/messaging/MessageRepository.java'), 'utf8');
    const healthController = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/health/HealthRecordController.java'), 'utf8');
    const scheduler = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/notification/HealthReminderScheduler.java'), 'utf8');
    const adoptionService = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/adoption/AdoptionService.java'), 'utf8');
    const followUpService = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/followup/FollowUpService.java'), 'utf8');
    const petResponse = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/pet/dto/PetResponse.java'), 'utf8');
    assert.match(header, /label: 'Mensajes', to: currentUser\.value\?\.role === 'SHELTER' \? '\/refugio\.html\?section=mensajes' : '\/mensajes\.html'/);
    assert.match(header, /case 'HEALTH_REMINDER': return refId \? `\/salud\.html\?pet=\$\{refId\}` : '\/salud\.html';/);
    assert.match(header, /case 'ADOPTION_FORMALIZED':[\s\S]*?return refId \? `\/seguimiento\.html\?application=\$\{refId\}` : '\/seguimiento\.html';/);
    assert.match(header, /case 'NEW_MESSAGE': return currentUser\.value\?\.role === 'SHELTER' \? '\/refugio\.html\?section=mensajes'/);
    assert.match(conversation, /String lastMessage,Instant lastMessageAt,long unreadCount/);
    assert.match(messageRepo, /findFirstByConversationIdOrderByCreatedAtDesc/);
    assert.match(messageRepo, /countByConversationIdAndReadAtIsNullAndSender_IdNot/);
    assert.match(messages, /thread\.lastMessage \|\| thread\.messages\.at\(-1\)\?\.text/);
    assert.match(messages, /adopter-unread-badge/);
    assert.match(messages, /thread\.unreadCount = 0;/);
    assert.match(messages, /currentUser\.value\?\.role === 'SHELTER' \? thread\?\.otherParticipant/);
    assert.match(shelter, /thread\.lastMessage \|\| thread\.messages\.at\(-1\)\?\.text/);
    assert.match(shelter, /shelter-unread-badge/);
    assert.match(shelter, /thread\.unreadCount=0;persist\(\)/);
    assert.match(follow, /Retirar solicitud/);
    assert.match(follow, /status: 'WITHDRAWN'/);
    assert.match(follow, /<ConfirmDialog :open="confirmWithdraw"/);
    assert.match(profile, /Ver en Seguimiento/);
    assert.match(profile, /Retirar solicitud/);
    assert.match(profile, /async function withdrawActivity/);
    assert.match(admin, /shelterVerificationStatus/);
    assert.match(admin, /shelterContactEmail/);
    assert.match(admin, /shelterOtherPets/);
    assert.doesNotMatch(admin, /no están disponibles en este endpoint/);
    assert.match(admin, /function verificationLabel\(status\)/);
    assert.match(petResponse, /fromForReview/);
    assert.match(petResponse, /ShelterPetSummary/);
    assert.match(scheduler, /"PET", record\.getPet\(\)\.getId\(\)/);
    assert.match(adoptionService, /"APPLICATION",app\.getId\(\)/);
    assert.match(followUpService, /"APPLICATION",adoption\.getApplication\(\)\.getId\(\)/);
    assert.match(health, /health-error-actions/);
    assert.match(health, /Ir a Seguimiento/);
    assert.match(health, /startEdit\(record\)/);
    assert.match(health, /confirmDeleteId = record\.id/);
    assert.match(health, /method: 'PUT', body \}/);
    assert.match(health, /method: 'DELETE'/);
    assert.match(health, /Editar registro/);
    assert.match(healthController, /@PutMapping\("\/\{recordId\}"\)/);
    assert.match(healthController, /@DeleteMapping\("\/\{recordId\}"\)/);
});

test('audit batch 5: required markers, format rules, aria-current/pressed, carousel tabpanel and pause', () => {
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const petApi = fs.readFileSync(path.join(projectRoot, 'src/services/petApi.js'), 'utf8');
    const petForm = fs.readFileSync(path.join(projectRoot, 'src/services/petForm.js'), 'utf8');
    const accesoCss = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    const stylesCss = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    assert.match(access, /auth-required.*\* Obligatorio/);
    assert.match(access, /<p class="auth-requirement">Tu contraseña debe tener entre 12 y 72 caracteres\.<\/p>/);
    assert.match(access, /Formato válido: números y los caracteres \+ \( \) - \. ; entre 7 y 32 caracteres\./);
    assert.match(access, /reset-password[\s\S]{0,400}Tu contraseña debe tener entre 12 y 72 caracteres/);
    assert.match(accesoCss, /\.auth-required/);
    assert.match(profile, /adopter-required.*\* Obligatorio/);
    assert.match(profile, /Tu contraseña debe tener entre 12 y 72 caracteres/);
    assert.match(profile, /Formato válido: números y los caracteres \+ \( \) - \. ; entre 7 y 32 caracteres\./);
    assert.match(shelter, /function isStepFieldRequired\(field\)/);
    assert.match(shelter, /shelter-required.*\* Obligatorio/);
    assert.match(shelter, /Tu contraseña debe tener entre 12 y 72 caracteres/);
    assert.match(shelter, /:aria-current="state\.step===index \? 'step' : undefined"/);
    assert.match(shelter, /:aria-pressed="petFilter===filter"/);
    assert.match(shelter, /:aria-pressed="requestFilter===filter"/);
    assert.match(stylesCss, /\.shelter-required/);
    assert.match(explore, /value="Cachorro">Cachorro \(hasta 1 año\)/);
    assert.match(explore, /value="Joven">Joven \(1 a 3 años\)/);
    assert.match(explore, /value="Adulto">Adulto \(3 a 8 años\)/);
    assert.match(explore, /value="Mayor">Mayor \(más de 8 años\)/);
    assert.match(explore, /cachorro: 0, joven: 1, adulto: 2, mayor: 3/);
    assert.doesNotMatch(explore, /<option>Senior<\/option>/);
    assert.match(petApi, /SENIOR: 'Mayor'/);
    assert.match(petForm, /Mayor: 'SENIOR'/);
    assert.doesNotMatch(petApi, /'Senior'/);
    assert.match(header, /:aria-current="isNavActive\(item\) \? 'page' : undefined"/);
    assert.match(header, /:aria-current="route\.path === accountRoute \? 'page' : undefined"/);
    assert.match(header, /:aria-current="route\.path === '\/admin\.html' \? 'page' : undefined"/);
    assert.match(home, /id="process-carousel-panel"/);
    assert.match(home, /role="tabpanel"/);
    assert.match(home, /:aria-controls="'process-carousel-panel'"/);
    assert.match(home, /home-pets-pause/);
    assert.match(home, /:aria-pressed="petAutoplayPaused"/);
    assert.match(home, /togglePetAutoplay/);
    assert.match(home, /petAutoplayPaused\.value/);
});

test('audit batch M1: contrast, wrapping, 320px, accessible names, nav, live roles, home loading and counter', () => {
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const accesoCss = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const footer = fs.readFileSync(path.join(projectRoot, 'src/components/SiteFooter.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const messages = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    assert.match(styles, /--hero-title-light: #3EB2BA;/);
    assert.match(styles, /\.progress-stage\.is-future,\.progress-stage\.is-future strong \{ color:var\(--text-soft\); \}/);
    assert.match(styles, /\.progress-stage\.is-future small \{ color:var\(--text-soft\); \}/);
    assert.match(styles, /\.application-item-copy strong \{ font-size:\.8rem; \}/);
    assert.match(styles, /\.pet-follow-facts dd \{ margin-top:3px; color:var\(--text\); font-size:\.75rem; font-weight:700; \}/);
    assert.match(styles, /\.progress-stage small \{ max-width:112px; color:var\(--text-soft\); font-size:\.75rem;/);
    assert.match(styles, /\.formalized-badge \{ font-size:\.75rem!important; \}/);
    assert.doesNotMatch(styles, /overflow-x: clip;/);
    // Sin anchos mínimos fijos que rompan el reflow a 320 px: el único
    // `min-width: 300px` permitido es el del botón "Registrar refugio",
    // que la media query de 700 px pone a ancho completo.
    assert.equal((styles.match(/min-width: 300px;/g) || []).length, 1, 'only the organization button may use a 300px min-width');
    assert.match(styles, /\.organization-button \{[^}]*min-width: 300px;/);
    assert.match(styles, /\.shelter-toast-close/);
    assert.match(admin, /\.admin-breadcrumb strong \{ color:var\(--text-soft\); \}/);
    assert.match(admin, /import SiteHeader from '\.\.\/components\/SiteHeader\.vue';/);
    assert.match(admin, /<SiteHeader \/>/);
    assert.match(admin, /\.admin-topbar \{ position:static;/);
    assert.match(header, /Abrir notificaciones, \$\{unreadNotifications\} sin leer/);
    assert.match(header, /Seguimiento: solicitudes recibidas por tu refugio/);
    assert.match(header, /<span class="favorites-count" aria-live="polite">/);
    assert.match(header, /\.account-display-name \{ max-width:170px; color:var\(--text\);/);
    assert.match(footer, /<h2>Ayuda<\/h2><RouterLink to="\/aliados\.html">Aliados<\/RouterLink><RouterLink to="\/acceso\.html">Ingresar<\/RouterLink>/);
    assert.doesNotMatch(footer, /Iniciar sesión/);
    assert.match(access, /auth-back-link/);
    assert.match(access, />Ingresar<\/button>/);
    assert.doesNotMatch(access, /Iniciar sesión/);
    assert.match(access, /:role="feedback\.loginError \? 'alert' : 'status'"/);
    assert.match(access, /:role="feedback\.resendError \? 'alert' : 'status'"/);
    assert.match(access, /:role="feedback\.registerError \? 'alert' : 'status'"/);
    assert.match(accesoCss, /\.auth-back-link/);
    assert.match(shelter, /Marcar en revisión/);
    assert.doesNotMatch(shelter, /Continuar revisión/);
    assert.match(shelter, /function toast\(message, isError = false\)/);
    assert.match(shelter, /noticeIsError/);
    assert.match(shelter, /shelter-toast-close/);
    assert.match(shelter, /:role="noticeIsError \? 'alert' : 'status'"/);
    assert.match(shelter, /toast\('Tu perfil debe estar verificado para publicar\.', true\)/);
    assert.match(profile, /feedbackIsError/);
    assert.match(profile, /:role="feedbackIsError \? 'alert' : 'status'"/);
    assert.match(profile, /:role="passwordFeedbackIsError \? 'alert' : 'status'"/);
    assert.match(profile, /:role="roleFeedbackIsError \? 'alert' : 'status'"/);
    assert.match(home, /petsLoading/);
    assert.match(home, /home-pets-loading/);
    assert.match(home, /catalog-loading-card/);
    assert.match(home, /home-pets-counter/);
    assert.match(home, /Grupo \{\{ currentPetPage \+ 1 \}\} de \{\{ petPages\.length \}\}/);
    assert.match(messages, /:aria-label="`Opciones de conversación sobre \$\{thread\.petName\}`"/);
    assert.match(messages, /<span aria-hidden="true">•••<\/span>/);
});

test('audit batch M2: filters, sort label, pet ghost card, unified favorites, adoption form, retry state', () => {
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const adoption = fs.readFileSync(path.join(projectRoot, 'src/views/AdoptionView.vue'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    assert.match(explore, /@change="applyFilters"/);
    assert.doesNotMatch(explore, /Aplicar filtros/);
    assert.match(explore, /Más recientes/);
    assert.doesNotMatch(explore, /Recomendadas/);
    assert.match(explore, /favoriteNotice/);
    assert.match(explore, /catalog-favorite-notice/);
    assert.match(explore, /retrying/);
    assert.match(explore, /Reintentando…/);
    assert.match(pet, /const pet = ref\(null\);/);
    assert.match(pet, /petLoading/);
    assert.match(pet, /pet-profile-loading/);
    assert.match(pet, /pet-profile-error-wrap/);
    assert.match(pet, /Volver a explorar/);
    assert.match(pet, /pet-profile-favorite-notice/);
    assert.doesNotMatch(pet, /name: 'Mascota'/);
    assert.match(home, /favoriteNotice/);
    assert.match(home, /home-favorite-notice/);
    assert.match(home, /petsRetrying/);
    assert.match(home, /retryPets/);
    assert.doesNotMatch(home, /router\.push\(\{ path: '\/acceso\.html', query: \{ redirect: '\/inicio\.html' \} \}\)/);
    assert.match(adoption, /petLoadError/);
    assert.match(adoption, /No pudimos cargar esta mascota/);
    assert.match(adoption, /Marca la casilla de arriba para poder enviar la solicitud\./);
    assert.match(adoption, /Actualiza tu teléfono en tu perfil<\/RouterLink>/);
    assert.match(adoption, /to="\/perfil\.html" target="_blank"/);
    assert.match(adoption, /documentNotice/);
    assert.match(adoption, /Solo se tomaron los primeros 5 archivos/);
    assert.match(adoption, /supera los 10 MB y no se agregó/);
    assert.match(adoption, /el documento \$\{item\.file\.name\} no pudo cargarse/);
    assert.match(header, /favoritesLoginRequired/);
    assert.match(header, /favorites-login-link/);
    assert.match(styles, /\.catalog-favorite-notice/);
    assert.match(styles, /\.pet-profile-loading/);
    assert.match(styles, /\.pet-profile-error-wrap/);
    assert.match(styles, /\.application-submit-hint/);
    assert.match(styles, /\.form-field-link/);
    assert.match(styles, /\.form-field-notice/);
});

test('audit batch M3: publish review notice, publishing state, draft note, photo selection, review filter, follow-up detail error and folio', () => {
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const followUp = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const register = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/dto/RegisterRequest.java'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/user/dto/UpdateProfileRequest.java'), 'utf8');
    const phoneMessage = 'El teléfono no tiene un formato válido. Usa solo dígitos y, si quieres, + ( ) - . , entre 7 y 32 caracteres.';
    assert.ok(access.includes(phoneMessage));
    assert.ok(register.includes(phoneMessage));
    assert.ok(profile.includes(phoneMessage));
    assert.match(shelter, /reviewNotice/);
    assert.match(shelter, /Marca la casilla para confirmar que la información es correcta antes de publicar\./);
    assert.match(shelter, /publishing/);
    assert.match(shelter, /Publicando…/);
    assert.match(shelter, /finally \{ publishing\.value = false; \}/);
    assert.match(shelter, /draftSavedAt/);
    assert.match(shelter, /Borrador guardado automáticamente/);
    assert.match(shelter, /Al publicar, tu mascota pasará por revisión antes de aparecer en el catálogo\./);
    assert.match(shelter, /supera los 10 MB y no se agregó/);
    assert.match(shelter, /no es una imagen y no se agregó/);
    assert.match(shelter, /Solo caben/);
    assert.match(shelter, /item\.moderationStatus === 'PENDING'/);
    assert.match(shelter, /status-en-revision">En revisión<\/span>/);
    assert.match(followUp, /selectionError/);
    assert.match(followUp, /@click="loadSelected\(selectedId\)">Reintentar<\/button>/);
    assert.match(followUp, /#\{\{ currentApplication\.id\.slice\(0, 8\) \}\}/);
    assert.doesNotMatch(followUp, /<dd>\{\{ currentApplication\.id \}\}<\/dd>/);
    assert.match(styles, /\.shelter-draft-saved/);
    assert.match(styles, /\.shelter-review-note/);
    assert.match(styles, /\.shelter-review-notice/);
});

test('audit batch M4: follow-up stages and reports, messages search, day separators, mobile chat and profile space', () => {
    const followUp = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const messages = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    assert.match(followUp, /key: 'RECEIVED', title: 'Recepción'[^}]*date: undefined/);
    assert.match(followUp, /Reportes de esta solicitud/);
    assert.doesNotMatch(followUp, /Reportes en selección/);
    assert.match(followUp, /item\.shelterName \|\| 'Refugio no disponible'/);
    assert.match(followUp, /showAllReports/);
    assert.match(followUp, /Ver todos los reportes/);
    assert.match(followUp, /Ver menos reportes/);
    assert.match(messages, /groupedMessages/);
    assert.match(messages, /dayLabel/);
    assert.match(messages, /formatFullDate/);
    assert.match(messages, /:title="formatFullDate\(message\.createdAt \|\| message\.date\)"/);
    assert.match(messages, /thread\.pet\?\.shelterName/);
    assert.match(messages, /thread\.otherParticipant/);
    assert.match(messages, /Buscar por mascota o refugio/);
    assert.match(messages, /wideLayout/);
    assert.match(messages, /min-width: 761px/);
    assert.match(messages, /wideLayout\.value \? conversations\.value\[0\] : null/);
    assert.match(messages, /messages-login-link/);
    assert.match(messages, /Ingresar<\/RouterLink>/);
    assert.match(profile, /Tu espacio de refugio/);
    assert.match(profile, /Ir a mi espacio de refugio/);
    assert.match(profile, /Ir a Administración/);
    assert.match(profile, /:to="isShelter \? '\/refugio\.html' : '\/administracion\.html'"/);
});

test('audit batch M5: admin labels, reason dialog, queue pagination and filters, review navigation, section errors, health record back and login', () => {
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const health = fs.readFileSync(path.join(projectRoot, 'src/views/HealthRecordsView.vue'), 'utf8');
    assert.match(admin, /roleLabel/);
    assert.match(admin, /accountStatusLabel/);
    assert.match(admin, /speciesLabel\(pet\.species\)/);
    assert.match(admin, /adoptionStatusLabel\(pet\.adoptionStatus\)/);
    assert.match(admin, /moderationActionLabel\(item\.action\)/);
    assert.match(admin, /reportStatusLabel\(item\.status\)/);
    assert.match(admin, /reportCategoryLabel\(item\.category\)/);
    assert.match(admin, /auditActionLabel\(item\.action\)/);
    assert.match(admin, /entityTypeLabel\(item\.entityType\)/);
    assert.match(admin, /verificationLabel\(item\.verificationStatus\)/);
    assert.doesNotMatch(admin, /window\.prompt\(/);
    assert.match(admin, /reasonDialog/);
    assert.match(admin, /Acción cancelada\. No se hizo ningún cambio\./);
    assert.match(admin, /queueTotalPages/);
    assert.match(admin, /filteredQueue/);
    assert.match(admin, /queueSpecies/);
    assert.match(admin, /queueShelter/);
    assert.match(admin, /queueDate/);
    assert.match(admin, /Página \{\{ queuePage \+ 1 \}\} de \{\{ queueTotalPages \}\}/);
    assert.match(admin, /stepReview/);
    assert.match(admin, /Anterior<\/button>/);
    assert.match(admin, /Siguiente →<\/button>/);
    assert.match(admin, /sectionErrors/);
    assert.match(admin, /queueError/);
    assert.match(admin, /affiliatesError/);
    assert.match(admin, /Mostrando \{\{ users\.length \}\} de \{\{ listTotals\.users \}\} usuarios\./);
    assert.match(health, /goBack/);
    assert.match(health, /@click="goBack"/);
    assert.doesNotMatch(health, /@click="router\.back\(\)"/);
    assert.match(health, /El refugio de \{\{ pet\?\.shelterName \|\| pet\?\.name \}\} agrega los registros; pregúntale por/);
    assert.match(health, /to="\/mensajes\.html">mensajes<\/RouterLink>/);
    assert.match(health, /errorIsLogin/);
    assert.match(health, /health-login-link/);
});

test('audit batch B1: gallery counter, hidden empty facts, favorites title, unified search, reset and verify link failures, other pets fields, application skeleton', () => {
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const adoption = fs.readFileSync(path.join(projectRoot, 'src/views/AdoptionView.vue'), 'utf8');
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    assert.match(pet, /gallery-counter/);
    assert.match(pet, /\{\{ carouselSlides\[centerTrackIndex\]\?\.sourceIndex \+ 1 \}\} de \{\{ photos\.length \}\}/);
    assert.match(styles, /\.gallery-counter/);
    assert.match(pet, /generalInformation = computed\(\(\) => \[[\s\S]*?\.filter\(\(\[, value\]\) => value\)\);/);
    assert.doesNotMatch(pet, /No proporcionado/);
    assert.match(explore, /\{\{ favoritesOnly \? 'Tus favoritos' : 'Mascotas disponibles' \}\}/);
    assert.match(home, /placeholder="Buscar por nombre, especie o ubicación"/);
    assert.match(explore, /placeholder="Buscar por nombre, especie o ubicación"/);
    assert.match(access, /resetLinkFailed/);
    assert.match(access, /Solicitar un nuevo enlace/);
    assert.match(access, /to="\/acceso\.html\?mode=reset">Solicitar un nuevo enlace<\/RouterLink>/);
    assert.match(access, /verifyLinkFailed/);
    assert.match(access, /Ese enlace ya no es válido\. Pide uno nuevo abajo\./);
    assert.match(access, /:disabled="loading \|\| verifyLinkFailed"/);
    assert.match(adoption, /v-if="form\.otherPets === 'si'"/);
    assert.match(adoption, /:required="form\.otherPets === 'si'"/);
    assert.match(adoption, /watch\(\(\) => form\.otherPets/);
    assert.match(adoption, /petLoading/);
    assert.match(adoption, /v-if="petLoading" class="application-pet-card" role="status" aria-label="Cargando mascota"/);
    assert.doesNotMatch(adoption, /vaccination: 'Pendiente de confirmar'/);
});

test('audit batch B2: health record field errors, locked shelter sections, chat send guard, profile cancel confirm, public profile hints, report photo cap, progress note by status', () => {
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const follow = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    assert.match(shelter, /healthRecordErrors/);
    assert.match(shelter, /:aria-invalid="healthRecordErrors\.title"/);
    assert.match(shelter, /:aria-invalid="healthRecordErrors\.performedAt"/);
    assert.match(shelter, /is-invalid/);
    assert.match(shelter, /is-locked/);
    assert.match(shelter, /Bloqueada/);
    assert.match(shelter, /Completa el perfil de tu organización para habilitar el panel\./);
    assert.match(shelter, /:disabled="!profileExists && item\.label !== 'Perfil \/ Organización'"/);
    assert.match(shelter, /sendingMessage/);
    assert.match(shelter, /:disabled="sendingMessage"/);
    assert.match(shelter, /Enviando…/);
    assert.match(shelter, /confirmCancelProfile/);
    assert.match(shelter, /profileHasChanges/);
    assert.match(shelter, /¿Descartar los cambios\?/);
    assert.match(shelter, /Estos datos se muestran públicamente a las familias adoptantes\./);
    assert.match(shelter, /placeholder="Ej\.: 77500"/);
    assert.match(shelter, /placeholder="Ej\.: \+52 998 123 4567"/);
    assert.match(styles, /\.shelter-nav-item\.is-locked/);
    assert.match(styles, /\.shelter-nav-lock-note/);
    assert.match(follow, /uploadNotice/);
    assert.match(follow, /Se adjuntaron las primeras 6 fotografías\./);
    assert.match(follow, /progressNote/);
    assert.match(follow, /El refugio no aprobó esta solicitud\. El proceso terminó aquí\./);
    assert.match(follow, /Retiraste esta solicitud\. El proceso terminó aquí\./);
});

test('audit batch B3: clickable admin metrics, shelter in profile activity, health header only for editors, assistant subtitle, already-done lows verified', () => {
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const health = fs.readFileSync(path.join(projectRoot, 'src/views/HealthRecordsView.vue'), 'utf8');
    const assistant = fs.readFileSync(path.join(projectRoot, 'src/components/AssistantChat.vue'), 'utf8');
    const footer = fs.readFileSync(path.join(projectRoot, 'src/components/SiteFooter.vue'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const adoption = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/adoption/dto/AdoptionResponse.java'), 'utf8');
    assert.match(admin, /<button type="button" @click="activeSection = 'pets'"><strong>\{\{ metrics\.totalPets \}\}<\/strong><span>Mascotas registradas<\/span><\/button>/);
    assert.match(admin, /@click="activeSection = 'shelters'"><strong>\{\{ metrics\.activeShelters \}\}<\/strong>/);
    assert.match(admin, /\.admin-metrics button/);
    assert.doesNotMatch(admin, /\.admin-metrics article \{/);
    assert.match(adoption, /String shelterName/);
    assert.match(adoption, /a\.getPet\(\)\.getShelter\(\)\.getOrganizationName\(\)/);
    assert.match(profile, /\{\{ item\.shelterName \|\| 'Refugio no disponible' \}\} · \{\{ item\.submittedAt/);
    assert.match(profile, /\{\{ item\.shelterName \|\| 'Refugio no disponible' \}\} · \{\{ item\.adoptionDate/);
    assert.match(health, /Historial de \{\{ pet\.name \}\}\.<template v-if="canEdit"> Mantén sus cuidados documentados y sus próximas fechas visibles\.<\/template>/);
    assert.match(assistant, /Pregúntame por una mascota, una solicitud o cómo adoptar/);
    assert.match(footer, /<div class="footer-column"><h2>Ayuda<\/h2><RouterLink to="\/aliados\.html">Aliados<\/RouterLink><RouterLink to="\/acceso\.html">Ingresar<\/RouterLink><RouterLink to="\/privacidad\.html">Aviso de privacidad<\/RouterLink><RouterLink to="\/terminos\.html">Términos y condiciones<\/RouterLink><a href="mailto:pethop13@gmail\.com">Escríbenos: pethop13@gmail\.com<\/a>/);
    assert.doesNotMatch(footer, /Organizaciones<\/h2><RouterLink to="\/acceso\.html"/);
    assert.doesNotMatch(explore, /Senior/);
    assert.match(explore, /Mayor/);
});

test('modern web guidance: skill installed for codex and no legacy slice-sort remains', () => {
    const skill = fs.readFileSync(path.join(projectRoot, '.agents/skills/modern-web-guidance/SKILL.md'), 'utf8');
    const lock = JSON.parse(fs.readFileSync(path.join(projectRoot, 'skills-lock.json'), 'utf8'));
    const messages = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    assert.match(skill, /name: modern-web-guidance/);
    assert.equal(lock.skills['modern-web-guidance'].source, 'GoogleChrome/modern-web-guidance');
    assert.match(messages, /\.toSorted\(\(a, b\) => Number\(Boolean\(b\.pinned\)\) - Number\(Boolean\(a\.pinned\)\)\)/);
    assert.doesNotMatch(messages, /\.slice\(\)\.sort\(/);
    for (const dir of ['src/views', 'src/services', 'src/components']) {
        for (const file of fs.readdirSync(path.join(projectRoot, dir))) {
            const text = fs.readFileSync(path.join(projectRoot, dir, file), 'utf8');
            assert.doesNotMatch(text, /\.slice\(\)\.sort\(/, `${dir}/${file} still uses legacy slice().sort()`);
        }
    }
});

test('audit P0: role actions are hidden, not explained, and interactive contrast passes', () => {
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const card = fs.readFileSync(path.join(projectRoot, 'src/components/PetCard.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const adoption = fs.readFileSync(path.join(projectRoot, 'src/views/AdoptionView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const acceso = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    const solicitud = fs.readFileSync(path.join(projectRoot, 'css/solicitud.css'), 'utf8');

    // Roles: hide, don't explain
    assert.match(header, /v-if="index === 1 && \(!currentUser \|\| currentUser\.role === 'ADOPTER'\)"/);
    assert.match(card, /const canFavorite = computed\(\(\) => !currentUser\.value \|\| currentUser\.value\.role === 'ADOPTER'\)/);
    assert.match(card, /v-if="canFavorite"\s+class="favorite"/);
    assert.match(home, /const canFavorite = computed\(\(\) => !currentUser\.value \|\| currentUser\.value\.role === 'ADOPTER'\)/);
    assert.match(home, /<button v-if="canFavorite" class="favorite"/);
    assert.match(pet, /const canAdopt = computed\(\(\) => !currentUser\.value \|\| currentUser\.value\.role === 'ADOPTER'\)/);
    assert.match(pet, /<button v-if="canAdopt" class="gallery-favorite"/);
    assert.match(pet, /<button v-if="canAdopt" type="button" class="message-shelter-button"/);
    assert.match(pet, /<RouterLink v-if="canAdopt" class="pet-apply-button login-button"/);
    assert.match(pet, /const isOwnPet = computed\(\(\) => currentUser\.value\?\.role === 'SHELTER' && !!myShelterId\.value && !!pet\.value && String\(pet\.value\.shelterId \|\| ''\) === String\(myShelterId\.value\)\)/);
    assert.match(pet, /<RouterLink v-if="isOwnPet" class="pet-apply-button" :to="\{ path: '\/refugio\.html', query: \{ section: 'solicitudes' \} \}">Ver solicitudes<\/RouterLink>/);
    assert.match(adoption, /if \(user\.value\.role === 'SHELTER'\) \{ await router\.replace\(\{ path: '\/refugio\.html', query: \{ section: 'solicitudes' \} \}\)/);
    assert.match(adoption, /if \(user\.value\.role === 'ADMIN'\) \{ await router\.replace\(\{ path: '\/admin\.html' \}\)/);
    assert.match(shelter, /const selectedRequestThread = computed\(\(\) => selectedRequest\.value \? inbox\.value\.find/);
    assert.match(shelter, /<button v-if="selectedRequestThread" class="button button-outline" @click="requestAction\('contact'\)">Contactar<\/button>/);

    // Contrast: the only primary button that actually failed was .catalog-apply
    assert.match(css, /\.catalog-apply,\.catalog-apply:hover,\.catalog-apply:focus-visible \{ border-color:var\(--primary-strong\); background:var\(--primary-strong\); color:var\(--white\); \}/);
    assert.match(css, /\.favorite\.active,[\s\S]*?color:\s*var\(--primary-strong\)/);
    // Action cards: card 2 strong bg, white text; card 3 keeps dark text on its pale bg; hover keeps each card's bg
    assert.match(css, /\.action-card:nth-child\(2\) \{\s*background: var\(--primary-strong\);/);
    assert.match(css, /\.action-card \.action-content strong \{\s*color: var\(--white\);/);
    assert.match(css, /\.action-card:nth-child\(3\) \.action-content strong \{\s*color: var\(--white\);/);
    assert.doesNotMatch(css, /\.action-card:hover \{[^}]*background: var\(--primary-hover\)/);
    // Borders: strong token on form controls, >= 3:1 vs white
    const token = css.match(/--border-strong:\s*(#[0-9A-Fa-f]{6});/);
    assert.ok(token, 'missing --border-strong token');
    const luminance = hex => {
        const [r, g, b] = [0, 2, 4].map(i => parseInt(hex.slice(1 + i, 3 + i), 16) / 255);
        const f = c => c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
        return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
    };
    const ratio = 1.05 / (luminance(token[1]) + 0.05);
    assert.ok(ratio >= 3, `--border-strong ${token[1]} is ${ratio.toFixed(2)}:1 vs white, needs 3:1`);
    for (const [name, source] of [['styles.css', css], ['acceso.css', acceso], ['solicitud.css', solicitud]]) {
        assert.match(source, /var\(--border-strong\)/, `${name} never uses --border-strong`);
    }
    assert.match(css, /\.shelter-field input[^}]*border:1px solid var\(--border-strong\)/);
    // Selection and completed step marker also use the strong tone now
    assert.match(css, /::selection \{\s*background: var\(--primary-strong\);/);
    assert.match(css, /\.status-step\.completed \.status-step-marker,[\s\S]*?background: var\(--primary-strong\);/);
});

test('audit P1: one button system, token radii, one h1 scale, collapsed palette, no name-based photos', () => {
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const acceso = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    const solicitud = fs.readFileSync(path.join(projectRoot, 'css/solicitud.css'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const followUp = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');

    // Buttons: early definitions use the strong tone; the dead .btn-primary is gone
    // (el --primary solo queda como stroke decorativo de tarjetas y detalles, no en botones)
    for (const selector of ['.primary-button', '.login-button', '.search-button', '.filter-button', '.catalog-apply', '.pet-apply-button', '.application-submit', '.organization-button']) {
        const block = css.match(new RegExp(selector.replace('.', '\\.') + ' \\{[\\s\\S]*?\\}'));
        if (block) assert.doesNotMatch(block[0], /border: 1px solid var\(--primary\);/, `${selector} usa --primary en el borde`);
    }
    assert.doesNotMatch(css, /\.btn-primary/);
    assert.match(css, /\.primary-button \{[\s\S]*?border: 1px solid var\(--primary-strong\);[\s\S]*?color: var\(--white\);/);
    assert.match(css, /\.login-button:hover \{[\s\S]*?background: var\(--primary-strong-hover\);/);
    assert.match(css, /\.filter-button:hover \{[\s\S]*?background: var\(--primary-strong-hover\);/);
    // Radii: tokens exist and no hardcoded single-value px radii remain (except pills 99/999)
    assert.match(css, /--radius-xs: 8px;/);
    const radiusRe = /border-radius:\s*(?!999px|99px|var\()(\d+)px\s*[;}]/g;
    for (const [name, source] of [['styles.css', css], ['acceso.css', acceso], ['solicitud.css', solicitud]]) {
        assert.ok(!radiusRe.test(source), `${name} still has hardcoded radii`);
        radiusRe.lastIndex = 0;
    }
    // H1: one scale (catalog, explore scoped override, login); the hero is the
    // intentional exception: bigger (4.25rem max) so it fills the hero.
    assert.match(css, /\.hero h1 \{[\s\S]*?font-size: clamp\(2\.5rem, 5vw, 4\.25rem\);/);
    assert.match(css, /\.catalog-header h1 \{[\s\S]*?font-size: clamp\(2rem, 4vw, 3\.3rem\);/);
    assert.match(explore, /\.catalog-header h1 \{ max-width:970px; color:var\(--hero-title-dark\); font-size:clamp\(2rem,4vw,3\.3rem\);/);
    assert.match(acceso, /font:800 clamp\(2rem,4vw,3\.3rem\)\/1\.12 "Poppins",sans-serif;/);
    // Palette: no stray blue/purple/amber/green/red hex, no name-based photo rules
    assert.doesNotMatch(css, /#28628a|#6652a6|#825e09|#754600|#9c2525|#3dba7c|#14545c/i);
    assert.doesNotMatch(css, /data-name=/);
    assert.doesNotMatch(followUp, /is-blue|is-purple/);
    assert.match(css, /--tint: #EAF5F4;/);
    assert.match(css, /--success-bg: #E6F5EC;/);
    assert.match(css, /--warning-bg: #FFF4D9;/);
    assert.match(css, /--danger-bg: #FFE5E3;/);
    assert.match(css, /--warning-border: #F0C9A8;/);
});

test('audit P2: login language everywhere, dark rhythm, min text 0.75rem, style guide', () => {
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const acceso = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    const solicitud = fs.readFileSync(path.join(projectRoot, 'css/solicitud.css'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    const affiliates = fs.readFileSync(path.join(projectRoot, 'src/views/AffiliatesView.vue'), 'utf8');
    const health = fs.readFileSync(path.join(projectRoot, 'src/views/HealthRecordsView.vue'), 'utf8');
    const pet = fs.readFileSync(path.join(projectRoot, 'src/views/PetView.vue'), 'utf8');
    const band = fs.readFileSync(path.join(projectRoot, 'src/components/DarkBand.vue'), 'utf8');
    const guide = fs.readFileSync(path.join(projectRoot, 'docs/guia-de-estilos.md'), 'utf8');
    const chat = fs.readFileSync(path.join(projectRoot, 'src/components/AssistantChat.vue'), 'utf8');
    const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
    const messages = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterMessagesView.vue'), 'utf8');
    const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
    const followUp = fs.readFileSync(path.join(projectRoot, 'src/views/FollowUpView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const shelter = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');

    // 16. Explorar tiene el aire del login: banda con foto + titular con palabra turquesa
    assert.match(explore, /catalog-header-visual/);
    assert.match(explore, /src="\/img\/mascotas-hero-v2\.webp"/);
    assert.match(explore, /Encuentra una mascota <span>que conecte contigo\.<\/span>/);
    assert.match(styles, /\.catalog-header-visual \{[^}]*border-radius: var\(--radius-lg\);/);
    assert.match(styles, /\.catalog-header-visual \{[^}]*box-shadow: var\(--shadow-md\);/);
    assert.match(styles, /\.catalog-header-container \{[^}]*display: grid;/);
    // La regla tardía del h1 de Explorar ya no contradice la escala única (agujero del P1)
    assert.match(styles, /\.catalog-header h1 \{[^}]*font-size: clamp\(2rem, 4vw, 3\.3rem\);[^}]*letter-spacing: -0\.045em;/);
    // El detalle ya tenía foto (galería) y titular canónico con aire
    assert.match(pet, /pet-gallery/);
    assert.match(styles, /\.pet-profile-heading \{[^}]*margin-bottom: 24px;/);
    assert.match(styles, /\.pet-profile-heading h1 \{[^}]*font-size: clamp\(2rem, 4vw, 3\.3rem\);/);

    // 17. Afiliados y Salud tienen imagen (antes solo listas con iconos)
    assert.match(affiliates, /affiliate-hero-visual/);
    assert.match(affiliates, /src="\/img\/como-funciona-acompana\.jpg"/);
    assert.match(health, /health-pet-photo/);
    assert.match(health, /pet\?\.photos\?\.\[0\]/);

    // 18. Banda oscura en las 4 vistas, con el componente compartido
    assert.match(band, /background:var\(--primary-dark\)/);
    assert.match(band, /color:var\(--white\)/);
    assert.match(band, /background:var\(--white\);color:var\(--primary-strong\)/);
    for (const [name, source] of [['ExploreView', explore], ['PetView', pet], ['AffiliatesView', affiliates], ['HealthRecordsView', health]]) {
        assert.match(source, /<DarkBand /, `${name} no usa DarkBand`);
        assert.match(source, /import DarkBand from '\.\.\/components\/DarkBand\.vue';/, `${name} no importa DarkBand`);
    }
    // Contraste de la banda: texto #DDE8EB sobre #2C5357 y #167F87 sobre blanco, >= 4.5:1
    const luminance = hex => {
        const [r, g, b] = [0, 2, 4].map(i => parseInt(hex.slice(1 + i, 3 + i), 16) / 255);
        const f = c => c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
        return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
    };
    const contrast = (a, b) => {
        const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
        return (hi + 0.05) / (lo + 0.05);
    };
    assert.ok(contrast('#DDE8EB', '#2C5357') >= 4.5, `band text ${contrast('#DDE8EB', '#2C5357').toFixed(2)}:1`);
    assert.ok(contrast('#167F87', '#FFFFFF') >= 4.5, `band CTA ${contrast('#167F87', '#FFFFFF').toFixed(2)}:1`);

    // 19. Sin texto menor de 0.75rem en CSS ni en vistas (rem, em y shorthand)
    // Excepción: los textos secundarios de la tarjeta de mascota usan 0.64-0.7rem
    // como en la referencia aprobada (corrección de Ruisu, 17 nov 2026).
    const stylesMinText = styles
        .replace(/\.pet-species \{[^}]*\}/, '')
        .replace(/\.pet-details span \{[^}]*\}/, '')
        .replace(/\.pet-location \{[^}]*\}/, '');
    const smallSize = /font-size:\s*0?\.(?:[0-6][0-9]?|7[0-4]?)rem/;
    const smallEm = /font-size:\s*0?\.(?:[0-6][0-9]?|7[0-4]?)em/;
    const smallShorthand = /font:\s*[^\s;}]*\s+0?\.(?:[0-6][0-9]?|7[0-4]?)rem/;
    for (const [name, source] of [['styles.css', stylesMinText], ['acceso.css', acceso], ['solicitud.css', solicitud], ['AssistantChat', chat], ['SiteHeader', header], ['AdminView', admin], ['AdopterMessagesView', messages], ['AdopterProfileView', profile], ['AffiliatesView', affiliates], ['ExploreView', explore], ['FollowUpView', followUp], ['HealthRecordsView', health], ['HomeView', home], ['PetView', pet], ['ShelterView', shelter]]) {
        assert.doesNotMatch(source, smallSize, `${name} tiene font-size < 0.75rem`);
        assert.doesNotMatch(source, smallEm, `${name} tiene font-size < 0.75em`);
        assert.doesNotMatch(source, smallShorthand, `${name} tiene font shorthand < 0.75rem`);
    }

    // 20. La guía existe y documenta tokens + 3 ejemplos
    assert.match(guide, /# Guía de estilos de Pethop/);
    assert.match(guide, /--primary-strong/);
    assert.match(guide, /--radius-lg/);
    assert.match(guide, /--shadow-sm/);
    assert.match(guide, /Inicio de sesión/);
    assert.match(guide, /Tarjeta de mascota/);
    assert.match(guide, /Botón primario/);
});

test('audit fix: titles use the logo system, white surfaces, secondary stroke', () => {
    const styles = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    const petCard = fs.readFileSync(path.join(projectRoot, 'src/components/PetCard.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    // Sistema de color del logo: main #167F87, oscuro #2C5357, secundario #3EB2BA.
    assert.match(styles, /--hero-title-dark: #2C5357;/);
    assert.match(styles, /--hero-title-light: #3EB2BA;/);
    assert.match(styles, /--background: #FFFFFF;/);
    assert.match(styles, /--primary-soft: #FFFFFF;/);
    assert.doesNotMatch(styles, /--background: #F3FEFF;/);
    assert.doesNotMatch(styles, /--primary-soft: #F3FEFF;/);
    // Títulos de dos colores usan el sistema; los de un color quedan en negro casi.
    assert.match(styles, /\.hero h1 \{[^}]*color: var\(--hero-title-dark\);/);
    assert.match(styles, /\.hero h1 span \{[^}]*color: var\(--hero-title-light\);/);
    assert.match(styles, /\.section-title h2 \{[^}]*color: var\(--text\);/);
    assert.match(styles, /\.pet-title h3 \{[^}]*color: var\(--text\);/);
    assert.match(styles, /\.pet-profile-heading h1 \{[^}]*color: var\(--text\);/);
    assert.match(styles, /\.application-header h1 \{[^}]*color: var\(--text\);/);
    // Tarjeta: borde fino secundario, chips blancos con stroke, textos como la referencia.
    assert.match(styles, /\.pet-card \{[^}]*border: 1px solid var\(--primary\);/);
    assert.match(styles, /\.pet-details span,[\s\S]*?border: 1px solid var\(--primary\);[\s\S]*?background: var\(--white\);/);
    assert.match(styles, /\.pet-species \{[^}]*font-size: 0\.64rem;/);
    assert.match(styles, /\.pet-details span \{[^}]*font-size: 0\.65rem;/);
    assert.match(styles, /\.pet-location \{[^}]*font-size: 0\.7rem;/);
    // Especie en caja normal, como en la referencia.
    assert.doesNotMatch(petCard, /toLocaleUpperCase/);
    assert.doesNotMatch(home, /toLocaleUpperCase/);
});

test('prelaunch A: legal pages and cookie banner', () => {
    const router = fs.readFileSync(path.join(projectRoot, 'src/router/index.js'), 'utf8');
    const privacy = fs.readFileSync(path.join(projectRoot, 'src/views/PrivacyView.vue'), 'utf8');
    const terms = fs.readFileSync(path.join(projectRoot, 'src/views/TermsView.vue'), 'utf8');
    const app = fs.readFileSync(path.join(projectRoot, 'src/App.vue'), 'utf8');
    const banner = fs.readFileSync(path.join(projectRoot, 'src/components/CookieBanner.vue'), 'utf8');
    const footer = fs.readFileSync(path.join(projectRoot, 'src/components/SiteFooter.vue'), 'utf8');
    const server = fs.readFileSync(path.join(projectRoot, 'server.mjs'), 'utf8');
    assert.match(router, /path: '\/privacidad\.html'[^}]*title: 'Aviso de privacidad \| PetHop'[^}]*description: /);
    assert.match(router, /path: '\/terminos\.html'[^}]*title: 'Términos y condiciones \| PetHop'[^}]*description: /);
    assert.match(router, /meta\.description/);
    assert.match(privacy, /Ley Federal de Protección de Datos Personales en Posesión de los Particulares/);
    assert.match(privacy, /derechos de Acceso, Rectificación, Cancelación y Oposición \(ARCO\)/);
    assert.match(privacy, /pethop13@gmail\.com/);
    assert.match(terms, /refugios verificados/);
    assert.match(terms, /PetHop no garantiza que una solicitud sea aprobada/);
    assert.match(terms, /proyecto universitario/);
    assert.match(footer, /to="\/privacidad\.html">Aviso de privacidad</);
    assert.match(footer, /to="\/terminos\.html">Términos y condiciones</);
    assert.match(app, /import CookieBanner from '\.\/components\/CookieBanner\.vue';/);
    assert.match(app, /<CookieBanner \/>/);
    assert.match(banner, /pethop-cookie-consent/);
    assert.match(banner, />Aceptar</);
    assert.match(banner, />Rechazar</);
    assert.match(server, /'privacidad\.html', 'refugio\.html'/);
    assert.match(server, /'solicitud\.html', 'terminos\.html'/);
});

test('prelaunch B: meta, Open Graph, sitemap and robots', () => {
    const index = fs.readFileSync(path.join(projectRoot, 'index.html'), 'utf8');
    const router = fs.readFileSync(path.join(projectRoot, 'src/router/index.js'), 'utf8');
    const sitemap = fs.readFileSync(path.join(projectRoot, 'public/sitemap.xml'), 'utf8');
    const robots = fs.readFileSync(path.join(projectRoot, 'public/robots.txt'), 'utf8');
    assert.match(index, /<meta name="description" content="PetHop conecta adoptantes/);
    assert.match(index, /<meta name="theme-color" content="#FFFFFF">/);
    assert.match(index, /<meta property="og:title" content="PetHop \| Adopta, conecta, cambia vidas">/);
    assert.match(index, /<meta property="og:image" content="https:\/\/pethop-production\.up\.railway\.app\/img\/mascotas-hero-og\.jpg">/);
    assert.match(index, /<meta property="og:url" content="https:\/\/pethop-production\.up\.railway\.app\/">/);
    assert.match(index, /<meta name="twitter:card" content="summary_large_image">/);
    const titles = router.match(/title: '/g).length;
    const descriptions = router.match(/description: '/g).length;
    assert.equal(titles, descriptions, `every route title needs a description (${titles} titles, ${descriptions} descriptions)`);
    for (const page of ['inicio.html', 'explorar.html', 'aliados.html', 'acceso.html', 'privacidad.html', 'terminos.html']) {
        assert.ok(sitemap.includes(`<loc>https://pethop-production.up.railway.app/${page}</loc>`), `sitemap lacks ${page}`);
    }
    assert.doesNotMatch(sitemap, /admin\.html/);
    assert.match(robots, /Sitemap: https:\/\/pethop-production\.up\.railway\.app\/sitemap\.xml/);
    assert.match(robots, /Disallow: \/admin\.html/);
});

test('prelaunch C: custom 404 page', () => {
    const router = fs.readFileSync(path.join(projectRoot, 'src/router/index.js'), 'utf8');
    const view = fs.readFileSync(path.join(projectRoot, 'src/views/NotFoundView.vue'), 'utf8');
    const server = fs.readFileSync(path.join(projectRoot, 'server.mjs'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    assert.doesNotMatch(router, /pathMatch\(\.\*\)\*', redirect: '\/inicio\.html'/);
    assert.match(router, /path: '\/:pathMatch\(\.\*\)\*', name: 'not-found', meta: \{ title: 'Página no encontrada \| PetHop', description: /);
    assert.match(router, /component: \(\) => import\('\.\.\/views\/NotFoundView\.vue'\)/);
    assert.match(view, /404/);
    assert.match(view, /se escapó\./);
    assert.match(view, /to="\/inicio\.html"[^>]*>Volver al inicio</);
    assert.match(view, /to="\/explorar\.html"[^>]*>Explorar mascotas</);
    assert.match(server, /toLowerCase\(\) !== '\.html'/);
    assert.match(css, /\.not-found-code \{[^}]*color: var\(--primary-dark\);/);
});

test('prelaunch D: compressed images, no dead favicons', () => {
    const webp = path.join(projectRoot, 'img/mascotas-hero-v2.webp');
    const og = path.join(projectRoot, 'img/mascotas-hero-og.jpg');
    const favicon = path.join(projectRoot, 'img/favicon.png');
    assert.ok(fs.existsSync(webp), 'hero WebP missing');
    assert.ok(fs.statSync(webp).size < 400 * 1024, `hero WebP too big: ${fs.statSync(webp).size} bytes`);
    assert.ok(fs.existsSync(og), 'OG image missing');
    assert.ok(fs.statSync(og).size < 200 * 1024, `OG image too big: ${fs.statSync(og).size} bytes`);
    assert.ok(fs.statSync(favicon).size < 150 * 1024, `favicon too big: ${fs.statSync(favicon).size} bytes`);
    for (const dead of ['img/favicon2.png', 'img/favicaon.png', 'img/favicoaan.png', 'img/favicon3.png', 'img/mascotas-hero-v2.png']) {
        assert.ok(!fs.existsSync(path.join(projectRoot, dead)), `${dead} still present`);
    }
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const explore = fs.readFileSync(path.join(projectRoot, 'src/views/ExploreView.vue'), 'utf8');
    for (const [name, source] of [['HomeView', home], ['AccessView', access], ['ExploreView', explore]]) {
        assert.doesNotMatch(source, /mascotas-hero-v2\.png/, `${name} still references the PNG hero`);
        assert.match(source, /mascotas-hero-v2\.webp/, `${name} does not use the WebP hero`);
    }
});

test('prelaunch E: HTTPS redirect, HSTS, sitemap/robots content types', () => {
    const server = fs.readFileSync(path.join(projectRoot, 'server.mjs'), 'utf8');
    assert.match(server, /x-forwarded-proto/);
    assert.match(server, /writeHead\(301, \{ Location: `https:\/\/\$\{request\.headers\.host\}/);
    assert.match(server, /Strict-Transport-Security': 'max-age=31536000; includeSubDomains'/);
    assert.match(server, /'\.xml': 'application\/xml; charset=utf-8'/);
    assert.match(server, /'\.txt': 'text\/plain; charset=utf-8'/);
});

test('prelaunch F: honeypot antispam on public auth forms', () => {
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    const auth = fs.readFileSync(path.join(projectRoot, 'src/services/auth.js'), 'utf8');
    const registerRequest = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/dto/RegisterRequest.java'), 'utf8');
    const resetRequest = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/dto/PasswordResetRequest.java'), 'utf8');
    const controller = fs.readFileSync(path.join(projectRoot, 'backend/src/main/java/com/pethop/security/AuthController.java'), 'utf8');
    const acceso = fs.readFileSync(path.join(projectRoot, 'css/acceso.css'), 'utf8');
    for (const id of ['register-website', 'reset-website', 'resend-website']) {
        assert.match(access, new RegExp(`id="${id}" v-model="\\w+\\.website"`), `missing honeypot ${id}`);
    }
    assert.match(access, /website: registration\.value\.website \|\| null/);
    assert.match(auth, /requestPasswordReset\(email, website\)/);
    assert.match(auth, /resendEmailVerification\(email, website\)/);
    assert.match(registerRequest, /String website\)/);
    assert.match(resetRequest, /String website\)/);
    assert.match(controller, /request\.website\(\) != null && !request\.website\(\)\.isBlank\(\)/);
    assert.match(controller, /if \(request\.website\(\) == null \|\| request\.website\(\)\.isBlank\(\)\) passwordReset\.request/);
    assert.match(controller, /if \(request\.website\(\) == null \|\| request\.website\(\)\.isBlank\(\)\) emailVerification\.resend/);
    assert.match(acceso, /\.honeypot-field \{[^}]*left: -9999px;/);
});

test('prelaunch G: GA4 loads only with ID and cookie consent', () => {
    const analytics = fs.readFileSync(path.join(projectRoot, 'src/services/analytics.js'), 'utf8');
    const main = fs.readFileSync(path.join(projectRoot, 'src/main.js'), 'utf8');
    const router = fs.readFileSync(path.join(projectRoot, 'src/router/index.js'), 'utf8');
    const banner = fs.readFileSync(path.join(projectRoot, 'src/components/CookieBanner.vue'), 'utf8');
    assert.match(analytics, /VITE_GA_MEASUREMENT_ID/);
    assert.match(analytics, /pethop-cookie-consent'\) === 'accepted'/);
    assert.match(analytics, /anonymize_ip: true/);
    assert.match(analytics, /googletagmanager\.com\/gtag\/js\?id=/);
    assert.match(analytics, /export function initAnalytics/);
    assert.match(analytics, /export function trackPageView/);
    assert.match(main, /import \{ initAnalytics \} from '\.\/services\/analytics\.js';/);
    assert.match(main, /initAnalytics\(\);/);
    assert.match(router, /import \{ trackPageView \} from '\.\.\/services\/analytics\.js';/);
    assert.match(router, /trackPageView\(to\.fullPath\);/);
    assert.match(banner, /if \(value === 'accepted'\) initAnalytics\(\);/);
});

test('audit fix: hero title is bigger and higher, third action card is teal with white text', () => {
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    // Hero: the late rule used to cap the title at 3.3rem (the 4.7rem design size
    // never rendered) and the 570px hero left empty space above it.
    assert.match(css, /\.hero h1 \{[^}]*font-size: clamp\(2\.5rem, 5vw, 4\.25rem\);/);
    assert.match(css, /\.hero \{[^}]*min-height: 500px;/);
    assert.match(css, /\.hero-container \{[^}]*min-height: 500px;/);
    // Third card: the late rule puts all three cards on --primary-strong, so
    // its text must be white like the other two (dark text on teal was the bug);
    // the card itself stays teal, never white.
    assert.match(css, /\.action-card:nth-child\(3\) \{\s*background: var\(--primary-strong\);/);
    assert.match(css, /\.action-card:nth-child\(3\) \.action-content strong \{\s*color: var\(--white\);/);
    assert.doesNotMatch(css, /\.action-card:nth-child\(3\) \{[^}]*background: var\(--white\);/);
    // White text on the teal third card passes AA.
    const luminance = hex => {
        const [r, g, b] = [0, 2, 4].map(i => parseInt(hex.slice(1 + i, 3 + i), 16) / 255);
        const f = c => c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
        return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
    };
    const contrast = (a, b) => {
        const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
        return (hi + 0.05) / (lo + 0.05);
    };
    assert.ok(contrast('#FFFFFF', '#167F87') >= 4.5, `card 3 text ${contrast('#FFFFFF', '#167F87').toFixed(2)}:1`);
});

test('profile has a touch of color: teal avatar ring, panel top accent, tint icon chips', () => {
  const profile = fs.readFileSync(path.join(projectRoot, 'src/views/AdopterProfileView.vue'), 'utf8');
  // Avatar ring in the logo secondary teal (was --primary-soft, invisible on white).
  assert.match(profile, /\.adopter-avatar \{[^}]*border:4px solid var\(--primary\);/);
  // Personal and activity panels carry a 3px secondary-teal top line.
  assert.match(profile, /:deep\(\.adopter-activity-panel\) \{[^}]*border-top:3px solid var\(--primary\);/);
  // Panel icons sit in soft-tint circles instead of bare teal glyphs.
  assert.match(profile, /\.adopter-panel-heading > div > :deep\(svg\) \{[^}]*background:var\(--tint\);/);
});

test('admin menu selection uses the same tint as the shelter menu', () => {
  // The admin active item used var(--background), which is white since the
  // logo color system, so the selection was invisible on the white sidebar.
  // The shelter menu marks its active item with the tint background.
  const admin = fs.readFileSync(path.join(projectRoot, 'src/views/AdminView.vue'), 'utf8');
  assert.match(admin, /\.admin-nav-item\.active \{ background:var\(--tint\); color:var\(--primary-strong\); \}/);
  assert.match(admin, /\.admin-bottombar-item\.active \{ background:var\(--tint\); color:var\(--primary-strong\); \}/);
  assert.doesNotMatch(admin, /\.admin-nav-item\.active \{ background:var\(--background\);/);
});

test('organization section label is plain teal text, not a highlighted pill', () => {
  const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
  assert.doesNotMatch(css, /\.organization-container > div > span \{[^}]*background:/);
  assert.match(css, /\.organization-container > div > span \{[^}]*color: var\(--primary-strong\);/);
});
