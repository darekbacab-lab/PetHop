export const PROVIDER_WORKSPACE_PREFIX = 'pethop_provider_workspace';

export function providerWorkspaceKey(userId) {
  const normalizedId = String(userId ?? '').trim();
  return normalizedId ? `${PROVIDER_WORKSPACE_PREFIX}:${encodeURIComponent(normalizedId)}` : null;
}
