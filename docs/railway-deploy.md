# Despliegue de PetHop en Railway

PetHop se despliega como tres servicios independientes dentro del mismo proyecto Railway:

1. `frontend`: el repositorio completo con **Root Directory** `/`.
2. `backend`: el mismo repositorio con **Root Directory** `/backend`.
3. PostgreSQL administrado por Railway.

No se debe desplegar el repositorio completo como un único proyecto pnpm. El frontend no es un workspace pnpm: usa npm y el backend es una aplicación Maven independiente.

## Frontend

Configura el servicio frontend con estos valores:

- Root Directory: `/`
- Build Command: `npm ci && npm run build`
- Start Command: `npm start`
- Watch Paths: `/src/**`, `/public/**`, `/*.html`, `/package.json`, `/package-lock.json`, `/vite.config.js`, `/server.mjs`

El build genera `dist/`. `server.mjs` sirve los archivos estáticos, escucha el `PORT` que Railway inyecta y devuelve `index.html` para rutas SPA sin extensión. Las páginas multipágina existentes (`acceso.html`, `mascota.html`, etc.) continúan sirviéndose como archivos reales.

Define en el servicio frontend:

```text
VITE_API_URL=https://<dominio-publico-del-backend>/api/v1
```

La URL debe ser la del servicio backend, sin poner secretos ni claves de proveedores en ninguna variable `VITE_*`. Después de cambiar `VITE_API_URL` hay que crear un nuevo deployment porque se incorpora durante el build.

## Backend

Configura un segundo servicio apuntando al mismo repositorio:

- Root Directory: `/backend`
- Dockerfile: `Dockerfile` (detección automática)
- Healthcheck path: `/actuator/health`

El Dockerfile compila con Maven y ejecuta el JAR con Java 21. La aplicación acepta el `PORT` dinámico de Railway; localmente conserva el puerto 8080.

Variables mínimas del backend:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=${{Postgres.DATABASE_URL}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
FRONTEND_ORIGINS=https://<dominio-publico-del-frontend>
SESSION_COOKIE_SAME_SITE=none
SESSION_COOKIE_SECURE=true
FILE_STORAGE_PATH=/var/lib/pethop/files
```

`DB_URL`, `DB_USERNAME` y `DB_PASSWORD` deben ser referencias Railway al servicio PostgreSQL, no valores escritos en el repositorio. El perfil `prod` mantiene Flyway habilitado y Hibernate en `validate`; si faltan las variables obligatorias, el backend no debe arrancar con valores locales.

Para Paw, añade las variables solo en el backend:

```text
VETERINARY_AI_ENABLED=true
VETERINARY_AI_BASE_URL=https://api.groq.com/openai/v1/chat/completions
VETERINARY_AI_MODEL=openai/gpt-oss-20b
VETERINARY_AI_API_KEY=<clave configurada como secreto Railway>
```

Para habilitar recuperación de contraseña, configura además estas variables únicamente en el backend. Usa credenciales SMTP de un proveedor de correo; no las guardes en Git ni en variables `VITE_*`:

```text
PASSWORD_RESET_ENABLED=true
PASSWORD_RESET_MAIL_ENABLED=true
PASSWORD_RESET_FRONTEND_URL=https://<dominio-publico-del-frontend>
PASSWORD_RESET_MAIL_FROM=no-reply@<dominio-controlado>
PASSWORD_RESET_TOKEN_TTL_MINUTES=30
SMTP_HOST=<servidor-smtp>
SMTP_PORT=587
SMTP_USERNAME=<usuario-smtp>
SMTP_PASSWORD=<secreto-smtp>
SMTP_AUTH=true
SMTP_STARTTLS=true
```

Si todavía no tienes un proveedor SMTP, deja `PASSWORD_RESET_ENABLED=false` y `PASSWORD_RESET_MAIL_ENABLED=false`; la aplicación seguirá iniciando y mostrará que la recuperación no está disponible, sin fingir que se envió un correo.

La clave no debe aparecer en `VITE_*`, commits, logs ni documentación. Si se deja `VETERINARY_AI_ENABLED=false`, el resto de la aplicación funciona y el asistente informa que está no disponible.

## Persistencia de imágenes

El backend guarda las imágenes en `FILE_STORAGE_PATH`. La capa de archivos del contenedor no es persistente por sí sola. Crea un **Volume** Railway conectado al servicio backend con mount path exacto:

```text
/var/lib/pethop/files
```

Mantén `FILE_STORAGE_PATH` con ese mismo valor. Railway monta los volúmenes con propietario root; como el Dockerfile ejecuta la aplicación como usuario no root, configura `RAILWAY_RUN_UID=0` en el servicio backend para que pueda escribir en el volumen. Esto es una limitación operativa de este almacenamiento local; antes de escalar a varias réplicas conviene migrar a almacenamiento de objetos.

No elimines ni desconectes el volumen durante una actualización. La base de datos conserva los metadatos, pero sin el volumen los archivos no pueden recuperarse desde el contenedor.

## Orden de configuración

1. Crea PostgreSQL y espera a que esté disponible.
2. Crea el backend, configura sus variables y despliega. Comprueba `/actuator/health` y los logs de Flyway.
3. Crea el frontend con `VITE_API_URL` apuntando al backend y despliega.
4. Copia el dominio final del frontend a `FRONTEND_ORIGINS` del backend y vuelve a desplegar el backend.
5. Prueba login, CSRF, carga de imagen, catálogo público y Paw desde el dominio HTTPS.

Las sesiones HTTP y el rate limit actuales son locales al proceso. Una sola instancia de backend es el despliegue compatible con esta configuración. Varias réplicas requieren un almacén compartido de sesiones y un limitador distribuido; no se habilitan en este cambio.
