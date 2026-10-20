"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const data = require("../js/pethop-data.js");

class MemoryStorage {
    #items = new Map();
    getItem(key) { return this.#items.has(key) ? this.#items.get(key) : null; }
    setItem(key, value) { this.#items.set(key, String(value)); }
    removeItem(key) { this.#items.delete(key); }
}

test("malformed browser storage falls back safely and storage failures are reported", () => {
    const storage = new MemoryStorage();
    storage.setItem(data.MESSAGES_KEY, "not-json");
    assert.deepEqual(data.messages(storage), []);

    const blockedStorage = { getItem() { throw new Error("blocked"); }, setItem() { throw new Error("quota"); } };
    assert.deepEqual(data.read(blockedStorage, data.MESSAGES_KEY, []), []);
    assert.equal(data.write(blockedStorage, data.MESSAGES_KEY, []), false);
});

test("only public provider pets enter the catalog and duplicates are excluded", () => {
    const storage = new MemoryStorage();
    data.write(storage, data.PROVIDER_KEY, { pets: [
        { id: "draft-1", name: "Borrador", status: "En revisión" },
        { id: "pub-1", name: "Sol", status: "Publicada", photos: ["data:image/jpeg;base64,AA=="] }
    ] });

    assert.deepEqual(data.publishedPets(storage, ["PUB-1"]).map(pet => pet.id), []);
    assert.deepEqual(data.publishedPets(storage).map(pet => pet.name), ["Sol"]);
    assert.equal(data.petDirectory({ chino: { id: "chino" } }, storage)["pub-1"].name, "Sol");
});

test("adopter and provider exchange messages in the same thread", () => {
    const storage = new MemoryStorage();
    const thread = {
        id: "adoptante@ejemplo.mx:sol-1",
        petId: "sol-1",
        petName: "Sol",
        owner: "adoptante@ejemplo.mx",
        adopter: "Adoptante",
        messages: [{ sender: "adopter", text: "¿Cómo se lleva con perros?", createdAt: "2026-10-13T12:00:00.000Z" }]
    };
    assert.equal(data.upsertMessageThread(storage, thread), true);
    const providerThread = data.providerThreads(storage)[0];
    assert.equal(providerThread.id, thread.id);
    assert.equal(providerThread.messages[0].from, "adopter");
    assert.equal(data.appendMessage(storage, thread.id, { sender: "provider", text: "Muy bien", createdAt: "2026-10-13T12:01:00.000Z" }), true);

    const roundTrip = data.messages(storage)[0];
    assert.equal(roundTrip.messages.length, 2);
    assert.equal(data.providerThreads(storage)[0].messages[1].from, "provider");
});

test("threads without a pet or a nonempty message cannot be written", () => {
    const storage = new MemoryStorage();
    assert.equal(data.upsertMessageThread(storage, { id: "broken" }), false);
    assert.equal(data.appendMessage(storage, "missing", { sender: "provider", text: "  " }), false);
});

test("every page using app.js loads the shared data module before it", () => {
    const projectRoot = path.resolve(__dirname, "..");
    for (const page of ["index.html", "explorar.html", "mascota.html", "proveedor.html", "seguimiento.html", "solicitud.html"]) {
        const html = fs.readFileSync(path.join(projectRoot, page), "utf8");
        const dataScript = html.indexOf('src="js/pethop-data.js');
        const appScript = html.indexOf('src="app.js');
        assert.ok(dataScript >= 0, `${page} must load the shared data module`);
        assert.ok(appScript > dataScript, `${page} must load the data module before app.js`);
        assert.ok(fs.existsSync(path.join(projectRoot, "js", "pethop-data.js")));
    }
});

test("local HTML links and script or stylesheet assets resolve", () => {
    const projectRoot = path.resolve(__dirname, "..");
    const pages = ["index.html", "explorar.html", "mascota.html", "proveedor.html", "seguimiento.html", "solicitud.html", "acceso.html"];
    const missing = [];
    for (const page of pages) {
        const html = fs.readFileSync(path.join(projectRoot, page), "utf8");
        for (const match of html.matchAll(/\b(?:href|src)\s*=\s*["']([^"']+)["']/gi)) {
            const value = match[1].trim();
            if (!value || /^(?:#|https?:|mailto:|tel:|javascript:|data:|\/\/)/i.test(value)) continue;
            const pathname = decodeURIComponent(value.split(/[?#]/, 1)[0]);
            if (!pathname) continue;
            if (!fs.existsSync(path.resolve(projectRoot, pathname))) missing.push(`${page} -> ${value}`);
        }
    }
    assert.deepEqual(missing, [], `Missing local references: ${missing.join(", ")}`);
});

test("main stylesheet has balanced rule blocks", () => {
    const css = fs.readFileSync(path.resolve(__dirname, "../css/styles.css"), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/g, "");
    let depth = 0;
    for (const character of css) {
        if (character === "{") depth += 1;
        if (character === "}") depth -= 1;
        assert.ok(depth >= 0, "CSS contains an unexpected closing brace");
    }
    assert.equal(depth, 0, "CSS has unclosed rule blocks");
});
