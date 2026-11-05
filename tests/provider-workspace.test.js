import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { providerWorkspaceKey } from '../src/services/providerWorkspace.js';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

test('provider workspace keys are isolated per authenticated user', () => {
  assert.equal(providerWorkspaceKey('user-a'), 'pethop_provider_workspace:user-a');
  assert.equal(providerWorkspaceKey('user-b'), 'pethop_provider_workspace:user-b');
  assert.notEqual(providerWorkspaceKey('user-a'), providerWorkspaceKey('user-b'));
});

test('provider workspace keys safely encode identifiers and reject anonymous state', () => {
  assert.equal(providerWorkspaceKey('shelter/a'), 'pethop_provider_workspace:shelter%2Fa');
  assert.equal(providerWorkspaceKey(''), null);
  assert.equal(providerWorkspaceKey(null), null);
});

test('provider view does not read or write the old global workspace key', () => {
  const providerView = fs.readFileSync(path.join(projectRoot, 'src/views/ProviderView.vue'), 'utf8');
  assert.doesNotMatch(providerView, /storageService\.(read|write)\(['"]pethop_provider_workspace['"]/);
  assert.match(providerView, /storageService\.read\(providerWorkspaceKey\(user\.id\)/);
  assert.match(providerView, /storageService\.write\(workspaceKey/);
});
