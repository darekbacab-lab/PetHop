"use strict";

const STORAGE_KEYS = {
    favorites: "pethop_favorites",
    applications: "pethop_applications",
    followUps: "pethop_followups",
    user: "pethop_user",
    messages: "pethop_messages"
};

const PET_DIRECTORY = {
    chino: { id: "chino", name: "Chino", species: "Perro", age: "Adulto", gender: "Macho", size: "Mediano", location: "Playa del Carmen" },
    luna: { id: "luna", name: "Luna", species: "Perro", age: "Joven", gender: "Hembra", size: "Mediano", location: "Playa del Carmen" },
    milo: { id: "milo", name: "Milo", species: "Gato", age: "Joven", gender: "Macho", size: "Pequeño", location: "Cancún" },
    nala: { id: "nala", name: "Nala", species: "Gato", age: "Adulto", gender: "Hembra", size: "Pequeño", location: "Cozumel" },
    max: { id: "max", name: "Max", species: "Perro", age: "Senior", gender: "Macho", size: "Grande", location: "Cancún" },
    kira: { id: "kira", name: "Kira", species: "Perro", age: "Cachorro", gender: "Hembra", size: "Pequeño", location: "Playa del Carmen" }
};

const PET_GALLERY_PHOTOS = {
    perro: ["img2.jpeg", "img3.jpeg", "img4.jpeg", "img7.jpeg"],
    gato: ["img5.jpg", "img6.jpg"]
};
let modalReturnFocus = null;

function getPetDirectory() {
    return window.PetHopData?.petDirectory(PET_DIRECTORY, localStorage) || PET_DIRECTORY;
}

function safePhotoData(value) {
    return typeof value === "string" && /^data:image\/(?:jpeg|webp|png);base64,[a-z0-9+/=]+$/i.test(value) ? value : "";
}

function initializePublishedPets() {
    const grid = document.querySelector("#catalogGrid");
    if (!grid || !window.PetHopData) return;

    const existingIds = [...grid.querySelectorAll("[data-pet-id], [data-favorite]")]
        .map(element => String(element.dataset.petId || "").toLowerCase()).filter(Boolean);
    const template = grid.querySelector(".catalog-card");
    if (!template) return;

    window.PetHopData.publishedPets(localStorage, existingIds).forEach(pet => {
        const card = template.cloneNode(true);
        const petId = String(pet.id).toLowerCase();
        card.dataset.name = pet.name || "Mascota";
        card.dataset.species = pet.species || "Otro";
        card.dataset.age = pet.age || "";
        card.dataset.gender = pet.gender || "";
        card.dataset.size = pet.size || "";
        card.dataset.location = pet.location || "Ubicación por confirmar";
        card.dataset.description = pet.description || pet.story || "Consulta con la organización para conocer más sobre esta mascota.";
        const favorite = card.querySelector("[data-favorite], [data-pet-id]");
        if (favorite) {
            favorite.dataset.petId = petId;
            favorite.setAttribute("aria-label", `Agregar ${pet.name || "mascota"} a favoritos`);
        }
        const image = card.querySelector(".catalog-card-image");
        if (image) {
            image.dataset.photoSlot = "";
            const photo = safePhotoData(pet.photos?.[0]);
            image.style.backgroundImage = photo ? `url("${photo}")` : "none";
            image.style.backgroundColor = photo ? "transparent" : "var(--color-surface-soft, #e7f4f4)";
        }
        const heading = card.querySelector("h3");
        if (heading) heading.textContent = pet.name || "Mascota";
        const species = card.querySelector(".pet-species");
        if (species) {
            const speciesIcon = species.querySelector("i");
            species.replaceChildren();
            if (speciesIcon) species.append(speciesIcon);
            species.append(document.createTextNode((pet.species || "Otro").toLocaleUpperCase("es-MX")));
        }
        const details = card.querySelectorAll(".catalog-details span");
        [pet.age, pet.gender, pet.size].forEach((value, index) => { if (details[index]) details[index].textContent = value || "Por confirmar"; });
        const location = card.querySelector(".catalog-location");
        if (location) {
            const pin = location.querySelector("i");
            location.replaceChildren();
            if (pin) location.append(pin);
            location.append(document.createTextNode(` ${pet.location || "Ubicación por confirmar"}`));
        }
        const availability = card.querySelector(".available");
        if (availability) availability.textContent = "Disponible";
        grid.append(card);
    });
}

const PetHop = {
    get(key, fallback = null) {
        try {
            const value = localStorage.getItem(key);
            return value === null ? fallback : JSON.parse(value);
        } catch {
            return fallback;
        }
    },

    set(key, value) {
        try {
            localStorage.setItem(key, JSON.stringify(value));
            return true;
        } catch {
            return false;
        }
    },

    remove(key) {
        try {
            localStorage.removeItem(key);
        } catch {}
    }
};

function normalizeText(value) {
    return String(value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .trim();
}

function escapeHTML(value) {
    const div = document.createElement("div");
    div.textContent = String(value ?? "");
    return div.innerHTML;
}

function getCurrentPage() {
    const path = window.location.pathname.toLowerCase();

    if (path.includes("explorar")) {
        return "explorar";
    }

    if (path.includes("mascota")) {
        return "mascota";
    }

    if (path.includes("solicitud")) {
        return "solicitud";
    }

    if (path.includes("seguimiento")) {
        return "seguimiento";
    }

    if (path.includes("proveedor")) {
        return "proveedor";
    }

    return "inicio";
}

function showToast(message, type = "normal") {
    let container = document.querySelector(".toast-container");

    if (!container) {
        container = document.createElement("div");
        container.className = "toast-container";
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = "toast";

    if (type === "success") {
        toast.style.borderColor = "#7BBDAA";
    }

    if (type === "error") {
        toast.style.borderColor = "var(--danger)";
    }

    toast.textContent = message;
    container.appendChild(toast);

    window.setTimeout(() => {
        toast.classList.add("hide");

        window.setTimeout(() => {
            toast.remove();

            if (!container.children.length) {
                container.remove();
            }
        }, 250);
    }, 3000);
}

function createModal(content) {
    const existing = document.querySelector(".dynamic-modal");
    const previouslyFocused = existing ? modalReturnFocus : document.activeElement;

    if (!existing) modalReturnFocus = previouslyFocused;
    if (existing) {
        existing.remove();
    }

    const modal = document.createElement("div");
    modal.className = "dynamic-modal";
    modal.setAttribute("role", "presentation");

    modal.innerHTML = `
        <div class="dynamic-modal-content" role="dialog" aria-modal="true" aria-labelledby="dynamic-modal-title" tabindex="-1">
            <button type="button" class="dynamic-modal-close" aria-label="Cerrar"><i data-lucide="x" aria-hidden="true"></i></button>
            ${content}
        </div>
    `;

    document.body.appendChild(modal);
    if (window.lucide?.createIcons) window.lucide.createIcons({ root: modal });
    initializeButtonEffects(modal);

    const dialog = modal.querySelector('[role="dialog"]');
    const focusableSelector = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    requestAnimationFrame(() => {
        modal.classList.add("open");
        (modal.querySelector(focusableSelector) || dialog)?.focus();
    });

    let escapeHandler;
    const close = () => {
        modal.classList.remove("open");
        if (escapeHandler) document.removeEventListener("keydown", escapeHandler);
        if (previouslyFocused instanceof HTMLElement && previouslyFocused.isConnected) {
            previouslyFocused.focus();
        }
        modalReturnFocus = null;

        window.setTimeout(() => {
            modal.remove();
        }, 250);
    };

    const title = modal.querySelector(".dynamic-modal-content h2");
    if (title) title.id = "dynamic-modal-title";

    modal.querySelector(".dynamic-modal-close").addEventListener("click", close);

    modal.addEventListener("click", event => {
        if (event.target === modal) {
            close();
        }
    });

    modal.addEventListener("keydown", event => {
        if (event.key !== "Tab") return;
        const focusable = Array.from(modal.querySelectorAll(focusableSelector))
            .filter(element => element.getClientRects().length);
        if (!focusable.length) {
            event.preventDefault();
            dialog?.focus();
            return;
        }
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });

    escapeHandler = event => {
        if (event.key === "Escape") close();
    };
    document.addEventListener("keydown", escapeHandler);

    return modal;
}

function getFavorites() {
    const favorites = PetHop.get(STORAGE_KEYS.favorites, []);
    if (!Array.isArray(favorites)) return [];
    return [...new Set(favorites.map(value => String(value).trim().toLowerCase()).filter(Boolean))];
}

function saveFavorites(favorites) {
    return PetHop.set(STORAGE_KEYS.favorites, favorites);
}

function isFavorite(petId) {
    return getFavorites().includes(String(petId).trim().toLowerCase());
}

function updateFavoritesCount() {
    const count = getFavorites().length;

    document.querySelectorAll(".favorites-count").forEach(element => {
        element.textContent = count;
    });

    document.querySelectorAll(".favorites-button").forEach(button => {
        const countElement = button.querySelector(".favorites-count");

        if (countElement) {
            countElement.textContent = count;
        }
        button.setAttribute("aria-label", `Favoritos, ${count} ${count === 1 ? "mascota" : "mascotas"}`);
    });
}

function updateFavoriteButtons() {
    document.querySelectorAll("[data-favorite]").forEach(button => {
        const petId = button.dataset.petId || button.dataset.favorite;
        const active = isFavorite(petId);
        const petName = getPetDataFromElement(button).name || "mascota";

        button.classList.toggle("active", active);
        button.setAttribute("aria-pressed", String(active));
        button.setAttribute("aria-label", `${active ? "Quitar" : "Agregar"} ${petName} ${active ? "de" : "a"} favoritos`);


    });

    updateFavoritesCount();
}

function toggleFavorite(petId) {
    const id = String(petId).trim().toLowerCase();
    const favorites = getFavorites();
    const index = favorites.indexOf(id);

    if (index === -1) {
        favorites.push(id);
        if (!saveFavorites(favorites)) {
            showToast("No se pudieron guardar los favoritos en este navegador.", "error");
            return;
        }
        showToast("Mascota agregada a favoritos.", "success");
    } else {
        favorites.splice(index, 1);
        if (!saveFavorites(favorites)) {
            showToast("No se pudieron guardar los favoritos en este navegador.", "error");
            return;
        }
        showToast("Mascota eliminada de favoritos.");
    }

    updateFavoriteButtons();
}

function getPetDataFromElement(element) {
    const card = element.closest(".pet-card, .catalog-card, .pet-profile") || element.closest("[data-pet-id]");

    if (!card) {
        return {
            id: element.dataset.petId || "",
            name: element.dataset.petName || ""
        };
    }

    return {
        id:
            card.dataset.petId ||
            card.querySelector("[data-pet-id]")?.dataset.petId ||
            element.dataset.petId ||
            "",
        name:
            card.dataset.petName ||
            element.dataset.petName ||
            card.querySelector(
                ".pet-title h3, .catalog-card-top h3, .pet-profile-heading h1"
            )?.textContent.trim() ||
            ""
    };
}

function initializeFavorites() {
    document.addEventListener("click", event => {
        const button = event.target.closest("[data-favorite]");

        if (!button) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const data = getPetDataFromElement(button);
        const id = data.id || data.name;

        if (id) {
            toggleFavorite(id);
        }
    });

    document.querySelectorAll("[data-favorite]").forEach(button => {
        if (!button.dataset.petId) {
            const data = getPetDataFromElement(button);

            if (data.id) {
                button.dataset.petId = data.id;
            }
        }
    });

    updateFavoriteButtons();

    window.addEventListener("storage", event => {
        if (event.key === STORAGE_KEYS.favorites || event.key === null) updateFavoriteButtons();
    });
}

function initializeButtonEffects(root = document) {
    root.querySelectorAll(
        "button, .primary-button, .secondary-button, .pet-apply-button, .application-submit, .application-back-button, .organization-button, .message-shelter-button, .pet-button, .button, .catalog-apply, .filter-button, .search-button, .login-button, .how-button, .provider-top-cta"
    ).forEach(control => {
        if (!control.closest(".action-card")) control.classList.add("ui-button");
    });
}

function initializeFavoritesMenu() {
    document.addEventListener("click", event => {
        const button = event.target.closest(".favorites-button");

        if (!button) {
            return;
        }

        event.preventDefault();

        const favorites = getFavorites();

        if (!favorites.length) {
            createModal(`
                <h2>Favoritos</h2>
                <p>Aún no has guardado ninguna mascota.</p>
                <a href="explorar.html" class="primary-button">
                    Explorar mascotas
                    
                </a>
            `);

            return;
        }

        const cards = favorites
            .map(id => {
                const pet = getPetDirectory()[id];
                if (!pet) return "";
                return `
                <div class="dynamic-modal-list-item">
                    <div>
                        <strong>${escapeHTML(pet.name)}</strong>
                        <span>Guardada en favoritos</span>
                    </div>
                    <a
                        href="mascota.html?pet=${encodeURIComponent(id)}"
                        class="pet-button"
                    >
                        Ver perfil
                    </a>
                </div>
            `;
            })
            .filter(Boolean);

        if (!cards.length) {
            createModal(`
                <h2>Favoritos</h2>
                <p>Las mascotas guardadas ya no están disponibles en este catálogo.</p>
                <a href="explorar.html" class="primary-button">Explorar mascotas</a>
            `);

            return;
        }

        createModal(`
            <h2>Favoritos</h2>
            <p>Tus mascotas guardadas.</p>
            <div class="dynamic-modal-list">
                ${cards.join("")}
            </div>
        `);
    });
}

function initializeHomeSearch() {
    const searchForms = document.querySelectorAll(
        ".search-box, [data-search-form]"
    );

    searchForms.forEach(form => {
        form.addEventListener("submit", event => {
            event.preventDefault();

            const input =
                form.querySelector("input") ||
                document.querySelector("#searchInput");

            const value = input?.value.trim() || "";

            if (!value) {
                window.location.href = "explorar.html";
                return;
            }

            window.location.href =
                "explorar.html?search=" +
                encodeURIComponent(value);
        });
    });

    const heroSearch = document.querySelector(
        '[data-home-search], #homeSearch'
    );

    if (heroSearch) {
        heroSearch.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();

                const value = heroSearch.value.trim();

                window.location.href =
                    "explorar.html?search=" +
                    encodeURIComponent(value);
            }
        });
    }
}

function initializeCatalog() {
    const catalog = document.querySelector(".catalog-section");

    if (!catalog) {
        return;
    }

    const searchInput =
        document.querySelector("#catalogSearch") ||
        document.querySelector("#searchInput") ||
        document.querySelector('.catalog-search input[type="search"]') ||
        document.querySelector(
            '.search-box input[type="search"], .search-box input'
        );

    const searchButton = document.querySelector(
        ".search-button"
    );

    const cards = Array.from(
        document.querySelectorAll(
            ".catalog-card, .pet-card[data-pet-id]"
        )
    );

    const speciesSelect =
        document.querySelector("#speciesFilter") ||
        document.querySelector(
            '.catalog-filter-group select[name="species"], select[data-filter="species"]'
        );

    const ageSelect =
        document.querySelector("#ageFilter") ||
        document.querySelector(
            '.catalog-filter-group select[name="age"], select[data-filter="age"]'
        );

    const sexSelect =
        document.querySelector("#genderFilter") ||
        document.querySelector("#sexFilter") ||
        document.querySelector(
            '.catalog-filter-group select[name="sex"], select[data-filter="sex"]'
        );

    const sizeSelect =
        document.querySelector("#sizeFilter") ||
        document.querySelector(
            '.catalog-filter-group select[name="size"], select[data-filter="size"]'
        );

    const locationSelect =
        document.querySelector("#locationFilter") ||
        document.querySelector(
            '.catalog-filter-group select[name="location"], select[data-filter="location"]'
        );

    const applyButton =
        document.querySelector(".filter-button") ||
        document.querySelector(".catalog-apply") ||
        document.querySelector("#applyFilters") ||
        document.querySelector("[data-apply-filters]");

    const clearButton =
        document.querySelector(".clear-filters") ||
        document.querySelector("[data-clear-filters]");

    const sortSelect =
        document.querySelector("#sortPets") ||
        document.querySelector(".sort-control select");

    const countElement =
        document.querySelector("#resultsCount, .results-count strong") ||
        document.querySelector("[data-results-count]");

    const emptyState =
        document.querySelector("#catalogEmpty, .catalog-empty");

    const initialSearch = new URLSearchParams(window.location.search).get("search");
    if (searchInput && initialSearch) searchInput.value = initialSearch;
    countElement?.parentElement?.setAttribute("aria-live", "polite");

    const getCardValue = (card, names) => {
        for (const name of names) {
            if (card.dataset[name]) {
                return normalizeText(card.dataset[name]);
            }
        }

        return normalizeText(card.textContent);
    };

    const filterCards = () => {
        const search = normalizeText(searchInput?.value);
        const species = normalizeText(speciesSelect?.value);
        const age = normalizeText(ageSelect?.value);
        const sex = normalizeText(sexSelect?.value);
        const size = normalizeText(sizeSelect?.value);
        const location = normalizeText(locationSelect?.value);

        let visibleCards = cards.filter(card => {
            const text = normalizeText(card.textContent);

            const matchesSearch =
                !search ||
                text.includes(search) ||
                getCardValue(card, ["petName", "name"]).includes(search);

            const matchesSpecies =
                !species ||
                species === "todas" ||
                species === "todos" ||
                getCardValue(card, ["species", "especie"]).includes(species);

            const matchesAge =
                !age ||
                age === "todas" ||
                age === "todos" ||
                getCardValue(card, ["age", "edad"]).includes(age);

            const matchesSex =
                !sex ||
                sex === "todas" ||
                sex === "todos" ||
                getCardValue(card, ["gender", "sex", "sexo"]).includes(sex);

            const matchesSize =
                !size ||
                size === "todas" ||
                size === "todos" ||
                getCardValue(card, ["size", "tamano"]).includes(size);

            const matchesLocation =
                !location ||
                location === "todas" ||
                location === "todos" ||
                getCardValue(card, ["location", "ubicacion"]).includes(location);

            return (
                matchesSearch &&
                matchesSpecies &&
                matchesAge &&
                matchesSex &&
                matchesSize &&
                matchesLocation
            );
        });

        cards.forEach(card => {
            card.style.display = "none";
        });

        visibleCards.forEach(card => {
            card.style.display = "";
        });

        if (countElement) {
            countElement.textContent = visibleCards.length;
            const countText = countElement.nextSibling;
            if (countText?.nodeType === Node.TEXT_NODE) {
                countText.textContent = visibleCards.length === 1
                    ? " mascota disponible"
                    : " mascotas disponibles";
            }
        }

        if (emptyState) {
            emptyState.style.display =
                visibleCards.length === 0 ? "block" : "none";
        }

        return visibleCards;
    };

    const sortCards = visibleCards => {
        if (!sortSelect) {
            return;
        }

        const value = normalizeText(sortSelect.value);

        if (value === "name" || value === "nombre" || value.includes("nombre")) {
            visibleCards.sort((a, b) => {
                const nameA =
                    a.dataset.petName ||
                    a.querySelector("h3")?.textContent ||
                    "";

                const nameB =
                    b.dataset.petName ||
                    b.querySelector("h3")?.textContent ||
                    "";

                return nameA.localeCompare(nameB, "es");
            });
        }

        if (value === "age" || value === "edad") {
            visibleCards.sort((a, b) => {
                const ageRank = age => ({ cachorro: 0, joven: 1, adulto: 2, senior: 3 })[normalizeText(age)] ?? 99;
                const ageA = ageRank(a.dataset.age);
                const ageB = ageRank(b.dataset.age);

                return ageA - ageB;
            });
        }

        const grid =
            document.querySelector(".catalog-grid") ||
            document.querySelector(".pets-grid");

        if (grid) {
            visibleCards.forEach(card => {
                grid.appendChild(card);
            });
        }
    };

    const executeFilters = () => {
        const visible = filterCards();
        sortCards(visible);
    };

    if (searchInput) {
        searchInput.addEventListener("input", executeFilters);
    }

    if (searchButton) {
        searchButton.addEventListener("click", event => {
            event.preventDefault();
            executeFilters();
        });
    }

    searchInput?.closest("form")?.addEventListener("submit", event => {
        event.preventDefault();
        executeFilters();
    });

    [
        speciesSelect,
        ageSelect,
        sexSelect,
        sizeSelect,
        locationSelect
    ].forEach(select => {
        if (select) {
            select.addEventListener("change", executeFilters);
        }
    });

    if (applyButton) {
        applyButton.addEventListener("click", event => {
            event.preventDefault();
            executeFilters();
        });
    }

    if (clearButton) {
        clearButton.addEventListener("click", event => {
            event.preventDefault();

            if (searchInput) {
                searchInput.value = "";
            }

            [
                speciesSelect,
                ageSelect,
                sexSelect,
                sizeSelect,
                locationSelect
            ].forEach(select => {
                if (select) {
                    select.selectedIndex = 0;
                }
            });

            executeFilters();
            showToast("Filtros limpiados.");
        });
    }

    if (sortSelect) {
        sortSelect.addEventListener("change", executeFilters);
    }

    executeFilters();
}

function initializeGallery() {
    const track = document.querySelector(".gallery-track");

    if (!track) {
        return;
    }

    const requestedPet = new URLSearchParams(window.location.search).get("pet") || "chino";
    const requestedPetId = requestedPet.toLowerCase();
    let selectedPet = null;
    try {
        selectedPet = JSON.parse(sessionStorage.getItem("pethop_selected_pet") || "null");
    } catch {}
    if (selectedPet?.id !== requestedPetId) selectedPet = null;
    const pet = { ...getPetDirectory()[requestedPetId], ...(selectedPet || {}) };
    if (!pet.id) Object.assign(pet, PET_DIRECTORY.chino);
    const photos = Array.isArray(pet.photos) && pet.photos.length
        ? pet.photos
        : PET_GALLERY_PHOTOS[normalizeText(pet.species)] || PET_GALLERY_PHOTOS.perro;
    const slides = photos.map((photo, index) => {
        const slide = document.createElement("div");
        slide.className = "gallery-card";
        slide.dataset.slide = String(index);
        slide.setAttribute("role", "group");
        slide.setAttribute("aria-roledescription", "diapositiva");

        const image = document.createElement("img");
        image.className = "gallery-slide-image";
        image.src = photo.startsWith("data:") ? photo : `img/${photo}`;
        image.alt = pet.photos?.length ? `Fotografía ${index + 1} de ${pet.name}.` : `Imagen de referencia ${index + 1} de ${pet.name}. Confirma con el refugio si corresponde a esta mascota.`;
        image.loading = index === 0 ? "eager" : "lazy";
        slide.appendChild(image);
        return slide;
    });
    track.replaceChildren(...slides);

    const lastClone = slides[slides.length - 1].cloneNode(true);
    const firstClone = slides[0].cloneNode(true);
    lastClone.classList.add("gallery-clone");
    firstClone.classList.add("gallery-clone");
    lastClone.setAttribute("aria-hidden", "true");
    firstClone.setAttribute("aria-hidden", "true");
    track.insertBefore(lastClone, slides[0]);
    track.appendChild(firstClone);
    const renderedSlides = [lastClone, ...slides, firstClone];

    const gallery = track.closest(".pet-gallery") || track.parentElement;
    const previousButton = gallery.querySelector(".gallery-prev");
    const nextButton = gallery.querySelector(".gallery-next");

    let current = 1;
    let touchStartX = 0;
    let touchEndX = 0;
    let isAnimating = false;
    let animationFallback;

    const finishTransition = () => {
        if (!isAnimating) return;
        window.clearTimeout(animationFallback);

        if (current === slides.length + 1 || current === 0) {
            current = current === slides.length + 1 ? 1 : slides.length;
            track.style.transition = "none";
            updateGallery();
            track.offsetHeight;
            track.style.transition = "";
        }

        isAnimating = false;
        if (previousButton) previousButton.disabled = false;
        if (nextButton) nextButton.disabled = false;
    };

    track.addEventListener("transitionend", event => {
        if (event.target === track && event.propertyName === "transform") {
            finishTransition();
        }
    });

    const updateGallery = () => {
        const activeIndex = (current - 1 + slides.length) % slides.length;
        track.dataset.currentSlide = String(activeIndex);
        track.style.transform = `translateX(-${current * 100}%)`;
        renderedSlides.forEach((slide, index) => {
            slide.classList.toggle("gallery-card-center", index === current);
            slide.setAttribute("aria-hidden", String(index !== current));
            const slideNumber = index === 0
                ? slides.length
                : index === renderedSlides.length - 1
                    ? 1
                    : index;
            slide.setAttribute("aria-label", `Imagen ${slideNumber} de ${slides.length}`);
        });
    };

    const goTo = nextSlide => {
        if (isAnimating || nextSlide === current) return;
        isAnimating = true;
        if (previousButton) previousButton.disabled = true;
        if (nextButton) nextButton.disabled = true;
        current = nextSlide;
        updateGallery();
        animationFallback = window.setTimeout(finishTransition, 500);
    };

    const next = () => {
        goTo(current + 1);
    };

    const previous = () => {
        goTo(current - 1);
    };

    previousButton?.addEventListener("click", previous);
    nextButton?.addEventListener("click", next);

    track.addEventListener(
        "touchstart",
        event => {
            touchStartX = event.changedTouches[0].clientX;
        },
        { passive: true }
    );

    track.addEventListener(
        "touchend",
        event => {
            touchEndX = event.changedTouches[0].clientX;

            const difference =
                touchStartX - touchEndX;

            if (Math.abs(difference) < 45) {
                return;
            }

            if (difference > 0) {
                next();
            } else {
                previous();
            }
        },
        { passive: true }
    );

    document.addEventListener("keydown", event => {
        if (!track.closest(".pet-gallery")?.matches(":hover, :focus-within")) return;
        if (event.key === "ArrowRight") {
            event.preventDefault();
            next();
        }

        if (event.key === "ArrowLeft") {
            event.preventDefault();
            previous();
        }
    });

    track.style.transition = "none";
    updateGallery();
    track.offsetHeight;
    track.style.transition = "";
}

function initializeAdoptionForm() {
    const form =
        document.querySelector("#adoptionForm") ||
        document.querySelector("form[data-adoption-form]");

    if (!form) {
        return;
    }

    const dateInputs = form.querySelectorAll(
        'input[type="date"]'
    );

    const today = new Date();
    const localDate =
        today.getFullYear() +
        "-" +
        String(today.getMonth() + 1).padStart(2, "0") +
        "-" +
        String(today.getDate()).padStart(2, "0");

    dateInputs.forEach(input => {
        if (!input.min) {
            input.min = localDate;
        }
    });

    form.addEventListener("submit", event => {
        event.preventDefault();

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const data = Object.fromEntries(
            new FormData(form).entries()
        );

        const petId =
            form.dataset.petId ||
            new URLSearchParams(window.location.search).get("pet") ||
            "chino";

        const storedApplications = PetHop.get(
            STORAGE_KEYS.applications,
            []
        );
        const applications = Array.isArray(storedApplications)
            ? storedApplications.filter(item => item && typeof item === "object")
            : [];

        const application = {
            id:
                "APP-" +
                Date.now().toString(36).toUpperCase(),
            petId: String(petId),
            status: "Recibida",
            date: new Date().toISOString(),
            data
        };

        applications.push(application);

        if (!PetHop.set(STORAGE_KEYS.applications, applications)) {
            showToast("No se pudo guardar la solicitud en este dispositivo.", "error");
            return;
        }

        const user = {
            name: data.nombre || data.name || data.fullName || "",
            email: data.correo || data.email || "",
            phone: data.telefono || data.phone || ""
        };

        PetHop.set(STORAGE_KEYS.user, user);

        showToast(
            "Solicitud guardada en este dispositivo. El refugio aún debe confirmar su recepción.",
            "success"
        );

        window.setTimeout(() => {
            window.location.href =
                "seguimiento.html?application=" +
                encodeURIComponent(application.id);
        }, 900);
    });
}

function initializeFollowUpForm() {
    const form =
        document.querySelector("#followUpForm") ||
        document.querySelector("form[data-follow-up-form]");

    const historyContainer =
        document.querySelector(".follow-up-list") ||
        document.querySelector("[data-follow-up-history]");

    const renderHistory = () => {
        if (!historyContainer) {
            return;
        }

        const storedFollowUps = PetHop.get(STORAGE_KEYS.followUps, []);
        const followUps = Array.isArray(storedFollowUps)
            ? storedFollowUps.filter(item => item && typeof item === "object")
            : [];

        if (!followUps.length) {
            historyContainer.innerHTML = `
                <div class="follow-up-empty">
                    Aún no has registrado actualizaciones.
                </div>
            `;
            return;
        }

        const ordered = [...followUps].reverse();
        const labels = {
            bien: "Todo marcha bien",
            adaptacion: "En proceso de adaptación",
            atencion: "Requiere atención"
        };

        historyContainer.innerHTML = ordered
            .map(item => {
                const date = new Date(item.date);

                return `
                    <article class="follow-up-record-card">
                        <div class="follow-up-record-header">
                            <strong>${escapeHTML(
                                labels[item.status] || item.status || "Actualización personal"
                            )}</strong>
                            <span>${date.toLocaleDateString(
                                "es-MX"
                            )}</span>
                        </div>
                        <div class="summary-row">
                            <span>Bienestar</span>
                            <strong>${escapeHTML(
                                item.wellbeing || "Sin especificar"
                            )}</strong>
                        </div>
                        ${
                            item.notes
                                ? `
                                    <p class="follow-up-note">
                                        ${escapeHTML(item.notes)}
                                    </p>
                                `
                                : ""
                        }
                    </article>
                `;
            })
            .join("");
    };

    renderHistory();

    if (!form) {
        return;
    }

    form.addEventListener("submit", event => {
        event.preventDefault();

        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const data = Object.fromEntries(
            new FormData(form).entries()
        );

        const storedFollowUps = PetHop.get(
            STORAGE_KEYS.followUps,
            []
        );
        const followUps = Array.isArray(storedFollowUps)
            ? storedFollowUps.filter(item => item && typeof item === "object")
            : [];

        followUps.push({
            id:
                "FOLLOW-" +
                Date.now().toString(36).toUpperCase(),
            date: new Date().toISOString(),
            status:
                data.estado ||
                data.status ||
                "Actualización",
            wellbeing:
                data.bienestar ||
                data.wellbeing ||
                "Sin especificar",
            notes:
                data.notas ||
                data.notes ||
                ""
        });

        if (!PetHop.set(STORAGE_KEYS.followUps, followUps)) {
            showToast("No se pudo guardar la actualización en este dispositivo.", "error");
            return;
        }

        form.reset();
        renderHistory();

        showToast(
            "Nota guardada en este dispositivo.",
            "success"
        );
    });
}

function initializeOrganizationRegistration() {
    document.addEventListener("click", event => {
        const button = event.target.closest(
            '[href="#registrar"], [data-register-organization], #registerOrganizationButton, .organization-button'
        );

        if (!button) {
            return;
        }

        event.preventDefault();

        createModal(`
            <h2>Registrar refugio</h2>
            <p>
                Registra la información básica de tu organización
                para integrarla al prototipo de PetHop.
            </p>

            <form id="organizationForm">

                <label class="sr-only" for="organizationName">Nombre de la organización</label>

                <input
                    id="organizationName"
                    type="text"
                    name="organization"
                    placeholder="Nombre de la organización"
                    required
                >

                <label class="sr-only" for="organizationContact">Persona de contacto</label>

                <input
                    id="organizationContact"
                    type="text"
                    name="contact"
                    placeholder="Persona de contacto"
                    required
                >

                <label class="sr-only" for="organizationEmail">Correo electrónico</label>

                <input
                    id="organizationEmail"
                    type="email"
                    name="email"
                    placeholder="Correo electrónico"
                    required
                >

                <label class="sr-only" for="organizationPhone">Teléfono</label>

                <input
                    id="organizationPhone"
                    type="tel"
                    name="phone"
                    placeholder="Teléfono"
                    required
                >

                <label class="sr-only" for="organizationDescription">Descripción de la organización</label>

                <textarea
                    id="organizationDescription"
                    name="description"
                    placeholder="Describe brevemente la organización"
                    required
                ></textarea>

                <button type="submit">
                    Enviar registro
                </button>

            </form>
        `);

        const form =
            document.querySelector("#organizationForm");

        form?.addEventListener("submit", event => {
            event.preventDefault();

            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }

            document
                .querySelector(".dynamic-modal")
                ?.remove();

            showToast(
                "Este formulario pertenece al prototipo y todavía no se envía a una organización.",
                "normal"
            );
        });
    });
}

function initializeCardLinks() {
    document.querySelectorAll(
        ".pet-card, .catalog-card"
    ).forEach(card => {
        const favoriteButton = card.querySelector("[data-pet-id]");
        const petId = String(favoriteButton?.dataset.petId || card.dataset.petId || "").toLowerCase();
        const record = getPetDirectory()[petId];
        const name = record?.name || card.dataset.name || card.dataset.petName || card.querySelector("h3")?.textContent.trim() || "Mascota";

        if (!petId) return;

        if (record) {
            card.dataset.name = record.name;
            card.dataset.species = record.species;
            card.dataset.age = record.age;
            card.dataset.gender = record.gender;
            card.dataset.size = record.size;
            card.dataset.location = record.location;
            const heading = card.querySelector("h3");
            if (heading) heading.textContent = record.name;
            const species = card.querySelector(".pet-species");
            if (species) species.textContent = record.species.toLocaleUpperCase("es-MX");
            const details = card.querySelectorAll(".pet-details span, .catalog-details span");
            [record.age, record.gender, record.size].forEach((value, index) => {
                if (details[index]) details[index].textContent = value;
            });
            const location = card.querySelector(".pet-location, .catalog-location");
            if (location) {
                const icon = location.querySelector("i, svg, span");
                location.replaceChildren();
                if (icon) location.append(icon);
                location.append(document.createTextNode(` ${record.location}`));
            }
        }

        const openProfile = () => {
            const pet = {
                ...(record || {}),
                id: petId,
                name,
                species: card.dataset.species || "",
                age: card.dataset.age || "",
                gender: card.dataset.gender || card.dataset.sex || "",
                size: card.dataset.size || "",
                location: card.dataset.location || "",
                description: card.dataset.description || "",
                photos: record?.photos || []
            };
            try {
                sessionStorage.setItem("pethop_selected_pet", JSON.stringify(pet));
            } catch {}
            window.location.href = `mascota.html?pet=${encodeURIComponent(petId)}`;
        };

        card.tabIndex = 0;
        card.setAttribute("role", "link");
        card.setAttribute("aria-label", `Abrir perfil de ${name}`);

        card.addEventListener("click", event => {
            if (event.target.closest("button, a, input, select, textarea")) return;
            openProfile();
        });

        card.addEventListener("keydown", event => {
            if (event.target === card && (event.key === "Enter" || event.key === " ")) {
                event.preventDefault();
                openProfile();
            }
        });
    });
}

function initializeSmoothLinks() {
    document.addEventListener("click", event => {
        const link = event.target.closest(
            'a[href^="#"]'
        );

        if (!link) {
            return;
        }

        const targetId =
            link.getAttribute("href");

        if (!targetId || targetId === "#") {
            return;
        }

        if (targetId === "#registrar") {
            return;
        }

        const target =
            document.querySelector(targetId);

        if (!target) {
            if (targetId.startsWith("#")) {
                event.preventDefault();
                window.location.href = `index.html${targetId}`;
            }
            return;
        }

        event.preventDefault();

        target.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

        history.replaceState(
            null,
            "",
            targetId
        );
    });
}

function initializeActiveNavigation() {
    const currentPage = getCurrentPage();

    document.querySelectorAll(
        ".main-nav a"
    ).forEach(link => {
        const href =
            link.getAttribute("href") || "";

        const normalized =
            href.toLowerCase();

        let active = false;

        if (
            currentPage === "inicio" &&
            normalized.includes("index.html")
        ) {
            active = true;
        }

        if (
            currentPage === "explorar" &&
            normalized.includes("explorar.html")
        ) {
            active = true;
        }

        if (
            (currentPage === "mascota" || currentPage === "solicitud") &&
            normalized.includes("explorar.html")
        ) {
            active = true;
        }

        if (
            currentPage === "seguimiento" &&
            normalized.includes("seguimiento.html")
        ) {
            active = true;
        }

        link.classList.toggle(
            "active",
            active
        );

        if (active) {
            link.setAttribute(
                "aria-current",
                "page"
            );
        }
    });
}

function initializeDateFields() {
    const inputs =
        document.querySelectorAll(
            'input[type="date"]'
        );

    const today = new Date();

    const formatted =
        today.getFullYear() +
        "-" +
        String(
            today.getMonth() + 1
        ).padStart(2, "0") +
        "-" +
        String(
            today.getDate()
        ).padStart(2, "0");

    inputs.forEach(input => {
        if (!input.min) {
            input.min = formatted;
        }
    });
}

function initializeImageFallbacks() {
    document
        .querySelectorAll("img")
        .forEach(image => {
            image.addEventListener(
                "error",
                () => {
                    if (
                        image.dataset.fallbackApplied
                    ) {
                        return;
                    }

                    image.dataset.fallbackApplied =
                        "true";

                    if (
                        image.alt === "PetHop" ||
                        image.closest(".brand")
                    ) {
                        image.style.display =
                            "none";
                    }
                }
            );
        });
}

function initializeRevealAnimations() {
    const elements =
        document.querySelectorAll(
            ".reveal, .action-card, .pet-card, .catalog-card, .process-step, .information-item"
        );

    if (!elements.length) {
        return;
    }

    if (
        !("IntersectionObserver" in window)
    ) {
        elements.forEach(element => {
            element.classList.add(
                "is-revealed"
            );
        });

        return;
    }

    elements.forEach(element => {
        if (
            !element.classList.contains(
                "reveal"
            )
        ) {
            element.classList.add("reveal");
        }
    });

    const observer =
        new IntersectionObserver(
            entries => {
                entries.forEach(entry => {
                    if (
                        entry.isIntersecting
                    ) {
                        entry.target.classList.add(
                            "is-revealed"
                        );

                        observer.unobserve(
                            entry.target
                        );
                    }
                });
            },
            {
                threshold: 0.08,
                rootMargin:
                    "0px 0px -30px 0px"
            }
        );

    elements.forEach(element => {
        observer.observe(element);
    });
}

function initializeScrollProgress() {
    const progress =
        document.createElement("div");

    progress.className =
        "scroll-progress";

    document.body.appendChild(progress);

    const update = () => {
        const scrollTop =
            window.scrollY;

        const height =
            document.documentElement
                .scrollHeight -
            window.innerHeight;

        const percentage =
            height > 0
                ? (scrollTop / height) * 100
                : 0;

        progress.style.width =
            Math.min(
                100,
                Math.max(
                    0,
                    percentage
                )
            ) + "%";
    };

    window.addEventListener(
        "scroll",
        update,
        { passive: true }
    );

    window.addEventListener(
        "resize",
        update
    );

    update();
}

function initializeKeyboardShortcuts() {
    document.addEventListener(
        "keydown",
        event => {
            const target =
                event.target;

            const typing =
                target instanceof
                    HTMLInputElement ||
                target instanceof
                    HTMLTextAreaElement ||
                target instanceof
                    HTMLSelectElement;

            if (typing) {
                return;
            }

            if (
                event.key === "/" &&
                !event.ctrlKey &&
                !event.metaKey &&
                !event.altKey
            ) {
                const input =
                    document.querySelector(
                        ".search-input input, #searchInput"
                    );

                if (input) {
                    event.preventDefault();
                    input.focus();
                }
            }
        }
    );
}

function initializeFormAnimations() {
    document.querySelectorAll(
        ".form-field input, .form-field select, .form-field textarea"
    ).forEach(field => {
        field.addEventListener(
            "blur",
            () => {
                if (
                    field.value &&
                    field.checkValidity()
                ) {
                    field.classList.add(
                        "form-success"
                    );
                    field.classList.remove(
                        "form-error"
                    );
                } else if (
                    field.value &&
                    !field.checkValidity()
                ) {
                    field.classList.add(
                        "form-error"
                    );
                    field.classList.remove(
                        "form-success"
                    );
                }
            }
        );

        field.addEventListener(
            "input",
            () => {
                if (
                    field.checkValidity()
                ) {
                    field.classList.remove(
                        "form-error"
                    );
                }
            }
        );
    });
}

function initializePageTransitions() {
    document.addEventListener(
        "click",
        event => {
            const link =
                event.target.closest(
                    "a[href]"
                );

            if (!link) {
                return;
            }

            const href =
                link.getAttribute(
                    "href"
                );

            if (
                !href ||
                href.startsWith("#") ||
                href.startsWith("mailto:") ||
                href.startsWith("tel:") ||
                href.startsWith("javascript:") ||
                link.target === "_blank"
            ) {
                return;
            }

            if (
                href.startsWith(
                    "http://"
                ) ||
                href.startsWith(
                    "https://"
                )
            ) {
                return;
            }

            document.body.classList.add(
                "page-leaving"
            );
        }
    );
}

function initializeApplicationState() {
    const params =
        new URLSearchParams(
            window.location.search
        );

    const applicationId =
        params.get("application");

    if (!applicationId) {
        return;
    }

    const storedApplications = PetHop.get(STORAGE_KEYS.applications, []);
    const applications = Array.isArray(storedApplications)
        ? storedApplications.filter(item => item && typeof item === "object")
        : [];

    const application =
        applications.find(
            item =>
                item.id ===
                applicationId
        );

    if (!application) {
        return;
    }

    document
        .querySelectorAll(
            "[data-application-id]"
        )
        .forEach(element => {
            element.textContent =
                application.id;
        });

    document
        .querySelectorAll(
            "[data-application-status]"
        )
        .forEach(element => {
            element.textContent =
                application.status;
        });
}

function initializeFollowUpStatus() {
    const storedApplications = PetHop.get(STORAGE_KEYS.applications, []);
    const applications = Array.isArray(storedApplications)
        ? storedApplications.filter(item => item && typeof item === "object")
        : [];
    const requestedId = new URLSearchParams(window.location.search).get("application");
    const latest = requestedId
        ? applications.find(item => item.id === requestedId)
        : applications[applications.length - 1];
    const statusElement = document.querySelector("[data-application-status]");
    const timeline = document.querySelector(".status-timeline");
    const statusHeading = document.querySelector(".status-card-header h2");
    const followUpCard = document.querySelector(".follow-up-pet-card");
    const summary = document.querySelector(".follow-up-summary");

    if (!latest) {
        if (followUpCard) followUpCard.hidden = true;
        if (statusElement) {
            statusElement.textContent = "Sin solicitud activa";
            statusElement.className = "status-pending";
        }
        if (statusHeading) statusHeading.textContent = "Todavía no tienes solicitudes";
        if (timeline) {
            timeline.classList.add("is-empty");
            timeline.replaceChildren();
            const message = document.createElement("p");
            message.className = "follow-up-empty";
            message.textContent = "Cuando envíes una solicitud, aquí podrás consultar el estado y los pasos siguientes.";
            const explore = document.createElement("a");
            explore.className = "primary-button";
            explore.href = "explorar.html";
            explore.textContent = "Explorar mascotas";
            timeline.append(message, explore);
        }
        if (summary) {
            const title = summary.querySelector("h2");
            if (title) title.textContent = "Sin solicitud";
            const subtitle = summary.querySelector("p");
            if (subtitle) subtitle.textContent = "Aún no hay solicitudes registradas";
            summary.querySelectorAll(".summary-row strong").forEach(value => { value.textContent = "—"; });
        }
        const recordCard = document.querySelector(".follow-up-record-card");
        const recordTitle = recordCard?.querySelector(".follow-up-record-header h2");
        const recordDescription = recordCard?.querySelector(".follow-up-record-header p");
        if (recordTitle) recordTitle.textContent = "Notas personales";
        if (recordDescription) recordDescription.textContent = "Tus notas se guardan en este dispositivo y no cambian el estado oficial de la solicitud.";
        return;
    }

    const pet = getPetDirectory()[String(latest.petId || "").toLowerCase()] || null;
    const petName = pet?.name || "Mascota";
    const status = latest.status || "Recibida";
    const statusSteps = document.querySelectorAll(".status-step");
    timeline?.classList.remove("is-empty");
    const statusConfig = {
        Recibida: { label: "Solicitud guardada", current: 0, className: "status-received" },
        "En revisión": { label: "En revisión", current: 1, className: "status-pending" },
        Contactada: { label: "Contacto iniciado", current: 2, className: "status-pending" },
        Aprobada: { label: "Adopción en proceso", current: 3, className: "status-approved" },
        Adoptada: { label: "Adopción completada", current: -1, className: "status-complete" },
        Rechazada: { label: "Solicitud no aprobada", current: -1, className: "status-required" }
    };
    const config = statusConfig[status] || statusConfig.Recibida;

    if (statusElement) {
        statusElement.textContent = config.label;
        statusElement.className = config.className;
    }
    if (statusHeading) statusHeading.textContent = `Solicitud de ${petName}`;
    if (followUpCard) {
        followUpCard.hidden = false;
        const petImage = followUpCard.querySelector(".follow-up-pet-image");
        if (petImage) petImage.dataset.name = petName;
        const petImageLabel = petImage?.querySelector(".sr-only");
        if (petImageLabel) petImageLabel.textContent = `Imagen de ${petName}`;
        const name = followUpCard.querySelector("h2");
        const label = followUpCard.querySelector(".pet-species");
        const description = followUpCard.querySelector(".follow-up-pet-info > p");
        const facts = followUpCard.querySelectorAll(".follow-up-health span");
        if (name) name.textContent = petName;
        if (label) label.textContent = `SOLICITUD ${status.toLocaleUpperCase("es-MX")}`;
        if (description) description.textContent = `Solicitud enviada el ${new Date(latest.date).toLocaleDateString("es-MX")}`;
        if (facts[0]) facts[0].textContent = status === "Adoptada" ? "Adopción registrada" : "Estado de solicitud";
        if (facts[1]) facts[1].textContent = config.label;
    }
    statusSteps.forEach((step, index) => {
        step.classList.remove("current", "completed");
        step.removeAttribute("aria-current");
        if (status === "Adoptada" || (config.current >= 0 && index < config.current)) step.classList.add("completed");
        if (index === config.current) {
            step.classList.add("current");
            step.setAttribute("aria-current", "step");
        }
        if (status === "Adoptada") step.classList.add("completed");
    });
    if (summary) {
        const title = summary.querySelector("h2");
        if (title) title.textContent = petName;
        const subtitle = summary.querySelector("p");
        if (subtitle) subtitle.textContent = `Solicitud ${latest.id || "registrada"}`;
        const values = summary.querySelectorAll(".summary-row strong");
        if (values[0]) values[0].textContent = config.label;
        if (values[1]) values[1].textContent = new Date(latest.date).toLocaleDateString("es-MX");
        if (values[2]) values[2].textContent = pet?.location || "Por confirmar";
    }
}

function initializeBackButtons() {
    document.addEventListener(
        "click",
        event => {
            const button =
                event.target.closest(
                    ".back-link, .application-back-button, [data-go-back]"
                );

            if (!button) {
                return;
            }

            const href =
                button.getAttribute(
                    "href"
                );

            if (
                href &&
                href !== "#" &&
                href !== "javascript:void(0)"
            ) {
                return;
            }

            event.preventDefault();

            if (
                window.history.length >
                1
            ) {
                window.history.back();
            } else {
                window.location.href =
                    "index.html";
            }
        }
    );
}

function initializeActionCards() {
    document
        .querySelectorAll(
            ".action-card"
        )
        .forEach(card => {
            card.addEventListener(
                "mouseenter",
                () => {
                    card.classList.add(
                        "is-hovered"
                    );
                }
            );

            card.addEventListener(
                "mouseleave",
                () => {
                    card.classList.remove(
                        "is-hovered"
                    );
                }
            );
        });
}

function initializePetProfile() {
    const profile =
        document.querySelector(
            ".pet-profile"
        );

    if (!profile) {
        return;
    }

    const params = new URLSearchParams(window.location.search);
    const requestedPetId = String(
        profile.dataset.petId || params.get("pet") || "chino"
    ).toLowerCase();

    let selectedPet = null;
    try {
        selectedPet = JSON.parse(sessionStorage.getItem("pethop_selected_pet") || "null");
    } catch {}
    if (selectedPet?.id !== requestedPetId) selectedPet = null;
    const directory = getPetDirectory();
    const pet = { ...directory[requestedPetId], ...(selectedPet || {}) };
    if (!pet.id) Object.assign(pet, directory.chino);
    const petId = pet.id;
    const name = pet.name;
    const description = pet.description || `El refugio aún no ha compartido una descripción sobre ${name}. Puedes preguntarle cómo convive, qué actividades disfruta y qué tipo de hogar se adapta mejor a ${name}.`;
    const petLocation = pet.location || "Ubicación por confirmar";
    const locationValue = /quintana roo/i.test(petLocation) || petLocation === "Ubicación por confirmar"
        ? petLocation
        : `${petLocation}, Quintana Roo`;
    const profileFields = [
        locationValue,
        pet.species || "Por confirmar",
        pet.age,
        pet.gender,
        pet.size,
        "Pendiente de confirmar",
        "Pendiente de confirmar",
        "Pendiente de confirmar"
    ];

    document.querySelector(".pet-profile-heading h1")?.replaceChildren(document.createTextNode(name));
    document.querySelector(".pet-gallery")?.setAttribute("aria-label", `Galería de imágenes de referencia de ${name}`);
    const galleryCaption = document.querySelector(".gallery-caption");
    if (galleryCaption) galleryCaption.textContent = `Imágenes de referencia. Confirma con el refugio cuáles corresponden a ${name}.`;
    document.querySelector(".pet-story p")?.replaceChildren(document.createTextNode(description));
    document.querySelector(".pet-story")?.setAttribute("aria-label", `Descripción de ${name}`);
    document.querySelector(".pet-story-title")?.replaceChildren(document.createTextNode(`Detalles extra de ${name}`));
    document.querySelector(".pet-information h2")?.replaceChildren(document.createTextNode(`Datos de ${name}`));
    document.querySelectorAll(".information-item").forEach((item, index) => {
        if (profileFields[index]) item.querySelector("span")?.replaceChildren(document.createTextNode(profileFields[index]));
    });
    const notice = document.querySelector(".adoption-notice p");
    if (notice) notice.textContent = `Conoce las características y la información de salud de ${name} antes de enviar tu solicitud. El refugio puede resolver cualquier dato pendiente.`;
    document.title = `${name} | PetHop`;

    const profileName = name || "mascota";
    document.querySelectorAll(".pet-profile [data-favorite]").forEach(button => {
        button.dataset.petId = petId;
        button.setAttribute("aria-label", `Agregar ${profileName} a favoritos`);
    });
    document.querySelectorAll("[data-chat-pet]").forEach(button => { button.dataset.petId = petId; });
    document.querySelectorAll(".pet-apply-button").forEach(button => {
        button.href = `solicitud.html?pet=${encodeURIComponent(petId)}`;
    });

    document
        .querySelectorAll(
            "[data-pet-id]"
        )
        .forEach(element => {
            if (
                !element.dataset.petId
            ) {
                element.dataset.petId =
                    petId;
            }
        });

    document
        .querySelectorAll(
            "[data-apply-pet]"
        )
        .forEach(button => {
            button.dataset.petId = petId;
            button.addEventListener(
                "click",
                event => {
                    event.preventDefault();

                    window.location.href =
                        "solicitud.html?pet=" +
                        encodeURIComponent(
                            petId
                        );
                }
            );
        });
}

function initializeApplicationPet() {
    const form =
        document.querySelector(
            "#adoptionForm"
        );

    if (!form) {
        return;
    }

    const params =
        new URLSearchParams(
            window.location.search
        );

    const requestedId = (params.get("pet") || "chino").toLowerCase();
    let selectedPet = null;
    try {
        selectedPet = JSON.parse(sessionStorage.getItem("pethop_selected_pet") || "null");
    } catch {}
    if (selectedPet?.id !== requestedId) selectedPet = null;
    const pet = { ...getPetDirectory()[requestedId], ...(selectedPet || {}) };
    if (!pet.id) Object.assign(pet, PET_DIRECTORY.chino);

    form.dataset.petId = pet.id;
    const description = document.querySelector(".application-header-container > p");
    if (description) description.textContent = `Completa la información necesaria para iniciar el proceso de adopción de ${pet.name}.`;
    const backLink = document.querySelector(".application-header-container .back-link");
    if (backLink) backLink.href = `mascota.html?pet=${encodeURIComponent(pet.id)}`;
    document.querySelectorAll("#adoptionForm [placeholder*='Chino']").forEach(field => {
        field.placeholder = field.placeholder.replace(/Chino/g, pet.name);
    });
    document.querySelectorAll(".application-pet-card h2, .application-pet-card .sr-only").forEach(element => {
        element.textContent = element.textContent.replace(/Chino/g, pet.name);
    });
    const petImage = document.querySelector(".application-pet-card .application-pet-image");
    if (petImage) {
        petImage.dataset.name = pet.name;
        const photo = safePhotoData(pet.photos?.[0]);
        if (photo) petImage.style.backgroundImage = `url("${photo}")`;
    }
    const petSpecies = document.querySelector(".application-pet-content > .pet-species");
    if (petSpecies) petSpecies.textContent = pet.species.toLocaleUpperCase("es-MX");
    const petLocation = document.querySelector(".application-pet-content > p");
    if (petLocation) petLocation.textContent = pet.location || "Ubicación por confirmar";
}

function initializeFooterYear() {
    const year =
        new Date().getFullYear();

    document
        .querySelectorAll(
            "[data-current-year], .footer-year"
        )
        .forEach(element => {
            element.textContent =
                year;
        });
}

function initializeExternalNavigation() {
    document
        .querySelectorAll(
            'a[href="explorar.html"]'
        )
        .forEach(link => {
            link.addEventListener(
                "click",
                () => {
                        try {
                            sessionStorage.setItem(
                                "pethop_previous_page",
                                window.location.href
                            );
                        } catch {}
                }
            );
        });
}

function initialize() {
    if (window.lucide?.createIcons) {
        window.lucide.createIcons();
    }

    initializePublishedPets();
    initializeFavorites();
    initializeFavoritesMenu();
    initializeMessaging();
    initializeHomeSearch();
    initializeCardLinks();
    initializeCatalog();
    initializeGallery();
    initializeAdoptionForm();
    initializeFollowUpForm();
    initializeOrganizationRegistration();
    initializeSmoothLinks();
    initializeActiveNavigation();
    initializeDateFields();
    initializeImageFallbacks();
    initializeRevealAnimations();
    initializeScrollProgress();
    initializeKeyboardShortcuts();
    initializeFormAnimations();
    initializePageTransitions();
    initializeApplicationState();
    initializeFollowUpStatus();
    initializeBackButtons();
    initializeActionCards();
    initializePetProfile();
    initializeApplicationPet();
    initializeFooterYear();
    initializeExternalNavigation();
    initializeButtonEffects();

    updateFavoritesCount();
    updateFavoriteButtons();
}

if (document.readyState === "loading") {
    document.addEventListener(
        "DOMContentLoaded",
        initialize
    );
} else {
    initialize();
}


function getChatThreads() {
    if (window.PetHopData) return window.PetHopData.messages(localStorage);
    const threads = PetHop.get(STORAGE_KEYS.messages, []);
    return Array.isArray(threads)
        ? threads
            .filter(thread => thread && typeof thread === "object")
            .map(thread => ({
                ...thread,
                messages: Array.isArray(thread.messages)
                    ? thread.messages.filter(message => message && typeof message === "object")
                    : []
            }))
        : [];
}

function openChatThread(petId, petName) {
    const user = PetHop.get(STORAGE_KEYS.user, null);
    const owner = String(user?.email || "visitante").trim().toLowerCase();
    const threadId = `${owner}:${petId}`;
    const thread = getChatThreads().find(item => item.id === threadId);
    const messages = thread?.messages || [];
    const transcript = messages.length
        ? messages.map(message => {
            const date = new Date(message.createdAt);
            const time = Number.isNaN(date.getTime())
                ? ""
                : new Intl.DateTimeFormat("es-MX", { hour: "2-digit", minute: "2-digit" }).format(date);
            return `
                <article class="chat-message ${message.sender === "adopter" ? "chat-message-outgoing" : "chat-message-incoming"}">
                    <p>${escapeHTML(message.text)}</p>
                    ${time ? `<time>${time}</time>` : ""}
                </article>
            `;
        }).join("")
        : '<p class="chat-empty">Escribe al refugio para preguntar por su personalidad, rutina o proceso de adopción.</p>';

    const modal = createModal(`
        <section class="chat-panel" aria-label="Conversación sobre ${escapeHTML(petName)}">
            <header class="chat-header">
                <span class="chat-avatar"><i data-lucide="messages-square" aria-hidden="true"></i></span>
                <div><h2>Consulta por ${escapeHTML(petName)}</h2><p>Conversación de adopción en PetHop</p></div>
            </header>
            <div class="chat-messages" aria-live="polite">${transcript}</div>
            <form class="chat-form" id="petChatForm">
                <label class="sr-only" for="petChatInput">Escribe un mensaje</label>
                <textarea id="petChatInput" name="message" rows="2" maxlength="1000" placeholder="Escribe tu mensaje..." required></textarea>
                <button type="submit" aria-label="Enviar mensaje"><i data-lucide="send" aria-hidden="true"></i><span>Enviar</span></button>
            </form>
        </section>
    `);

    modal.querySelector("#petChatForm")?.addEventListener("submit", event => {
        event.preventDefault();
        const input = modal.querySelector("#petChatInput");
        const text = input?.value.trim();
        if (!text) return;

        const threads = getChatThreads();
        let activeThread = threads.find(item => item.id === threadId);
        if (!activeThread) {
            activeThread = { id: threadId, petId, petName, owner, adopter: user?.name || user?.fullName || user?.nombre || owner, messages: [] };
            threads.unshift(activeThread);
        }
        activeThread.petName = petName;
        activeThread.adopter ||= user?.name || user?.fullName || user?.nombre || owner;
        activeThread.updatedAt = new Date().toISOString();
        activeThread.messages.push({ sender: "adopter", text, createdAt: activeThread.updatedAt });
        const saved = window.PetHopData
            ? window.PetHopData.write(localStorage, STORAGE_KEYS.messages, threads)
            : PetHop.set(STORAGE_KEYS.messages, threads);
        if (!saved) {
            showToast("No se pudo guardar el mensaje en este navegador.", "error");
            return;
        }
        openChatThread(petId, petName);
    });
}

function openMessagesInbox() {
    const owner = PetHop.get(STORAGE_KEYS.user, null)?.email || "visitante";
    const threads = getChatThreads().filter(thread => String(thread.owner || "visitante").trim().toLowerCase() === owner);
    const list = threads.length
        ? `<div class="chat-inbox-list">${threads.map(thread => {
            return `<button type="button" class="chat-inbox-item" data-open-chat="${escapeHTML(thread.petId)}" data-chat-name="${escapeHTML(thread.petName)}">
                <span class="chat-avatar"><i data-lucide="message-circle" aria-hidden="true"></i></span>
                <span class="chat-inbox-copy"><strong>Nuevo mensaje sobre ${escapeHTML(thread.petName)}</strong></span>
                <i data-lucide="chevron-right" aria-hidden="true"></i>
            </button>`;
        }).join("")}</div>`
        : '<p class="chat-empty-inbox">Aún no tienes conversaciones. Abre el perfil de una mascota para escribir a su refugio.</p>';

    const modal = createModal(`<section class="chat-inbox"><h2>Mensajes</h2><p class="chat-inbox-intro">Tus consultas sobre adopción, reunidas en PetHop.</p>${list}</section>`);
    modal.querySelectorAll("[data-open-chat]").forEach(button => {
        button.addEventListener("click", () => openChatThread(button.dataset.openChat, button.dataset.chatName));
    });
}

function initializeMessaging() {
    document.addEventListener("click", event => {
        const inboxButton = event.target.closest(".messages-button");
        if (inboxButton) {
            event.preventDefault();
            openMessagesInbox();
            return;
        }
        const contactButton = event.target.closest("[data-chat-pet]");
        if (!contactButton) return;
        event.preventDefault();
        const petId = contactButton.dataset.petId || new URLSearchParams(window.location.search).get("pet") || "chino";
        const petName = document.querySelector(".pet-profile-heading h1")?.textContent.trim() || "esta mascota";
        openChatThread(petId, petName);
    });
}
