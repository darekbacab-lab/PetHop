// Solo guarda el borrador y las preferencias de la vista del refugio en este navegador.
// Mascotas, solicitudes, mensajes y perfiles viven en PostgreSQL a través de la API.
export const SHELTER_WORKSPACE_PREFIX = 'pethop_shelter_workspace';

export function shelterWorkspaceKey(userId) {
  const normalizedId = String(userId ?? '').trim();
  return normalizedId ? `${SHELTER_WORKSPACE_PREFIX}:${encodeURIComponent(normalizedId)}` : null;
}
