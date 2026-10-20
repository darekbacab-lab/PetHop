"use strict";

(() => {
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
        const feedback = document.querySelector("#login-feedback");
        feedback.textContent = "";
        if (!loginForm.reportValidity()) return;
        feedback.textContent = "El inicio de sesión aún no está disponible: PetHop no tiene un servicio de autenticación conectado. No se guardó tu contraseña.";
        feedback.classList.add("is-error");
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
        feedback.textContent = "El registro todavía no está disponible: falta conectar un servicio de autenticación. No se creó una cuenta ni se guardó tu contraseña.";
        feedback.classList.add("is-error");
    });
})();
