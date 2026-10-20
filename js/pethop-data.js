"use strict";

((root, factory) => {
    const api = factory();
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    if (root) root.PetHopData = api;
})(typeof globalThis !== "undefined" ? globalThis : this, () => {
    const PROVIDER_KEY = "pethop_provider_workspace";
    const APPLICATIONS_KEY = "pethop_applications";
    const MESSAGES_KEY = "pethop_messages";

    function read(storage, key, fallback) {
        try {
            const raw = storage.getItem(key);
            if (raw === null) return fallback;
            return JSON.parse(raw);
        } catch {
            return fallback;
        }
    }

    function write(storage, key, value) {
        try {
            storage.setItem(key, JSON.stringify(value));
            return true;
        } catch {
            return false;
        }
    }

    function workspace(storage) {
        const value = read(storage, PROVIDER_KEY, {});
        return value && typeof value === "object" && !Array.isArray(value) ? value : {};
    }

    function publishedPets(storage, excludedIds = []) {
        const excluded = new Set(excludedIds.map(id => String(id).trim().toLowerCase()));
        const pets = workspace(storage).pets;
        if (!Array.isArray(pets)) return [];
        return pets.filter(pet => pet && typeof pet === "object" && pet.id && pet.status === "Publicada" && !excluded.has(String(pet.id).trim().toLowerCase()));
    }

    function petDirectory(staticPets, storage) {
        const result = { ...staticPets };
        publishedPets(storage).forEach(pet => {
            result[String(pet.id).trim().toLowerCase()] = pet;
        });
        return result;
    }

    function messages(storage) {
        const value = read(storage, MESSAGES_KEY, []);
        if (!Array.isArray(value)) return [];
        return value.filter(thread => thread && typeof thread === "object" && thread.id && thread.petId).map(thread => ({
            ...thread,
            messages: Array.isArray(thread.messages) ? thread.messages.filter(message => message && typeof message === "object") : []
        }));
    }

    function upsertMessageThread(storage, thread) {
        if (!thread || !thread.id || !thread.petId) return false;
        const threads = messages(storage);
        const index = threads.findIndex(item => item.id === thread.id);
        if (index < 0) threads.unshift({ ...thread, messages: Array.isArray(thread.messages) ? thread.messages : [] });
        else threads[index] = { ...threads[index], ...thread, messages: Array.isArray(thread.messages) ? thread.messages : threads[index].messages };
        return write(storage, MESSAGES_KEY, threads);
    }

    function appendMessage(storage, threadId, message) {
        if (!threadId || !message || typeof message.text !== "string" || !message.text.trim()) return false;
        const threads = messages(storage);
        const thread = threads.find(item => item.id === threadId);
        if (!thread) return false;
        thread.messages.push({ ...message, text: message.text.trim() });
        thread.updatedAt = message.createdAt || new Date().toISOString();
        return write(storage, MESSAGES_KEY, threads);
    }

    function providerThreads(storage) {
        return messages(storage).map(thread => ({
            ...thread,
            adopter: thread.adopter || thread.ownerName || (thread.owner && thread.owner !== "visitante" ? thread.owner : "Visitante"),
            pet: thread.petName || thread.pet || "Mascota",
            status: thread.status || "Conversación",
            messages: thread.messages.map(message => ({
                ...message,
                from: message.sender === "adopter" ? "adopter" : "provider",
                date: message.createdAt || message.date || new Date().toISOString()
            }))
        }));
    }

    return { PROVIDER_KEY, APPLICATIONS_KEY, MESSAGES_KEY, read, write, workspace, publishedPets, petDirectory, messages, upsertMessageThread, appendMessage, providerThreads };
});
