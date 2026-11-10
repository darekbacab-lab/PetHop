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

Terminal 2, backend: `cd backend` y `./mvnw spring-boot:run` (Windows PowerShell: `./mvnw.cmd spring-boot:run`). Flyway aplica automáticamente las migraciones disponibles. Hibernate valida el esquema.

Terminal 3, frontend: desde la raíz, `npm install` y `npm run dev`.

Para parar PostgreSQL: `docker compose down`. Los datos permanecen en el volumen nombrado; `docker compose down -v` sí los elimina.

## Sesiones, archivos y despliegue

Cookies HttpOnly de sesión, protección CSRF y contraseñas BCrypt. En producción activa HTTPS y `SESSION_COOKIE_SECURE=true`, define orígenes exactos en `FRONTEND_ORIGINS`; si frontend y API están en sitios distintos, configura `SESSION_COOKIE_SAME_SITE=none`. Usa secretos gestionados, base administrada, almacenamiento de objetos y políticas de backup/retención. No expongas los endpoints de Actuator más allá de salud.

### Límites de solicitudes y sesiones

El backend aplica límites fijos en memoria: login (10 por IP cada 60 segundos), registro (5 por IP cada hora), cargas de imágenes (30 por usuario o IP cada 60 segundos) y reportes (10 por usuario o IP cada hora). Al superar un límite responde `429 Too Many Requests` con `Retry-After`. Ajusta los valores mediante las variables `RATE_LIMIT_*`. `RATE_LIMIT_TRUST_FORWARDED_HEADERS` debe permanecer en `false` salvo que exista un proxy confiable y controlado que sobrescriba correctamente `X-Forwarded-For`; nunca se deben aceptar esos encabezados directamente desde Internet.

El limitador es local a cada proceso y se reinicia al reiniciar el backend. Es suficiente para una primera instalación de una sola instancia. Si se usan varias réplicas, cada una tendrá contadores y sesiones HTTP distintos: una solicitud puede llegar a otra instancia y perder la sesión o repartir el límite. Antes de escalar horizontalmente se necesitará un almacén compartido de sesiones y un limitador distribuido, por ejemplo Redis; no forman parte de esta implementación.

El almacenamiento de archivos local es solo para desarrollo; perder `FILE_STORAGE_PATH` implica perder imágenes, aunque PostgreSQL conserve sus metadatos. En Compose, el backend opcional se inicia con `docker compose --profile app up --build`, activa el perfil `prod`, ejecuta como usuario no root y usa el volumen `pethop-file-storage` montado en `/var/lib/pethop/files`. Para producción, `FILE_STORAGE_PATH` debe apuntar a un volumen persistente respaldado y no a una capa efímera del contenedor. Compose usa `COMPOSE_DB_URL` si se define y conecta por defecto con el servicio PostgreSQL local; el arranque directo con Maven exige `DB_URL`. El perfil `prod` exige `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_ORIGINS`, `SESSION_COOKIE_SAME_SITE` y `FILE_STORAGE_PATH`; además desactiva Swagger/OpenAPI públicos.

El asistente veterinario está desactivado por defecto. Para habilitarlo configura `VETERINARY_AI_ENABLED=true`, `VETERINARY_AI_API_KEY`, `VETERINARY_AI_BASE_URL` y `VETERINARY_AI_MODEL`. La clave nunca se expone al frontend. Si el proveedor falta, falla o devuelve JSON inválido, el usuario recibe un error seguro; no se inventa una respuesta.

La recuperación de contraseña usa tokens aleatorios de un solo uso, almacenados únicamente como hash y con expiración de 30 minutos. Para habilitarla configura en `.env` `PASSWORD_RESET_ENABLED=true`, `PASSWORD_RESET_MAIL_ENABLED=true`, `PASSWORD_RESET_FRONTEND_URL`, `PASSWORD_RESET_MAIL_FROM` y las variables SMTP (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`). Docker Compose pasa estas variables al backend. Sin una cuenta SMTP válida, la recuperación permanece desactivada y el correo no se envía. La solicitud responde de forma uniforme aunque el correo no exista. El cambio de contraseña desde el perfil requiere la contraseña actual; el enlace de recuperación invalida los enlaces anteriores y no inicia sesión automáticamente.
