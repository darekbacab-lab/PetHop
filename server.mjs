import { createReadStream } from 'node:fs';
import { access, stat } from 'node:fs/promises';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), 'dist');
const port = Number.parseInt(process.env.PORT || '4173', 10);

const contentTypes = {
  '.css': 'text/css; charset=utf-8',
  '.gif': 'image/gif',
  '.html': 'text/html; charset=utf-8',
  '.ico': 'image/x-icon',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2'
};
const spaHtmlRoutes = new Set([
  'acceso.html', 'admin.html', 'explorar.html', 'index.html', 'inicio.html',
  'mascota.html', 'mensajes.html', 'perfil.html', 'refugio.html', 'seguimiento.html', 'solicitud.html'
]);

function safePath(urlPath) {
  const decoded = decodeURIComponent(urlPath.split('?')[0]);
  const requested = path.resolve(root, `.${decoded === '/' ? '/index.html' : decoded}`);
  return requested.startsWith(`${root}${path.sep}`) ? requested : null;
}

async function resolveFile(urlPath) {
  const requested = safePath(urlPath);
  if (!requested) return null;
  try {
    const details = await stat(requested);
    if (details.isFile()) return requested;
  } catch {
    // The SPA fallback below handles known Vue routes when their shell file is absent.
  }
  if (path.extname(requested) && !spaHtmlRoutes.has(path.basename(requested).toLowerCase())) return null;
  const fallback = path.join(root, 'index.html');
  try {
    await access(fallback);
    return fallback;
  } catch {
    return null;
  }
}

const server = http.createServer(async (request, response) => {
  if (!['GET', 'HEAD'].includes(request.method)) {
    response.writeHead(405, { Allow: 'GET, HEAD' }).end();
    return;
  }

  let file;
  try {
    file = await resolveFile(request.url || '/');
  } catch {
    response.writeHead(400).end('Bad request');
    return;
  }
  if (!file) {
    response.writeHead(404).end('Not found');
    return;
  }

  const headers = { 'Content-Type': contentTypes[path.extname(file).toLowerCase()] || 'application/octet-stream' };
  if (path.basename(file) === 'index.html') headers['Cache-Control'] = 'no-cache';
  if (file.includes(`${path.sep}assets${path.sep}`)) headers['Cache-Control'] = 'public, max-age=31536000, immutable';
  if (request.method === 'HEAD') {
    response.writeHead(200, headers).end();
    return;
  }
  response.writeHead(200, headers);
  createReadStream(file).pipe(response);
});

server.listen(port, '0.0.0.0', () => {
  console.log(`PetHop frontend listening on port ${port}`);
});
