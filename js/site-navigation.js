"use strict";

(() => {
    const initSiteMenu = () => {
        const toggle = document.querySelector(".menu-toggle");
        const menu = document.querySelector("#main-menu");
        if (!toggle || !menu || document.body.classList.contains("provider-page")) return;

        if (menu.parentElement !== document.body) document.body.append(menu);

        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
        let backdrop = document.querySelector(".menu-backdrop");
        if (!backdrop) {
            backdrop = document.createElement("div");
            backdrop.className = "menu-backdrop";
            backdrop.setAttribute("aria-hidden", "true");
            document.body.append(backdrop);
        }

        const icon = (open) => open
            ? '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="m18 6-12 12M6 6l12 12"/></svg>'
            : '<svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg>';
        const indicator = menu.querySelector(".nav-active-indicator") || (() => {
            const node = document.createElement("span");
            node.className = "nav-active-indicator";
            node.setAttribute("aria-hidden", "true");
            menu.prepend(node);
            return node;
        })();
        if (!menu.querySelector(".menu-drawer-header")) {
            const drawerHeader = document.createElement("div");
            drawerHeader.className = "menu-drawer-header";
            drawerHeader.innerHTML = `
                <a class="menu-drawer-brand" href="index.html" aria-label="PetHop, inicio">
                    <img src="img/image1.png" alt="PetHop">
                </a>
                <button class="menu-drawer-close" type="button" aria-label="Cerrar menú">
                    <i data-lucide="x" aria-hidden="true"></i>
                </button>
            `;
            indicator.after(drawerHeader);
        }
        const drawerClose = menu.querySelector(".menu-drawer-close");

        const current = (location.pathname.split("/").pop() || "index.html").toLowerCase();
        const profilePage = ["mascota.html", "solicitud.html"].includes(current);
        const links = [...menu.querySelectorAll("a[href]")];
        links.forEach(link => {
            const destination = link.getAttribute("href").split(/[?#]/, 1)[0].toLowerCase();
            const active = destination === current || (profilePage && destination === "explorar.html");
            link.classList.toggle("active", active);
            if (active) link.setAttribute("aria-current", "page");
            else link.removeAttribute("aria-current");
        });

        const placeIndicator = (target, animate = true) => {
            if (!target) return;
            indicator.style.setProperty("--indicator-top", `${target.offsetTop}px`);
            indicator.style.setProperty("--indicator-height", `${target.offsetHeight}px`);
            indicator.classList.toggle("is-visible", menu.classList.contains("open"));
            if (animate && !reducedMotion.matches) {
                indicator.classList.remove("is-moving");
                void indicator.offsetWidth;
                indicator.classList.add("is-moving");
            }
        };

        const close = (restoreFocus = false) => {
            menu.classList.remove("open");
            menu.inert = true;
            menu.setAttribute("aria-hidden", "true");
            toggle.classList.remove("active");
            toggle.setAttribute("aria-expanded", "false");
            toggle.setAttribute("aria-label", "Abrir menú");
            toggle.setAttribute("aria-controls", "main-menu");
            toggle.innerHTML = icon(false);
            document.body.classList.remove("menu-open");
            backdrop.classList.remove("is-visible");
            indicator.classList.remove("is-visible", "is-moving");
            if (restoreFocus) toggle.focus({ preventScroll: true });
        };
        drawerClose?.addEventListener("click", () => close(true));
        try { window.lucide?.createIcons({ root: menu }); } catch (error) { console.error("No se pudieron actualizar los iconos del menú.", error); }

        const open = () => {
            menu.inert = false;
            menu.setAttribute("aria-hidden", "false");
            menu.classList.add("open");
            toggle.classList.add("active");
            toggle.setAttribute("aria-expanded", "true");
            toggle.setAttribute("aria-label", "Cerrar menú");
            toggle.setAttribute("aria-controls", "main-menu");
            toggle.innerHTML = icon(true);
            document.body.classList.add("menu-open");
            backdrop.classList.add("is-visible");
            const active = menu.querySelector('[aria-current="page"]');
            placeIndicator(active, false);
            requestAnimationFrame(() => {
                if (!menu.classList.contains("open")) return;
                placeIndicator(active, false);
                (active || drawerClose || links[0] || menu.querySelector(".menu-favorites"))?.focus({ preventScroll: true });
            });
        };

        toggle.setAttribute("aria-controls", "main-menu");
        toggle.setAttribute("aria-expanded", "false");
        toggle.setAttribute("aria-label", "Abrir menú");
        toggle.innerHTML = icon(false);
        menu.inert = true;
        menu.setAttribute("aria-hidden", "true");
        placeIndicator(menu.querySelector('[aria-current="page"]'), false);

        toggle.addEventListener("click", event => {
            event.preventDefault();
            event.stopPropagation();
            menu.classList.contains("open") ? close() : open();
        });
        backdrop.addEventListener("click", () => close(true));
        menu.addEventListener("click", event => {
            const link = event.target.closest(".nav-link");
            const favorite = event.target.closest(".menu-favorites");
            if (link) {
                event.preventDefault();
                placeIndicator(link);
                const destination = link.href;
                window.setTimeout(() => window.location.assign(destination), reducedMotion.matches ? 0 : 220);
            } else if (favorite) {
                close();
            }
        });
        document.addEventListener("click", event => {
            if (menu.classList.contains("open") && !menu.contains(event.target) && !toggle.contains(event.target)) close();
        });
        document.addEventListener("keydown", event => {
            if (event.key === "Escape" && menu.classList.contains("open")) {
                event.preventDefault();
                close(true);
                return;
            }
            if (event.key !== "Tab" || !menu.classList.contains("open")) return;
            const focusable = [...menu.querySelectorAll('a[href], button:not([disabled])')];
            if (!focusable.length) return;
            if (event.shiftKey && document.activeElement === focusable[0]) {
                event.preventDefault();
                focusable.at(-1).focus();
            } else if (!event.shiftKey && document.activeElement === focusable.at(-1)) {
                event.preventDefault();
                focusable[0].focus();
            }
        });
        window.addEventListener("resize", () => placeIndicator(menu.querySelector('[aria-current="page"]'), false));
    };

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initSiteMenu, { once: true });
    else initSiteMenu();
})();
