export const PERSONALITY_OPTIONS = [
  'Cariñosa', 'Tranquila', 'Juguetona', 'Sociable', 'Independiente',
  'Convive con perros', 'Convive con gatos', 'Convive con niños'
];

const lifeStageValues = { Cachorro: 'PUPPY', Joven: 'YOUNG', Adulto: 'ADULT', Senior: 'SENIOR' };
const sexValues = { Hembra: 'FEMALE', Macho: 'MALE', 'No especificado': 'UNKNOWN' };
const sizeValues = { Pequeño: 'SMALL', Mediano: 'MEDIUM', Grande: 'LARGE' };

export function normalizePersonality(value) {
  const candidates = Array.isArray(value) ? value : typeof value === 'string' ? value.split(',') : [];
  return [...new Set(candidates.map(item => String(item).trim()).filter(item => item && item.toLowerCase() !== 'true' && item.toLowerCase() !== 'false'))];
}

export function togglePersonality(traits, trait) {
  const current = normalizePersonality(traits);
  return current.includes(trait) ? current.filter(item => item !== trait) : [...current, trait];
}

export function mergePhotoSelection(selectedKeys, files, limit = 6) {
  const keys = [...selectedKeys];
  const seen = new Set(keys);
  const additions = [];
  let duplicates = 0;
  for (const file of files) {
    const key = `${file.name}\u0000${file.size}\u0000${file.lastModified}`;
    if (seen.has(key)) { duplicates += 1; continue; }
    seen.add(key);
    additions.push({ file, key });
  }
  if (keys.length + additions.length > limit) return { additions: [], duplicates, exceeded: true };
  return { additions, duplicates, exceeded: false };
}

const booleanStatus = value => value === 'Sí' || value === 'Al día' ? true : value === 'No' || value === 'No vacunada' ? false : null;

export function petPayload(form) {
  const traits = normalizePersonality(form.personalityTraits ?? form.personality);
  const stageLabel = form.lifeStage || form.age;
  const weight = form.weight === '' || form.weight == null ? null : Number(form.weight);
  return {
    name: String(form.name || '').trim(),
    species: ({ Perro: 'DOG', Gato: 'CAT', Otro: 'OTHER' })[form.species] || form.species,
    breed: String(form.breed || '').trim() || null,
    sex: sexValues[form.gender] || form.sex || 'UNKNOWN',
    ageValue: form.ageValue ?? null,
    ageUnit: form.ageUnit ?? null,
    lifeStage: lifeStageValues[stageLabel] || stageLabel || null,
    size: sizeValues[form.size] || form.size || null,
    weight: Number.isFinite(weight) && weight > 0 ? weight : null,
    color: String(form.color || '').trim() || null,
    description: String(form.description || '').trim(),
    personality: traits.join(', '),
    personalityTraits: traits,
    healthSummary: String(form.healthNotes || form.healthSummary || '').trim() || null,
    healthCondition: String(form.condition || form.healthCondition || '').trim() || null,
    story: String(form.story || '').trim() || null,
    background: String(form.background || '').trim() || null,
    homeType: String(form.homeType || '').trim() || null,
    conditions: String(form.conditions || '').trim() || null,
    experience: String(form.experience || '').trim() || null,
    requirements: String(form.requirements || '').trim() || null,
    sterilized: booleanStatus(form.sterilization),
    vaccinated: booleanStatus(form.vaccination),
    dewormed: booleanStatus(form.deworming),
    location: String(form.location || '').trim() || null,
    adoptionStatus: form.adoptionStatus || 'AVAILABLE'
  };
}
