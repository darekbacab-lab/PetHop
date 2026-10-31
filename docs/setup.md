# Puesta en marcha y configuración

## Requisitos

- Java 21 LTS.
- Docker Desktop con soporte `docker compose` para PostgreSQL.
- Node.js y npm para el frontend.
- La primera ejecución necesita red para descargar Maven y dependencias del repositorio Maven Central.

## Variables

Copia `.env.example` a `.env` en la raíz. `POSTGRES_PASSWORD` y `DB_PASSWORD` deben coincidir en desarrollo local. Configura `VITE_API_URL`, `SERVER_PORT`, `FRONTEND_ORIGINS`, `SESSION_COOKIE_SECURE` y `FILE_STORAGE_PATH` según tu entorno. No compartas `.env`.

## Iniciar

Terminal 1, raíz: `docker compose up -d postgres`.

Terminal 2, backend: `cd backend` y `./mvnw spring-boot:run` (Windows PowerShell: `./mvnw.cmd spring-boot:run`). Flyway aplica automáticamente las migraciones V1-V8. Hibernate valida el esquema.

Terminal 3, frontend: desde la raíz, `npm install` y `npm run dev`.

Para parar PostgreSQL: `docker compose down`. Los datos permanecen en el volumen nombrado; `docker compose down -v` sí los elimina.

## Sesiones, archivos y despliegue

Cookies HttpOnly de sesión, protección CSRF y contraseñas BCrypt. En producción activa HTTPS y `SESSION_COOKIE_SECURE=true`, define orígenes exactos en `FRONTEND_ORIGINS`; si frontend y API están en sitios distintos, configura `SESSION_COOKIE_SAME_SITE=none`. Usa secretos gestionados, base administrada, almacenamiento de objetos y políticas de backup/retención. No expongas los endpoints de Actuator más allá de salud.

El almacenamiento de archivos local es solo para desarrollo; perder `FILE_STORAGE_PATH` implica perder imágenes, aunque PostgreSQL conserve sus metadatos.
