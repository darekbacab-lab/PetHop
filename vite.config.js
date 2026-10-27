import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const root = path.dirname(fileURLToPath(import.meta.url));
const pages = ['index', 'acceso', 'explorar', 'mascota', 'solicitud', 'seguimiento', 'mensajes', 'proveedor'];

async function filesIn(directory) {
  const result = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) result.push(...await filesIn(absolute));
    else result.push(absolute);
  }
  return result;
}

function legacyPublicAssets() {
  return {
    name: 'pethop-legacy-public-assets',
    apply: 'build',
    async generateBundle() {
      for (const source of await filesIn(path.join(root, 'img'))) {
          this.emitFile({
            type: 'asset',
            fileName: path.relative(root, source).replaceAll(path.sep, '/'),
            source: await readFile(source)
          });
      }
    }
  };
}

export default defineConfig({
  plugins: [vue(), legacyPublicAssets()],
  build: {
    rollupOptions: {
      input: Object.fromEntries(pages.map(page => [page, path.join(root, `${page}.html`)]))
    }
  }
});
