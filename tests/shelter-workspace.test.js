import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { shelterWorkspaceKey } from '../src/services/shelterWorkspace.js';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('shelter workspace keys are isolated per authenticated user', () => {
  assert.equal(shelterWorkspaceKey('user-a'), 'pethop_shelter_workspace:user-a');
  assert.equal(shelterWorkspaceKey('user-b'), 'pethop_shelter_workspace:user-b');
  assert.notEqual(shelterWorkspaceKey('user-a'), shelterWorkspaceKey('user-b'));
});

test('shelter workspace keys safely encode identifiers and reject anonymous state', () => {
  assert.equal(shelterWorkspaceKey('shelter/a'), 'pethop_shelter_workspace:shelter%2Fa');
  assert.equal(shelterWorkspaceKey(''), null);
  assert.equal(shelterWorkspaceKey(null), null);
});

test('shelter view does not read or write the old global workspace key', () => {
  const shelterView = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
  assert.doesNotMatch(shelterView, /storageService\.(read|write)\(['"]pethop_shelter_workspace['"]/);
  assert.match(shelterView, /storageService\.read\(shelterWorkspaceKey\(user\.id\)/);
  assert.match(shelterView, /storageService\.write\(workspaceKey/);
});

test('shelter workspace persists only local draft and view preferences, not backend data', () => {
  const shelterView = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
  // El borrador y las preferencias de vista son lo único local; mascotas,
  // solicitudes, mensajes y perfil viven en el backend y se recargan.
  assert.match(shelterView, /const localOnly = \{ draft, section: state\.section, step: state\.step, filter: state\.filter, petFilter: state\.petFilter, notify: state\.notify \}/);
  assert.match(shelterView, /storageService\.write\(workspaceKey, localOnly\)/);
  assert.doesNotMatch(shelterView, /storageService\.write\(workspaceKey, \{ \.\.\.state, draft \}\)/);
});

test('request detail modal links to the pet health record', () => {
  const shelterView = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
  assert.match(shelterView, /path: '\/salud\.html', query: \{ pet: selectedRequest\.petId \}/);
  assert.match(shelterView, /Expediente sanitario<\/RouterLink><button class="button button-outline" @click="requestAction\('contact'\)"/);
});

test('tracking link sends shelters to their requests instead of the adopter follow-up', () => {
  const header = fs.readFileSync(path.join(projectRoot, 'src/components/SiteHeader.vue'), 'utf8');
  const footer = fs.readFileSync(path.join(projectRoot, 'src/components/SiteFooter.vue'), 'utf8');
  assert.match(header, /const links = computed\(\(\) => \[/);
  assert.match(header, /currentUser\.value\?\.role === 'SHELTER' \? '\/refugio\.html\?section=solicitudes' : '\/seguimiento\.html'/);
  assert.match(footer, /const trackingLink = computed\(\(\) => currentUser\.value\?\.role === 'SHELTER' \? '\/refugio\.html\?section=solicitudes' : '\/seguimiento\.html'\)/);
  assert.match(footer, /<RouterLink :to="trackingLink">Seguimiento<\/RouterLink>/);
});

test('health record form is only shown to the owner shelter or an admin', () => {
  const healthView = fs.readFileSync(path.join(projectRoot, 'src/views/HealthRecordsView.vue'), 'utf8');
  assert.match(healthView, /const myShelterId = ref\(''\)/);
  assert.match(healthView, /isOwnerShelter = computed\(\(\) => currentUser\.value\?\.role === 'SHELTER' && !!myShelterId\.value && !!pet\.value && String\(pet\.value\.shelterId \|\| ''\) === String\(myShelterId\.value\)\)/);
  assert.match(healthView, /const canEdit = computed\(\(\) => currentUser\.value\?\.role === 'ADMIN' \|\| isOwnerShelter\.value\)/);
  assert.match(healthView, /const shelter = await api\('\/shelters\/me'\)/);
  assert.match(healthView, /Solo el refugio propietario de \{\{ pet\?\.name \}\} puede agregar registros a su expediente\./);
  assert.doesNotMatch(healthView, /canEdit = computed\(\(\) => currentUser\.value\?\.role === 'SHELTER' \|\| currentUser\.value\?\.role === 'ADMIN'\)/);
});

test('publish wizard can add health records at publication time', () => {
  const shelterView = fs.readFileSync(path.join(projectRoot, 'src/views/ShelterView.vue'), 'utf8');
  assert.match(shelterView, /draft\.healthRecords = Array\.isArray\(draft\.healthRecords\) \? draft\.healthRecords : \[\]/);
  assert.match(shelterView, /v-if="state\.step===1" class="shelter-health-records"/);
  assert.match(shelterView, /state\.draft\.healthRecords\.push\(record\)/);
  assert.match(shelterView, /for \(const record of \(draft\.healthRecords \|\| \[\]\)\) \{/);
  assert.match(shelterView, /await api\(`\/pets\/\$\{created\.id\}\/health-records`, \{ method: 'POST', body \}\)/);
  assert.match(shelterView, /catch \{ failedRecords\.push\(record\); \}/);
  assert.match(shelterView, /se agregarán al publicar/);
});

test('pages share one container width instead of leaving extra space on the sides', () => {
  const css = fs.readFileSync(path.join(projectRoot, 'css/styles.css'), 'utf8');
  assert.match(css, /--container-width: 1320px;/);
  for (const view of ['HealthRecordsView.vue', 'AdopterProfileView.vue', 'AffiliatesView.vue']) {
    const source = fs.readFileSync(path.join(projectRoot, 'src/views', view), 'utf8');
    assert.match(source, /width:min\(var\(--container-width\),calc\(100% - 40px\)\)/);
  }
  const home = fs.readFileSync(path.join(projectRoot, 'src/views/HomeView.vue'), 'utf8');
  assert.match(home, /\.process-carousel \{ position:relative; width:min\(var\(--container-width\), 100%\);/);
  const skeleton = fs.readFileSync(path.join(projectRoot, 'src/components/PageLoadingSkeleton.vue'), 'utf8');
  assert.match(skeleton, /width:min\(var\(--container-width\), calc\(100% - 48px\)\)/);
  for (const view of ['HealthRecordsView.vue', 'AdopterProfileView.vue', 'AffiliatesView.vue', 'HomeView.vue']) {
    const source = fs.readFileSync(path.join(projectRoot, 'src/views', view), 'utf8');
    assert.doesNotMatch(source, /width:min\(1120px/);
  }
});
