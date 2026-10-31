# API REST v1

Base: `/api/v1`. DTOs de solicitud y respuesta separados de las entidades. Las respuestas paginadas contienen `content`, `page`, `size`, `totalElements`, `totalPages`. Errores de validación se devuelven como Problem Details. La autenticación usa cookies de sesión; las mutaciones requieren `X-XSRF-TOKEN` obtenido desde `GET /auth/csrf`.

## Autenticación y usuarios

- `GET /auth/csrf` obtiene token CSRF.
- `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`.
- `GET/PATCH /users/me`, `PATCH /users/me/password`.

## Refugio y mascotas

- `POST /shelters`, `GET/PATCH /shelters/me`.
- `GET /pets` catálogo público; `GET /pets/mine` propias publicaciones. Parámetros: especie, sexo, tamaño, ubicación, búsqueda, orden, página y tamaño de página.
- `GET /pets/{id}`, `POST /pets`, `PATCH /pets/{id}`, `PATCH /pets/{id}/status`, `DELETE /pets/{id}` (baja lógica).
- `GET /pets/{id}/images`, `POST /pets/{id}/images`, `DELETE /pets/{id}/images/{imageId}`, `GET /pet-images/{id}`.
- `GET/POST /pets/{petId}/health-records`; `GET /pets/{petId}/health-records/upcoming`.

## Solicitudes y adopciones

- `POST /adoption-applications`, `GET /adoption-applications/mine`, `GET /adoption-applications/{id}`, `PATCH /adoption-applications/{id}/status`, `GET /adoption-applications/{id}/history`.
- `GET /shelters/me/applications`, `GET /shelters/me/adoptions`.
- `POST /adoptions`, `GET /adoptions/mine`, `GET /adoptions/{id}`. Aprobar una solicitud no formaliza por sí sola la adopción.

## Seguimiento, favoritos, mensajes y notificaciones

- `POST/GET /adoptions/{id}/follow-ups`, `PATCH /follow-ups/{id}`, `POST /follow-ups/{id}/media`, `GET /follow-up-media/{id}`.
- `GET /favorites`, `POST /favorites/{petId}`, `DELETE /favorites/{petId}`.
- `GET/POST /conversations`, `GET/POST /conversations/{id}/messages`.
- `GET /notifications`, `PATCH /notifications/{id}/read`.

Esquemas interactivos: `/swagger-ui.html`; documento JSON: `/api-docs`.
