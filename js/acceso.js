"use strict";

(() => {
    const storage = window.PetHopStorage || localStorage;
    const loginForm = document.querySelector("#login-form");
    const registerForm = document.querySelector("#register-form");
    const title = document.querySelector("#auth-title");
    const description = document.querySelector("#auth-description");
    const providerKind = document.querySelector("#provider-kind");

    function showView(view) {
        const registering = view === "register";
        loginForm.hidden = registering;
        registerForm.hidden = !registering;
        title.textContent = registering ? "Crea tu cuenta." : "Qué bueno verte.";
        description.textContent = registering
            ? "Cuéntanos cómo quieres participar en PetHop."
            : "Ingresa para continuar con tu proceso de adopción.";
        document.title = `${registering ? "Crear cuenta" : "Ingresar"} | PetHop`;
        document.querySelector(registering ? "#register-email" : "#login-email").focus();
    }

    document.querySelectorAll("[data-auth-show]").forEach(button => {
        button.addEventListener("click", () => showView(button.dataset.authShow));
    });

    document.querySelectorAll("[data-password-toggle]").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            button.textContent = show ? "Ocultar" : "Mostrar";
            button.setAttribute("aria-pressed", String(show));
        });
    });

    registerForm.querySelectorAll('input[name="profile"]').forEach(input => {
        input.addEventListener("change", () => {
            const needsKind = input.value === "provider" && input.checked;
            providerKind.hidden = !needsKind;
            providerKind.querySelectorAll('input[name="provider-kind"]').forEach(choice => {
                choice.required = needsKind;
                if (!needsKind) choice.checked = false;
            });
        });
    });

    loginForm.addEventListener("submit", event => {
        event.preventDefault();
        if (!loginForm.reportValidity()) return;
        window.location.assign("index.html");
    });

    registerForm.addEventListener("submit", event => {
        event.preventDefault();
        const feedback = document.querySelector("#register-feedback");
        const password = registerForm.elements.password;
        const confirm = registerForm.elements.confirm;
        feedback.classList.remove("is-error");
        feedback.textContent = "";
        if (password.value !== confirm.value) {
            confirm.setCustomValidity("Las contraseñas no coinciden.");
            confirm.reportValidity();
            confirm.addEventListener("input", () => confirm.setCustomValidity(""), { once: true });
            return;
        }
        confirm.setCustomValidity("");
        if (!registerForm.reportValidity()) return;

        const selectedProfile = registerForm.querySelector('input[name="profile"]:checked')?.value;
        if (selectedProfile === "provider") {
            const selectedKind = registerForm.querySelector('input[name="provider-kind"]:checked')?.value;
            try {
                const state = storage.read("pethop_provider_workspace", {}) || {};
                state.role = selectedKind;
                state.section = "Inicio";
                state.profile = {
                    ...(state.profile || {}),
                    email: registerForm.elements.email.value.trim()
                };
                storage.write("pethop_provider_workspace", state);
            } catch {
                feedback.textContent = "No se pudo guardar el perfil en este dispositivo. Revisa el almacenamiento del navegador e inténtalo de nuevo.";
                feedback.classList.add("is-error");
                return;
            }
            window.location.assign("proveedor.html");
            return;
        }

        window.location.assign("index.html");
    });
})();
