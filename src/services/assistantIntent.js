const veterinaryTerms = [
  /salud|enferm|sintom|dolor|vomit|diarre|estren|fiebr|herid|sangr|convulsion|respir|tos|cojera|comezon|picor/i,
  /vacun|desparas|esteriliz|aliment|comid|com[aeiouñ]*\s+(bien|mal|poco|nada)|apetit|no\s+esta\s+com|no\s+quiere\s+com/i,
  /higien|ban|lavar|asear|pelaje|piel|oido|unas|diente|comportamiento|ansiedad|estres|bienestar|cuidad|prevenc/i
];

export function isVeterinaryQuestion(text) {
  const normalized = String(text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  return veterinaryTerms.some(term => term.test(normalized));
}
