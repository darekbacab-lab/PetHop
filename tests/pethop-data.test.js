import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import '../js/pethop-data.js';
import { createStorageService, sessionStorageService } from '../src/services/storage.js';

const data = globalThis.PetHopData;
const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const legacyPages = path.join(projectRoot, 'src', 'legacy', 'pages');

class MemoryStorage {
    #items = new Map();
    getItem(key) { return this.#items.has(key) ? this.#items.get(key) : null; }
    setItem(key, value) { this.#items.set(key, String(value)); }
    removeItem(key) { this.#items.delete(key); }
}

test('malformed browser storage falls back safely and storage failures are reported', () => {
    const storage = new MemoryStorage();
    storage.setItem(data.MESSAGES_KEY, 'not-json');
    assert.deepEqual(data.messages(storage), []);

    const blockedStorage = { getItem() { throw new Error('blocked'); }, setItem() { throw new Error('quota'); } };
    assert.deepEqual(data.read(blockedStorage, data.MESSAGES_KEY, []), []);
    assert.equal(data.write(blockedStorage, data.MESSAGES_KEY, []), false);
});

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

test('only public provider pets enter the catalog and duplicates are excluded', () => {
    const storage = new MemoryStorage();
    data.write(storage, data.PROVIDER_KEY, { pets: [
        { id: 'draft-1', name: 'Borrador', status: 'En revisión' },
        { id: 'pub-1', name: 'Sol', status: 'Publicada', photos: ['data:image/jpeg;base64,AA=='] }
    ] });

    assert.deepEqual(data.publishedPets(storage, ['PUB-1']).map(pet => pet.id), []);
    assert.deepEqual(data.publishedPets(storage).map(pet => pet.name), ['Sol']);
    assert.equal(data.petDirectory({ chino: { id: 'chino' } }, storage)['pub-1'].name, 'Sol');
});

test('published provider data does not erase known catalog details when a field is missing', () => {
    const storage = new MemoryStorage();
    data.write(storage, data.PROVIDER_KEY, { pets: [
        { id: 'chino', name: 'Chino', status: 'Publicada', image: 'chino' }
    ] });

    const directory = data.petDirectory({ chino: { id: 'chino', age: 'Adulto', size: 'Mediano', location: 'Playa del Carmen' } }, storage);
    assert.equal(directory.chino.age, 'Adulto');
    assert.equal(directory.chino.size, 'Mediano');
    assert.equal(directory.chino.location, 'Playa del Carmen');
    assert.equal(directory.chino.status, 'Publicada');
});

test('adopter and provider exchange messages in the same thread', () => {
    const storage = new MemoryStorage();
    const thread = {
        id: 'adoptante@ejemplo.mx:sol-1',
        petId: 'sol-1',
        petName: 'Sol',
        owner: 'adoptante@ejemplo.mx',
        adopter: 'Adoptante',
        messages: [{ sender: 'adopter', text: '¿Cómo se lleva con perros?', createdAt: '2026-10-13T12:00:00.000Z' }]
    };
    assert.equal(data.upsertMessageThread(storage, thread), true);
    const providerThread = data.providerThreads(storage)[0];
    assert.equal(providerThread.id, thread.id);
    assert.equal(providerThread.messages[0].from, 'adopter');
    assert.equal(data.appendMessage(storage, thread.id, { sender: 'provider', text: 'Muy bien', createdAt: '2026-10-13T12:01:00.000Z' }), true);

    const roundTrip = data.messages(storage)[0];
    assert.equal(roundTrip.messages.length, 2);
    assert.equal(data.providerThreads(storage)[0].messages[1].from, 'provider');
});

test('threads without a pet or a nonempty message cannot be written', () => {
    const storage = new MemoryStorage();
    assert.equal(data.upsertMessageThread(storage, { id: 'broken' }), false);
    assert.equal(data.appendMessage(storage, 'missing', { sender: 'provider', text: '  ' }), false);
});

test('the seven views own declarative templates and do not depend on the legacy renderer', () => {
    const pages = {
        'index.html': 'HomeView.vue',
        'acceso.html': 'AccessView.vue',
        'explorar.html': 'ExploreView.vue',
        'mascota.html': 'PetView.vue',
        'solicitud.html': 'AdoptionView.vue',
        'seguimiento.html': 'FollowUpView.vue',
        'proveedor.html': 'ProviderView.vue'
    };
    for (const [page, view] of Object.entries(pages)) {
        const source = fs.readFileSync(path.join(legacyPages, page), 'utf8');
        const viewSource = fs.readFileSync(path.join(projectRoot, 'src', 'views', view), 'utf8');
        assert.ok(source.includes('<body'), `${page} is retained as a migration reference`);
        assert.ok(viewSource.includes('<template'), `${view} has a Vue template`);
        assert.ok(!viewSource.includes('LegacyPageRenderer'), `${view} does not use the legacy renderer`);
        assert.ok(!viewSource.includes('v-html'), `${view} does not inject markup`);
        assert.ok(!viewSource.includes('../legacy/pages/'), `${view} does not import archived HTML`);
    }
});

test('active HTML entry points load only the Vue/Vite bootstrap', () => {
    for (const page of ['index.html', 'acceso.html', 'explorar.html', 'mascota.html', 'proveedor.html', 'seguimiento.html', 'solicitud.html']) {
        const html = fs.readFileSync(path.join(projectRoot, page), 'utf8');
        assert.ok(html.includes('src="/src/main.js"'), `${page} loads the Vue application`);
        assert.ok(!html.includes('app.js'), `${page} does not load the former global controller`);
        assert.ok(!html.includes('js/proveedor.js'), `${page} does not load the former provider controller`);
        assert.ok(!html.includes('js/acceso.js'), `${page} does not load the former access controller`);
    }
    for (const view of ['HomeView.vue','AccessView.vue','ExploreView.vue','PetView.vue','AdoptionView.vue','FollowUpView.vue','ProviderView.vue']) {
        const source = fs.readFileSync(path.join(projectRoot, 'src', 'views', view), 'utf8');
        assert.ok(!source.includes('legacyRuntime'), `${view} does not import legacy runtime`);
    }
});

test('preserved local HTML links and script or stylesheet assets resolve', () => {
    const pages = ['index.html', 'explorar.html', 'mascota.html', 'proveedor.html', 'seguimiento.html', 'solicitud.html', 'acceso.html'];
    const missing = [];
    for (const page of pages) {
        const html = fs.readFileSync(path.join(legacyPages, page), 'utf8');
        for (const match of html.matchAll(/\b(?:href|src)\s*=\s*["']([^"']+)["']/gi)) {
            const value = match[1].trim();
            if (!value || /^(?:#|https?:|mailto:|tel:|javascript:|data:|\/\/)/i.test(value)) continue;
            const pathname = decodeURIComponent(value.split(/[?#]/, 1)[0]);
            if (!pathname) continue;
            if (!fs.existsSync(path.resolve(projectRoot, pathname))) missing.push(`${page} -> ${value}`);
        }
    }
    assert.deepEqual(missing, [], `Missing local references: ${missing.join(', ')}`);
});

test('all HTML entry points load the Vue application and route manifest covers every existing page', () => {
    const expectedRoutes = ['/acceso.html', '/explorar.html', '/mascota.html', '/solicitud.html', '/seguimiento.html', '/proveedor.html'];
    const router = fs.readFileSync(path.join(projectRoot, 'src', 'router', 'index.js'), 'utf8');
    for (const route of expectedRoutes) assert.ok(router.includes(`path: '${route}'`), `router registers ${route}`);

    for (const page of ['index.html', ...expectedRoutes.map(route => route.slice(1))]) {
        const html = fs.readFileSync(path.join(projectRoot, page), 'utf8');
        assert.ok(html.includes('/src/main.js'), `${page} starts Vue/Vite`);
    }
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
    const providerHeader = css.match(/\.provider-page\s+\.site-header\s*\{[^}]*z-index:\s*(\d+)/);
    assert.ok(header && backdrop && providerHeader, 'header, backdrop and provider header define stacking levels');
    assert.ok(Number(header[1]) > Number(backdrop[1]), 'header stacking context remains above the backdrop');
    assert.ok(Number(providerHeader[1]) > Number(backdrop[1]), 'provider entry header remains above the backdrop');

    const source = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
    assert.match(source, /:class="\{ open \}"/);
    assert.match(source, /:inert="!open"/);
    assert.match(source, /@click="open=false"/);
    assert.match(source, /@click="open = !open"/);
    for (const label of ['Inicio', 'Explorar', 'Dar en adopción', 'Seguimiento', 'Favoritos']) assert.ok(source.includes(label));
});

test('catalog cards retain the original information hierarchy and open accessibly without an extra CTA', () => {
    const source = fs.readFileSync(path.join(projectRoot, 'src/components/PetCard.vue'), 'utf8');
    const original = fs.readFileSync(path.join(legacyPages, 'explorar.html'), 'utf8');
    for (const marker of ['catalog-card-image', 'catalog-card-content', 'catalog-card-top', 'catalog-details', 'catalog-location']) {
        assert.ok(source.includes(marker), `Vue card keeps ${marker}`);
        assert.ok(original.includes(marker), `original card includes ${marker}`);
    }

    assert.match(source, /@click\.stop="emit\('favorite', pet\.id\)"/);
    assert.match(source, /pethop_selected_pet/);
    assert.doesNotMatch(source, /Conocer más/);
});

test('organization registration entry points save locally and lead into the provider workspace', () => {
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const provider = fs.readFileSync(path.join(projectRoot, 'src/views/ProviderView.vue'), 'utf8');
    const access = fs.readFileSync(path.join(projectRoot, 'src/views/AccessView.vue'), 'utf8');
    assert.match(home, /Registrar refugio/);
    assert.match(provider, /chooseRole\('organization'\)/);
    assert.match(access, /value="organization"/);
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
    assert.match(pet, /:class="\{ active: favorites\.includes\(pet\.id\) \}"/);
    assert.match(css, /\.favorite\.active,[\s\S]*?color:\s*#3EB2BA/);
    assert.match(css, /\.favorite:not\(\.active\),[\s\S]*?color:\s*#647174/);
});

test('provider dashboard restores the original overview composition and accessible responsive navigation', () => {
    const provider = fs.readFileSync(path.join(projectRoot, 'src/views/ProviderView.vue'), 'utf8');
    const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
    const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
    for (const label of ['Inicio','Mis mascotas','Publicar mascota','Solicitudes','En proceso','Completadas','Mensajes','Seguimiento','Perfil / Organización','Configuración','Ayuda']) assert.ok(provider.includes(`label: '${label}'`));
    for (const marker of ['overviewStats','recentRequests','provider-home-grid','provider-journey','recentPets','provider-sidebar-backdrop','aria-controls="provider-sidebar"','Volver a PetHop']) assert.ok(provider.includes(marker), `provider view includes ${marker}`);
    assert.match(provider, /item\.icon/);
    assert.match(home, /Registrar refugio/);
    assert.match(css, /\.provider-sidebar-backdrop/);
    assert.match(css, /white-space:\s*nowrap/);
    assert.doesNotMatch(css, /provider-dashboard-active \.provider-sidebar nav\s*\{\s*display:\s*flex/);
});
