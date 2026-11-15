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
