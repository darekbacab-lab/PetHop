import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const prodConfig = fs.readFileSync(new URL('../backend/src/main/resources/application-prod.yml', import.meta.url), 'utf8');
const compose = fs.readFileSync(new URL('../docker-compose.yml', import.meta.url), 'utf8');
const api = fs.readFileSync(new URL('../src/services/api.js', import.meta.url), 'utf8');
const viteConfig = fs.readFileSync(new URL('../vite.config.js', import.meta.url), 'utf8');

test('production backend configuration requires deployment values and hides API documentation', () => {
  for (const variable of ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'FRONTEND_ORIGINS', 'SESSION_COOKIE_SAME_SITE', 'FILE_STORAGE_PATH']) {
    assert.match(prodConfig, new RegExp(`\\$\\{${variable}\\}`));
  }
  assert.match(prodConfig, /ddl-auto:\s*validate/);
  assert.match(prodConfig, /api-docs:\s*\n\s+enabled:\s*false/);
  assert.match(prodConfig, /swagger-ui:\s*\n\s+enabled:\s*false/);
});

test('compose production backend uses explicit configuration and persistent storage', () => {
  assert.match(compose, /SPRING_PROFILES_ACTIVE:\s*prod/);
  for (const variable of ['DB_USERNAME', 'FRONTEND_ORIGINS', 'SESSION_COOKIE_SAME_SITE']) {
    assert.match(compose, new RegExp(`${variable}: \\$\\{${variable}:\\?`));
  }
  assert.match(compose, /DB_URL:\s*\$\{COMPOSE_DB_URL:-jdbc:postgresql:\/\//);
  assert.match(compose, /DB_PASSWORD:\s*\$\{DB_PASSWORD:\?/);
  assert.match(compose, /source: pethop-file-storage|pethop-file-storage:/);
});

test('production frontend configuration cannot emit a localhost API URL', () => {
  assert.match(api, /import\.meta\.env\?\.PROD && localApi/);
  assert.match(api, /productionFallback/);
  assert.match(viteConfig, /mode === 'production'/);
  assert.match(viteConfig, /import\.meta\.env\.VITE_API_URL/);
});
