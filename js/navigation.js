"use strict";

(() => {
function initializeMenu() {
    const menuToggle = document.querySelector(".menu-toggle");
    const mainMenu = document.querySelector(".main-nav");
    const providerPage = document.body.classList.contains("provider-page");

    if (!menuToggle || !mainMenu) {
        return;
    }

    if (mainMenu.parentElement !== document.body) document.body.append(mainMenu);

    mainMenu.inert = true;
    mainMenu.setAttribute("aria-hidden", "true");

    const currentPage = window.location.pathname.split("/").pop().toLowerCase() || "index.html";
    mainMenu.querySelectorAll("a[href]").forEach(link => {
        const linkPage = link.getAttribute("href").split("?")[0];
        const profileSectionPage = ["mascota.html", "solicitud.html"].includes(currentPage);
        const isActive = linkPage === currentPage || (profileSectionPage && linkPage === "explorar.html");

        link.classList.toggle("active", isActive);
        if (isActive) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
    });

    let indicator = mainMenu.querySelector(".nav-active-indicator");
    if (!indicator) {
        indicator = document.createElement("span");
        indicator.className = "nav-active-indicator";
        indicator.setAttribute("aria-hidden", "true");
        mainMenu.prepend(indicator);
    }

    if (!mainMenu.querySelector(".menu-drawer-header")) {
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
        drawerHeader.querySelector(".menu-drawer-close").addEventListener("click", () => closeMenu(true));
    }
    try { window.lucide?.createIcons({ root: mainMenu }); } catch (error) { console.error("No se pudieron actualizar los iconos del menú.", error); }

    const backdrop = document.createElement("div");
    backdrop.className = "menu-backdrop";
    backdrop.setAttribute("aria-hidden", "true");
    document.body.appendChild(backdrop);
    let pendingNavigation = null;

    const positionIndicator = (target, animate = true) => {
        if (!target || !indicator) return;

        indicator.style.setProperty("--indicator-top", `${target.offsetTop}px`);
        indicator.style.setProperty("--indicator-height", `${target.offsetHeight}px`);
        indicator.classList.toggle("is-visible", mainMenu.classList.contains("open"));

        if (animate) {
            indicator.classList.remove("is-moving");
            void indicator.offsetWidth;
            indicator.classList.add("is-moving");
        }
    };

    const activeLink = mainMenu.querySelector('[aria-current="page"]');
    positionIndicator(activeLink, false);

    const closeMenu = (restoreFocus = false) => {
        if (pendingNavigation) {
            window.clearTimeout(pendingNavigation);
            pendingNavigation = null;
        }

        const providerSidebar = providerPage && document.querySelector(".provider-sidebar");
        providerSidebar?.classList.remove("is-open");
        if (providerSidebar) {
            providerSidebar.inert = false;
            providerSidebar.setAttribute("aria-hidden", "false");
        }
        mainMenu.classList.remove("open");
        mainMenu.inert = true;
        mainMenu.setAttribute("aria-hidden", "true");
        menuToggle.classList.remove("active");
        menuToggle.setAttribute("aria-expanded", "false");
        menuToggle.setAttribute("aria-label", "Abrir menú");
        menuToggle.innerHTML = '<i data-lucide="menu" aria-hidden="true"></i>';
        try { window.lucide?.createIcons(); } catch (error) { console.error("No se pudieron actualizar los iconos del menú.", error); }
        document.body.classList.remove("menu-open");
        backdrop.classList.remove("is-visible");
        indicator.classList.remove("is-visible", "is-moving");
        if (providerSidebar) menuToggle.setAttribute("aria-controls", "provider-sidebar");
        if (restoreFocus) menuToggle.focus();
    };

    const openMenu = () => {
        const providerSidebar = providerPage && document.querySelector(".provider-sidebar");
        if (providerPage && document.body.classList.contains("provider-dashboard-active") && providerSidebar) {
            mainMenu.classList.remove("open");
            mainMenu.inert = true;
            mainMenu.setAttribute("aria-hidden", "true");
            providerSidebar.inert = false;
            providerSidebar.setAttribute("aria-hidden", "false");
            providerSidebar.classList.add("is-open");
            menuToggle.setAttribute("aria-controls", "provider-sidebar");
            menuToggle.classList.add("active");
            menuToggle.setAttribute("aria-expanded", "true");
            menuToggle.setAttribute("aria-label", "Cerrar menú");
            menuToggle.innerHTML = '<i data-lucide="x" aria-hidden="true"></i>';
            try { window.lucide?.createIcons(); } catch (error) { console.error("No se pudieron actualizar los iconos del menú.", error); }
            document.body.classList.add("menu-open");
            backdrop.classList.add("is-visible");
            const focusTarget = providerSidebar.querySelector("button.is-active, a[href], button:not([disabled])");
            window.requestAnimationFrame(() => {
                if (providerSidebar.classList.contains("is-open")) focusTarget?.focus({ preventScroll: true });
            });
            return;
        }

        menuToggle.setAttribute("aria-controls", "main-menu");
        mainMenu.inert = false;
        mainMenu.setAttribute("aria-hidden", "false");
        mainMenu.classList.add("open");
        menuToggle.classList.add("active");
        menuToggle.setAttribute("aria-expanded", "true");
        menuToggle.setAttribute("aria-label", "Cerrar menú");
        menuToggle.innerHTML = '<i data-lucide="x" aria-hidden="true"></i>';
        try { window.lucide?.createIcons(); } catch (error) { console.error("No se pudieron actualizar los iconos del menú.", error); }
        document.body.classList.add("menu-open");
        backdrop.classList.add("is-visible");
        const selectedItem = mainMenu.querySelector('[aria-current="page"]');
        const focusTarget = selectedItem || mainMenu.querySelector(".menu-drawer-close") || mainMenu.querySelector("a[href]") || mainMenu.querySelector(".menu-favorites");
        positionIndicator(selectedItem, false);
        window.requestAnimationFrame(() => {
            if (!mainMenu.classList.contains("open")) return;
            positionIndicator(selectedItem, false);
            focusTarget?.focus({ preventScroll: true });
        });
    };

    menuToggle.addEventListener("click", event => {
        event.preventDefault();
        event.stopPropagation();

        const providerSidebar = providerPage && document.querySelector(".provider-sidebar");
        if (menuToggle.getAttribute("aria-expanded") === "true" || mainMenu.classList.contains("open") || providerSidebar?.classList.contains("is-open")) {
            closeMenu();
        } else {
            openMenu();
        }
    });

    mainMenu.addEventListener("click", event => {
        const link = event.target.closest(".nav-link, .menu-favorites");

        if (link) {
            if (link.matches("a[href]")) {
                event.preventDefault();
                positionIndicator(link);

                const navigate = () => {
                    pendingNavigation = null;
                    if (!mainMenu.classList.contains("open")) return;
                    closeMenu();
                    window.location.assign(link.href);
                };

                if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
                    navigate();
                } else {
                    pendingNavigation = window.setTimeout(navigate, 320);
                }
            } else {
                closeMenu();
            }
        }
    });

    backdrop.addEventListener("click", () => closeMenu(true));

    if (providerPage) {
        const providerSidebar = document.querySelector(".provider-sidebar");
        if (providerSidebar) {
            providerSidebar.inert = false;
            providerSidebar.setAttribute("aria-hidden", "false");
        }
        window.addEventListener("resize", () => {
            const sidebar = document.querySelector(".provider-sidebar");
            if (!sidebar) return;
            closeMenu();
            sidebar.inert = false;
            sidebar.setAttribute("aria-hidden", "false");
        });
    }

    document.addEventListener("click", event => {
        const providerSidebar = providerPage && document.querySelector(".provider-sidebar");
        if (providerSidebar?.classList.contains("is-open")) {
            if (event.target.closest("[data-section]")) {
                closeMenu();
                const heading = document.querySelector("#provider-section-content h1");
                if (heading) {
                    heading.setAttribute("tabindex", "-1");
                    heading.focus({ preventScroll: true });
                }
            } else if (!providerSidebar.contains(event.target) && !menuToggle.contains(event.target)) {
                closeMenu();
            }
        }

        if (
            mainMenu.classList.contains("open") &&
            !mainMenu.contains(event.target) &&
            !menuToggle.contains(event.target)
        ) {
            closeMenu();
        }
    });

    mainMenu.addEventListener("keydown", event => {
        if (event.key !== "Tab" || !mainMenu.classList.contains("open")) return;

        const focusable = [...mainMenu.querySelectorAll('a[href], button:not([disabled])')];
        if (!focusable.length) return;
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

    document.addEventListener("keydown", event => {
        const providerSidebar = providerPage && document.querySelector(".provider-sidebar");
        if (event.key === "Escape" && (mainMenu.classList.contains("open") || providerSidebar?.classList.contains("is-open"))) {
            closeMenu(true);
        }

        if (event.key !== "Tab" || !providerSidebar?.classList.contains("is-open")) return;
        const focusable = [...providerSidebar.querySelectorAll('a[href], button:not([disabled])')];
        if (!focusable.length) return;
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

    window.addEventListener("resize", () => {
        positionIndicator(mainMenu.querySelector('[aria-current="page"]'), false);
    });
}

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeMenu, { once: true });
    } else {
        initializeMenu();
    }
})();
