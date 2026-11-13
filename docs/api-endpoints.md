# API REST v1

Base: `/api/v1`. DTOs de solicitud y respuesta separados de las entidades. Las respuestas paginadas contienen `content`, `page`, `size`, `totalElements`, `totalPages`. Errores de validación se devuelven como Problem Details. La autenticación usa cookies de sesión; las mutaciones requieren `X-XSRF-TOKEN` obtenido desde `GET /auth/csrf`.

## Autenticación y usuarios

- `GET /auth/csrf` obtiene token CSRF.
- `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`.
- `POST /auth/password-reset/request`, `POST /auth/password-reset/confirm`.
- `POST /auth/email-verification/confirm`, `POST /auth/email-verification/resend` (público, con límite de 5/hora por IP; responde 202 exista o no la cuenta).
- Los correos transaccionales (verificación y recuperación) se envían por la API HTTP de Brevo, no por SMTP: Railway bloquea SMTP saliente en planes gratuitos. Variables: `BREVO_API_KEY` y `BREVO_FROM_EMAIL`.
- `GET/PATCH /users/me`, `PATCH /users/me/password`.

## Refugio y mascotas

- `POST /shelters`, `GET/PATCH /shelters/me`.
- `GET /pets` catálogo público; `GET /pets/mine` propias publicaciones. Parámetros: especie, sexo, tamaño, ubicación, búsqueda, orden, página y tamaño de página.
- `GET /pets/{id}`, `POST /pets`, `PATCH /pets/{id}`, `PATCH /pets/{id}/status`, `DELETE /pets/{id}` (baja lógica).
- `GET /pets/{id}/images`, `POST /pets/{id}/images`, `DELETE /pets/{id}/images/{imageId}`, `GET /pet-images/{id}`.
- `GET/POST /pets/{petId}/health-records`; `GET /pets/{petId}/health-records/upcoming`.
- La lectura requiere autenticación y respeta el acceso del refugio propietario o de una adopción formalizada; el alta está restringida al refugio propietario y ADMIN.
- `GET /affiliates` catálogo público de aliados. Administración: `GET/POST /admin/affiliates`, `PATCH/DELETE /admin/affiliates/{id}`.
- `GET /pets/{petId}/veterinary-assistant/context`; `POST /pets/{petId}/veterinary-assistant/messages` (sesión autenticada y acceso autorizado a la mascota).

## Solicitudes y adopciones

- `POST /adoption-applications`, `GET /adoption-applications/mine`, `GET /adoption-applications/{id}`, `PATCH /adoption-applications/{id}/status`, `GET /adoption-applications/{id}/history`.
- `GET /shelters/me/applications`, `GET /shelters/me/adoptions`.
- `POST /adoptions`, `GET /adoptions/mine`, `GET /adoptions/{id}`. Aprobar una solicitud no formaliza por sí sola la adopción.

## Seguimiento, favoritos, mensajes y notificaciones

- `POST/GET /adoptions/{id}/follow-ups`, `PATCH /follow-ups/{id}`, `POST /follow-ups/{id}/media`, `GET /follow-up-media/{id}`.
- `GET /favorites`, `POST /favorites/{petId}`, `DELETE /favorites/{petId}`.
- `GET/POST /conversations`, `GET/POST /conversations/{id}/messages`.
- `GET /notifications`, `PATCH /notifications/{id}/read`.
- Los recordatorios de salud se generan automáticamente hasta siete días antes de la fecha programada y se deduplican por registro y fecha.

Esquemas interactivos: `/swagger-ui.html`; documento JSON: `/api-docs`.

## Asistente veterinario

El asistente usa únicamente contexto no identificable de la mascota autorizado para el usuario: especie, raza, edad, sexo, tamaño, peso, color, etapa, resumen/condición de salud, vacunación, esterilización, desparasitación y requisitos de cuidado. No se envían nombre del usuario, correo, teléfono, refugio ni registros personales. La orientación es preventiva, no es un diagnóstico ni una receta y siempre debe confirmarse con un veterinario. Si `VETERINARY_AI_ENABLED=false` o falta `VETERINARY_AI_API_KEY`, el backend responde `503` y el frontend informa que el servicio no está disponible.
