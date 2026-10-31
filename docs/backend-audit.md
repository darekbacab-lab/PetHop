# Auditoría de backend de PetHop

Fecha: 2026-10-30

## Estado de esta auditoría

PetHop es una aplicación Vue 3 + Vite que conserva rutas `.html` históricas. No existe backend, API REST, configuración de base de datos, autenticación de servidor ni almacenamiento remoto de imágenes. La auditoría se completó antes de crear código de backend; la integración posterior debe hacerse por etapas y mantener operativo el frontend mientras cada flujo se migra.

## Estructura y tecnología actual

- `src/main.js`, `src/App.vue`, `src/router/index.js`: montaje Vue, estado global visual y rutas Vue Router.
- `src/views/`: `HomeView.vue`, `AccessView.vue`, `ExploreView.vue`, `PetView.vue`, `AdoptionView.vue`, `FollowUpView.vue`, `AdopterMessagesView.vue` y `ProviderView.vue`.
- `src/components/`: encabezado, pie, tarjeta de mascota, iconos, tema, esqueletos y asistente.
- `src/services/storage.js`, `src/services/pets.js`: adaptador de Web Storage y acceso al catálogo/favoritos locales.
- `js/pethop-data.js`: compatibilidad de directorio publicado e hilos de mensajes con las claves históricas.
- `js/proveedor.js`, `js/acceso.js`, `app.js` y `src/legacy/pages/*.html`: referencias históricas; la aplicación Vue activa no los carga como controladores de página. No deben migrarse dos veces.
- `package.json`: Vue 3, Vue Router 4 y Vite 6; sin cliente HTTP ni dependencias de backend.
- No se encontró `README.md`, `docs/` previo, `AGENTS.md`, `.env.example`, Compose ni backend. `.gitignore` solo ignora `node_modules/`, `dist/` y `.vite/`.
- No hay rutas protegidas ni identidad de usuario confiable en servidor. La navegación a vistas privadas solo está simulada por interfaz.

## Funcionalidad y persistencia observadas

| Área / archivos | Situación actual | Persistencia o límite actual |
|---|---|---|
| Inicio y registro de refugio (`HomeView.vue`) | Catálogo destacado; formulario de organización local. | Publicaciones a `pethop_provider_workspace`; no se crea una cuenta ni un perfil de refugio de servidor. |
| Acceso y registro (`AccessView.vue`) | Comprueba HTML, coincidencia de contraseña y navegación; el login no autentica. El registro de adoptante no crea cuenta. | Registro de proveedor guarda perfil mínimo en `pethop_provider_workspace`. No se persiste contraseña. |
| Catálogo y detalle (`ExploreView.vue`, `PetView.vue`, `PetCard.vue`) | Busca/filtra/ordena datos locales y abre el detalle; imágenes de ejemplo. | `staticPets` en `src/services/pets.js`, publicaciones locales del directorio y `pethop_selected_pet` en `sessionStorage`. |
| Favoritos (`pets.js`, `SiteHeader.vue`, `PetCard.vue`) | Agregar/quitar y modal de favoritos funcional en el dispositivo. | `pethop_favorites` en `localStorage`, ligado al navegador y no a una cuenta autenticada. |
| Publicación y panel de refugio (`ProviderView.vue`) | Asistente de 7 pasos, edición, filtros, perfiles y acciones de estado locales. | `PETS` y `DEMO_REQUESTS` hardcodeados; workspace y borrador en `pethop_provider_workspace`. Fotos procesadas a JPEG/base64 y guardadas localmente. Restablecer datos es una operación de demo. |
| Solicitud de adopción (`AdoptionView.vue`) | Formulario y confirmación local. | Agrega registros a `pethop_applications`; también guarda datos de contacto en `pethop_user`. No hay verificación de sesión, unicidad ni expediente de estado separado. |
| Revisiones del refugio (`ProviderView.vue`) | Lee las solicitudes del mismo dispositivo y cambia estado local. | Modifica `pethop_applications` y el workspace; sin control de permisos, historial transaccional ni notificación real. |
| Seguimiento (`FollowUpView.vue`) | Muestra etapas derivadas del estado local y permite agregar notas. | `pethop_followups`; la propia interfaz aclara que son notas locales y no cambian el estado oficial. No hay adopciones formalizadas ni permisos. |
| Mensajería (`PetView.vue`, `AdopterMessagesView.vue`, `ProviderView.vue`, `js/pethop-data.js`) | Hilos REST-like locales entre vistas, envío y listado en el dispositivo. | `pethop_messages`, con compatibilidad de formato antiguo/workspace. No hay identidad, participantes autenticados, autorización ni sondeo de servidor. |
| Asistente (`AssistantChat.vue`) | Respuestas/atajos predefinidos en UI; no hay proveedor de IA ni servicio sanitario conectado. | Solo estado de conversación en memoria. No debe anunciarse ni persistirse como diagnóstico o IA real. |
| Preferencias | Tema y filtros temporales. | `pethop_theme` y otros estados UI; pueden permanecer locales. |
| Notificaciones | No se encontró un centro de notificaciones funcional ni persistencia de notificaciones. | No implementar UI/API de notificaciones como terminado; dejar infraestructura de dominio hasta que existan eventos confirmados. |

## Inventario de almacenamiento web

- LocalStorage: `pethop_favorites`, `pethop_user`, `pethop_applications`, `pethop_followups`, `pethop_provider_workspace`, `pethop_messages`, `pethop_theme`.
- SessionStorage: `pethop_selected_pet` (selección temporal del detalle).
- Registros que deben migrarse con estrategia explícita: usuarios, perfiles de refugio, mascotas publicadas, solicitudes, sus estados y sus hilos de mensajes. Favoritos y seguimientos solo pueden asociarse a un usuario tras identificar/autenticarlo. Las fotos base64 deben migrarse a almacenamiento de archivos.
- Mantener locales el tema, filtros y estado temporal de navegación. No borrar datos de navegador al instalar el cliente HTTP. Para datos antiguos, ofrecer migración autenticada/consentida o export/import controlado; nunca aceptar su identidad o rol como fuente de autoridad.

## Entidades candidatas y límites de alcance

1. Núcleo: `users`, `shelter_profiles`, `pets`, `pet_images`.
2. Flujo ya presente: `adoption_applications`, `application_answers`, `application_status_history`, mensajería (`conversations`, `conversation_participants`, `messages`) y favoritos.
3. Seguimiento actual solo son notas, no adopción formal: se necesitan `adoptions`, `follow_up_reports`, `follow_up_media` para convertirlo en registro oficial, pero el backend no debe inventar aprobación automática ni cambiar el proceso de UI sin una etapa confirmada.
4. Expediente sanitario: el formulario de publicación captura resumen/flags; no se encontró una interfaz para administrar registros clínicos históricos. Preparar el modelo solicitado, pero conectar la interfaz de historial cuando se incorpore explícitamente.
5. Notificaciones: no hay bandeja actual. Los eventos se pueden emitir desde las transacciones que correspondan, pero no afirmar que el usuario ya puede consultar notificaciones.
6. IA/triaje: no existe proveedor ni flujo clínico. No crear resultados médicos; una futura integración requiere proveedor real, consentimiento y aviso visible de que no sustituye a un veterinario.
7. Pagos, geolocalización precisa, verificación documental, chat en tiempo real y microservicios no están presentes ni requeridos; quedan fuera.

## Endpoints necesarios para los flujos existentes

- `/api/v1/auth`: registro, login, logout, usuario actual y cambio de contraseña.
- `/api/v1/shelters/me`: perfil autenticado de refugio; aprobación/verificación queda restringida a ADMIN y no debe simularse.
- `/api/v1/pets`: catálogo paginado y filtros públicos disponibles; detalle; CRUD restringido al refugio dueño; baja lógica; imágenes.
- `/api/v1/pets/{id}/health-records`: historial separado, pendiente de integrar a una interfaz de refugio.
- `/api/v1/adoption-applications`: envío, listado del adoptante, detalle autorizado, solicitudes del refugio, cambio de estado e historial.
- `/api/v1/favorites`: listado/agregar/eliminar para la identidad autenticada.
- `/api/v1/conversations`: listado, crear/recuperar hilo permitido, mensajes paginados y enviar mensaje con verificación de participante.
- `/api/v1/adoptions/{id}/follow-ups`: reportes y medios solo cuando hay adopción formal y participante autorizado.
- `/api/v1/notifications`: requiere una decisión/UI de producto; la API no basta para declarar esta función implementada.

## Riesgos y plan de integración

- Los datos demo actuales no tienen cuentas ni relaciones estables; no importarlos automáticamente como información real ni permitir que cualquier visitante los publique.
- Hay múltiples representaciones/formatos de mascota, solicitud y mensaje (local y legacy). Definir DTO de API y un mapeo único por servicio Vue antes de retirar compatibilidad.
- El registro y login actuales no persisten contraseña y no cuentan con guards; un cambio parcial puede dar falsa sensación de seguridad. Migrar auth y guards antes de habilitar escrituras privadas.
- Las fotos se guardan como data URLs en almacenamiento local; el backend debe validar firma MIME, tamaño y extensión y el cliente debe enviar multipart, sin confiar en `Content-Type` declarado.
- Evitar perder el catálogo durante el cambio: el backend debe sembrar ejemplos idempotentes solo con perfil `dev`; no sobrescribir datos del usuario.
- No borrar automáticamente `localStorage`. Preferencias quedan locales; registros viejos requieren decisión de migración autenticada, con compatibilidad y respaldo.
- Los endpoints sensibles deben derivar el usuario del `SecurityContext`, verificar propiedad y responder errores `ProblemDetail`; CORS no debe ser comodín con cookies.
- La interfaz actual no establece política de retención de mensajes, documentos sanitarios o datos personales. Mantener la información mínima y documentar decisiones antes de producción.

## Estado de ejecución detectado

El frontend tiene scripts `npm run dev`, `npm run build`, `npm test`. No se detectaron ejecutables `mvn` o `docker` en PATH durante la auditoría. Java requiere comprobación del runtime instalado; hasta confirmar herramienta/descargas del wrapper, la compilación de backend no se debe declarar exitosa. El frontend sí tiene una compilación Vite y pruebas Node existentes.

## Implementación incorporada (2026-10-30)

- Se agregó `backend/` como monolito modular Java 21 / Spring Boot 3.5.x con Maven Wrapper, configuración por variables de entorno y Springdoc.
- Flyway define el modelo relacional y restricciones en V1-V8: cuentas y refugios, catálogo e imágenes, expediente sanitario, solicitudes y estados, adopciones y seguimientos, favoritos, conversaciones y mensajes, y notificaciones.
- La API usa sesiones de servidor, BCrypt, CSRF, validación, roles y autorización por participante/propietario. PostgreSQL se levanta con Compose y los archivos subidos se guardan fuera de la base en almacenamiento local durante desarrollo.
- Se conectaron a la API los flujos actuales de registro/acceso, catálogo, detalle/favoritos, solicitud, panel de refugio, mensajes y seguimiento. Preferencias visuales y algunos estados temporales de navegación continúan locales. La información demo histórica no se migra automáticamente.
- Se añadieron endpoints y tablas para historial sanitario y notificaciones, pero todavía no hay una interfaz para administrar/consultar esos módulos. El asistente sigue siendo contenido predefinido y no se presenta como IA ni diagnóstico real.
- La auditoría original de arriba describe el estado previo a la implementación; conserva su valor como inventario de origen.

## Verificación posterior

- `npm test`: correcto, 18 pruebas.
- `npm run build`: correcto con Vite.
- `git diff --check`: sin errores de espacios; Git advierte que normalizará los finales de línea de `.gitignore` según la configuración local.
- No se pudo compilar ni probar Java: Maven Wrapper no pudo resolver Maven Central por una falla DNS de red en el entorno. Docker tampoco está disponible en PATH, por lo que PostgreSQL/Testcontainers y el recorrido integrado quedan pendientes de ejecutar en una máquina con Java 21, red y Docker.
- Esta limitación impide declarar el backend compilado o desplegable hasta ejecutar `cd backend; .\\mvnw.cmd test` y completar el recorrido local indicado en `docs/setup.md`.
