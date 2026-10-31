# PetHop

PetHop conserva su frontend Vue 3 + Vite en la raíz del repositorio y agrega una API Java 21 / Spring Boot con PostgreSQL y Flyway en `backend/`.

## Desarrollo local

Requisitos: Node.js compatible con Vite 6, Java 21 y Docker Desktop con Compose.

1. Copia `.env.example` como `.env` y cambia `POSTGRES_PASSWORD` y `DB_PASSWORD` al mismo valor local.
2. Desde la raíz, inicia PostgreSQL: `docker compose up -d postgres`.
3. En una terminal: `cd backend; .\mvnw.cmd spring-boot:run` (Linux/macOS: `./mvnw spring-boot:run`). Flyway crea el esquema al iniciar.
4. En otra terminal de la raíz: `npm install` y `npm run dev`.
5. La API queda en `http://localhost:8080/api/v1`; OpenAPI en `http://localhost:8080/swagger-ui.html`; salud en `/actuator/health`.

El frontend usa `VITE_API_URL` (por defecto `http://localhost:8080/api/v1`). Las imágenes se guardan en `FILE_STORAGE_PATH` (por defecto `backend/var/pethop-files` si se inicia Maven desde `backend/`). `.env`, el volumen PostgreSQL y archivos cargados son locales y no deben desplegarse al repositorio.

## Validación

- Frontend: `npm test` y `npm run build`.
- Backend: desde `backend/`, `./mvnw test` o `mvnw.cmd test`.
- Los tests con Testcontainers requieren Docker. En CI configura Docker o PostgreSQL de prueba antes de ejecutar la suite.

Consulta [docs/setup.md](docs/setup.md), [docs/database-model.md](docs/database-model.md), [docs/api-endpoints.md](docs/api-endpoints.md) y [docs/backend-audit.md](docs/backend-audit.md).

## Producción

No uses los valores del `.env.example`. Configura secretos administrados, `SESSION_COOKIE_SECURE=true`, HTTPS, una lista exacta de orígenes CORS y almacenamiento de objetos duradero. Si el frontend y la API quedan en sitios distintos, ajusta `SESSION_COOKIE_SAME_SITE=none`. Restringe Actuator, limita tamaño/tasa de cargas, realiza copias de seguridad de PostgreSQL y de los archivos, y revisa migraciones antes de desplegarlas. El almacenamiento local está pensado solo para desarrollo.
