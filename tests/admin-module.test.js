import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const view = fs.readFileSync(new URL('../src/views/AdminView.vue', import.meta.url), 'utf8');
const router = fs.readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8');

test('el panel administrativo expone secciones con operaciones reales', () => {
  for (const section of ['Mascotas', 'Refugios', 'Usuarios', 'Reportes', 'Actividad']) assert.match(view, new RegExp(section));
  for (const endpoint of ['/admin/shelters/pending', '/admin/users', '/admin/reports', '/admin/audit']) assert.match(view, new RegExp(endpoint.replaceAll('/', '\\/')));
  assert.match(router, /path: '\/admin\.html'/);
});

test('la revisión de mascotas muestra datos reales, galería y checklist sin evidencia inventada', () => {
  for (const label of ['Identidad y características', 'Salud y cuidados', 'Historia y personalidad', 'Requisitos de adopción', 'Lista de revisión', 'Historial de moderación']) assert.match(view, new RegExp(label));
  assert.match(view, /selected\.images/);
  assert.match(view, /imageUrl\(image\)/);
  assert.match(view, /Esta publicación no tiene fotografías cargadas/);
  assert.match(view, /v-model="checklist\[item\[0\]\]"/);
  assert.match(view, /no están disponibles en este endpoint/);
  assert.doesNotMatch(view, /placeholder\.com|unsplash\.com|picsum\.photos/);
});

