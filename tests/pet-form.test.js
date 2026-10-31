import test from 'node:test';
import assert from 'node:assert/strict';
import { mapPet } from '../src/services/petApi.js';
import { mergePhotoSelection, petPayload, togglePersonality } from '../src/services/petForm.js';

test('personalidad se alterna de forma independiente y admite varias características', () => {
  const first = togglePersonality([], 'Cariñosa');
  const both = togglePersonality(first, 'Convive con gatos');
  const removed = togglePersonality(both, 'Cariñosa');
  assert.deepEqual(first, ['Cariñosa']);
  assert.deepEqual(both, ['Cariñosa', 'Convive con gatos']);
  assert.deepEqual(removed, ['Convive con gatos']);
});

test('selecciones sucesivas de fotos se agregan, quitan duplicados y respetan el límite', () => {
  const first = { name: 'uno.jpg', size: 20, lastModified: 1 };
  const second = { name: 'dos.png', size: 30, lastModified: 2 };
  const appended = mergePhotoSelection(['uno.jpg\u000020\u00001'], [second, first]);
  assert.equal(appended.exceeded, false);
  assert.equal(appended.additions.length, 1);
  assert.equal(appended.additions[0].file, second);
  assert.equal(mergePhotoSelection(['a','b','c','d','e','f'], [second]).exceeded, true);
});

test('payload conserva ubicación, etapa y todos los apartados sin inventar edad exacta', () => {
  const payload = petPayload({
    name: 'Nube', species: 'Gato', lifeStage: 'Senior', gender: 'Hembra', location: 'Cozumel',
    personalityTraits: ['Tranquila', 'Convive con gatos'], description: 'Descripción real',
    story: 'Llegó por rescate', background: 'Antecedente real', homeType: 'Casa',
    conditions: 'Patio seguro', experience: 'Con experiencia', requirements: 'Ventanas protegidas',
    vaccination: 'Pendiente de confirmar', sterilization: 'No', deworming: 'Al día'
  });
  assert.equal(payload.location, 'Cozumel');
  assert.equal(payload.lifeStage, 'SENIOR');
  assert.equal(payload.ageValue, null);
  assert.equal(payload.vaccinated, null);
  assert.equal(payload.sterilized, false);
  assert.equal(payload.dewormed, true);
  assert.deepEqual(payload.personalityTraits, ['Tranquila', 'Convive con gatos']);
  assert.equal(payload.story, 'Llegó por rescate');
  assert.equal(payload.background, 'Antecedente real');
  assert.equal(payload.description, 'Descripción real');
  assert.equal(payload.homeType, 'Casa');
});

test('mapPet muestra etapa de vida real, rasgos, ubicación, booleanos y fotos reales', () => {
  const mapped = mapPet({
    id: 'p1', lifeStage: 'YOUNG', personalityTraits: ['Juguetona'], location: 'Cancún',
    vaccinated: null, sterilized: false, dewormed: true,
    images: [{ url: '/api/v1/pet-images/i1' }]
  });
  assert.equal(mapped.age, 'Joven');
  assert.equal(mapped.location, 'Cancún');
  assert.equal(mapped.vaccination, 'Pendiente de confirmar');
  assert.equal(mapped.sterilization, 'No');
  assert.equal(mapped.deworming, 'Sí');
  assert.deepEqual(mapped.personalityTraits, ['Juguetona']);
  assert.deepEqual(mapped.photos, ['http://localhost:8080/api/v1/pet-images/i1']);
  assert.equal(mapPet({}).age, 'No especificada');
});
