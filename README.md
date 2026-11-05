# PetHop

PetHop conserva su frontend Vue 3 + Vite en la raíz del repositorio y agrega una API Java 21 / Spring Boot con PostgreSQL y Flyway en `backend/`.

## Desarrollo local

Requisitos: Node.js compatible con Vite 6, Java 21 y Docker Desktop con Compose.

1. Copia `.env.example` como `.env` y cambia `POSTGRES_PASSWORD` y `DB_PASSWORD` al mismo valor local.
2. Desde la raíz, inicia PostgreSQL: `docker compose up -d postgres`.
3. En una terminal: `cd backend; .\mvnw.cmd spring-boot:run` (Linux/macOS: `./mvnw spring-boot:run`). Flyway crea el esquema al iniciar.
4. En otra terminal de la raíz: `npm install` y `npm run dev`.
5. La API queda en `http://localhost:8080/api/v1`; OpenAPI en `http://localhost:8080/swagger-ui.html`; salud en `/actuator/health`.

El frontend usa `VITE_API_URL` (por defecto `http://localhost:8080/api/v1` durante desarrollo). En un build de producción, una URL localhost configurada se reemplaza por la ruta same-origin `/api/v1`; define `VITE_API_URL` con la URL pública de la API si frontend y backend están separados. Las imágenes se guardan en `FILE_STORAGE_PATH` (por defecto `backend/var/pethop-files` si se inicia Maven desde `backend/`). `.env`, el volumen PostgreSQL y archivos cargados son locales y no deben desplegarse al repositorio.

Para ejecutar el backend en Compose con almacenamiento persistente: `docker compose --profile app up --build`. El servicio usa el perfil `prod`, monta el volumen `pethop-file-storage` en `/var/lib/pethop/files` y ejecuta como usuario no root; no elimines ese volumen durante una actualización. Compose usa `COMPOSE_DB_URL` si se define y, en desarrollo local, conecta por defecto con el servicio `postgres`; al ejecutar Maven directamente se usa `DB_URL`. En producción, proporciona `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_ORIGINS`, `SESSION_COOKIE_SAME_SITE` y `FILE_STORAGE_PATH` (y `COMPOSE_DB_URL` si la base no es el servicio Compose). La configuración `prod` falla al iniciar si faltan los valores requeridos, mantiene Hibernate en `validate` y desactiva Swagger/OpenAPI públicos.

## Validación

- Frontend: `npm test` y `npm run build`.
- Backend: desde `backend/`, `./mvnw test` o `mvnw.cmd test`.
- Los tests con Testcontainers requieren Docker. En CI configura Docker o PostgreSQL de prueba antes de ejecutar la suite.

Consulta [docs/setup.md](docs/setup.md), [docs/railway-deploy.md](docs/railway-deploy.md), [docs/database-model.md](docs/database-model.md), [docs/api-endpoints.md](docs/api-endpoints.md) y [docs/backend-audit.md](docs/backend-audit.md).

## Producción

No uses los valores del `.env.example`. Configura secretos administrados, `SESSION_COOKIE_SECURE=true`, HTTPS, una lista exacta de orígenes CORS y almacenamiento de objetos duradero. Si el frontend y la API quedan en sitios distintos, ajusta `SESSION_COOKIE_SAME_SITE=none`. Restringe Actuator, limita tamaño/tasa de cargas, realiza copias de seguridad de PostgreSQL y de los archivos, y revisa migraciones antes de desplegarlas. El almacenamiento local está pensado solo para desarrollo.

El limitador de solicitudes es en memoria y está pensado para una sola instancia inicial. Las sesiones HTTP también son locales al proceso; para múltiples réplicas se necesitarán sesiones compartidas y rate limiting distribuido.

El asistente de orientación veterinaria es opcional y está desactivado por defecto. Para activarlo en el backend configura `VETERINARY_AI_ENABLED`, `VETERINARY_AI_API_KEY`, `VETERINARY_AI_BASE_URL` y `VETERINARY_AI_MODEL`; nunca pongas la clave en variables `VITE_*`. Solo envía contexto veterinario autorizado y no sustituye una consulta profesional. Sin clave o ante una falla del proveedor, informa indisponibilidad mediante `503`.
