# Migración de PetHop a Vue 3 + Vite

## Estado

La conversión de las siete rutas a vistas Vue declarativas está implementada. Ninguna vista usa `v-html`, importa el HTML archivado ni carga `app.js`, `js/acceso.js` o `js/proveedor.js` como controlador. `LegacyPageRenderer.vue` y `legacyRuntime.js` ya no forman parte de la aplicación.

Las vistas convertidas son:

| Ruta | Vista Vue | Funciones principales trasladadas |
| --- | --- | --- |
| `/` | `HomeView.vue` | Inicio, búsqueda, mascotas destacadas, favoritos, registro local de refugio y enlaces de proceso. |
| `/acceso.html` | `AccessView.vue` | Formularios de acceso y registro, tipo de perfil, validación, visibilidad de contraseña y almacenamiento local del perfil proveedor. |
| `/explorar.html` | `ExploreView.vue` | Búsqueda, filtros aplicados por botón, ordenamiento, favoritos, vacíos y consulta de publicaciones públicas. |
| `/mascota.html` | `PetView.vue` | Datos y galería de mascota, botones, teclado y gesto táctil, favoritos, navegación a solicitud y conversación local con el proveedor. |
| `/solicitud.html` | `AdoptionView.vue` | Formulario de adopción, validación, confirmación de responsabilidad y guardado local de solicitudes y perfil. |
| `/seguimiento.html` | `FollowUpView.vue` | Estado de solicitud, pasos del proceso, notas locales y listado histórico. |
| `/proveedor.html` | `ProviderView.vue` | Selección de perfil, panel adaptable, publicaciones, filtros, asistente de publicación y fotos, solicitudes, proceso, seguimiento, mensajes, perfil, preferencias y ayuda. |

## Arquitectura actual

- `src/router/index.js` registra las rutas históricas con Vue Router.
- `src/components/SiteHeader.vue`, `SiteFooter.vue`, `PetCard.vue` e `Icon.vue` contienen elementos reutilizables. `Icon.vue` renderiza trazados declarativos exportados desde la copia local de Lucide v1.52.0 (`js/vendor/lucide.min.js`); no inicializa el renderer DOM ni agrega una dependencia npm. El encabezado gestiona el menú adaptable y el resumen modal de favoritos.
- `src/views/` contiene las siete plantillas y la lógica reactiva de página.
- `src/services/storage.js` encapsula `localStorage` y `sessionStorage`; `src/services/pets.js` conecta catálogo, publicaciones y favoritos.
- `js/pethop-data.js` se conserva como módulo de datos compatible con las claves y estructuras históricas de PetHop para directorio público, almacenamiento de mensajes e intercambio de conversaciones. No representa un controlador DOM y no carga páginas.
- `src/App.vue` conserva el indicador de progreso de desplazamiento y el atajo `/` para enfocar la búsqueda.
- `src/icons/lucide-icons.js` conserva como datos SVG las definiciones Lucide originales usadas por Vue. `css/styles.css`, `css/acceso.css`, `css/solicitud.css` e `img/` siguen siendo recursos activos. Los HTML de raíz son shells de entrada a Vite.

## Persistencia y límites

La aplicación conserva las claves locales existentes, entre ellas `pethop_user`, `pethop_favorites`, `pethop_selected_pet` (sesión), `pethop_applications`, `pethop_followups`, `pethop_provider_workspace` y `pethop_messages`. El adaptador central mantiene JSON y maneja fallos de almacenamiento.

El acceso y registro no autentican usuarios con un servidor. Las solicitudes, preferencias, publicaciones, perfiles y mensajes permanecen en el navegador. La carga de fotos comprime imágenes localmente y guarda datos en el dispositivo; no existe una subida remota. No se implementaron backend, servicios externos ni Paw/IA. Los datos de demostración originales del panel siguen siendo datos del prototipo.

## Dependencias antiguas que permanecen

- `src/legacy/pages/*.html` se conserva como referencia para comparar contenido, rutas y formularios; las vistas no lo importan ni lo leen en ejecución.
- `app.js`, `js/acceso.js`, `js/proveedor.js` y los scripts de navegación originales permanecen en el repositorio como código de referencia. No se cargan desde las siete entradas activas ni se copian al build.
- `js/pethop-data.js` sí es importado como servicio de datos compartido, no como renderer ni controlador de eventos.
- Los estilos originales y el directorio de imágenes se mantienen porque las vistas Vue todavía usan sus reglas visuales y recursos.

Una búsqueda en las vistas, componentes, `App.vue`, `main.js` y las entradas HTML no encontró referencias a `v-html`, `LegacyPageRenderer`, `legacyRuntime` ni a los controladores de interfaz antiguos. La salida actual de producción contiene bundles Vue y recursos visuales, no `app.js` ni los controladores antiguos.

## Verificación ejecutada

- `node --test tests/*.test.js`: 18 de 18 pruebas pasan. Además de persistencia, compatibilidad de datos y estructura declarativa, hay regresiones estáticas para menú, tarjetas, registro, uso de trazados Lucide originales, color/estado de favoritos y la composición y navegación del panel proveedor.
- `node node_modules/.pnpm/vite@6.4.5/node_modules/vite/bin/vite.js build`: compilación de producción completada; Vite transformó 53 módulos y generó las siete entradas y sus bundles. Se invocó Vite directamente porque `npm` no está disponible en el shell; `pnpm` intentó crear un enlace simbólico que el entorno denegó (`EPERM`). No se instalaron dependencias.
- `git diff --check`: completado; solo reportó avisos de conversión de finales de línea Git (LF/CRLF), sin errores de whitespace.
- Las causas de los defectos revisados fueron: backdrop con `z-index: 1100` superior al contexto del encabezado (`1000`), también en la entrada del panel; modales Vue sin la clase `.open` que activa visibilidad e interacción; y una tarjeta Vue con botón extra y jerarquía distinta a la tarjeta original. La capa del backdrop quedó por debajo del encabezado, el encabezado del proveedor usa el mismo nivel, los modales implicados aplican `.open`, y `PetCard.vue` vuelve a la estructura original con apertura por tarjeta, teclado y favorito independiente.
- Se intentó abrir el servidor local desde el navegador integrado en `127.0.0.1:5174`; el navegador devolvió `net::ERR_CONNECTION_TIMED_OUT`. Por ello, los clics y recorridos reales de rutas no quedan verificados en navegador. La compilación y las pruebas estáticas no sustituyen esa verificación.
- En la revisión visual posterior se confirmó en los archivos fuente que el original sí utilizaba Lucide: el HTML declara `data-lucide`, `js/vendor/lucide.min.js` provee v1.52.0 y `app.js`/`js/proveedor.js` invocan `lucide.createIcons()`. Vue no incluía Lucide ni esos SVG, y había reemplazado parte de la navegación por caracteres. Ahora usa un subconjunto local de los mismos nodos SVG, gestiona favorito activo con trazo/relleno `#3EB2BA`, y las tarjetas detienen la propagación del clic.
- La vista de Inicio del proveedor volvió a incluir las cuatro estadísticas con acción, actividad/solicitudes recientes, resumen del proceso y publicaciones recientes con fotografías y contador de solicitudes. El menú conserva las once secciones, iconos, indicador de solicitudes, perfil/tipo de cuenta y regreso a PetHop. En móvil abre como cajón con backdrop, cierre por Escape/clic y contenido fuera del foco al estar cerrado. Perfil recuperó indicador de verificación, avance de datos, resumen y tarjeta de acción; el botón de inicio ahora dice «Registrar refugio» y conserva el mismo formulario y guardado local.
- Esta pasada no pudo hacer una comprobación visual real de capturas/escritorio/móvil: el navegador de la aplicación bloqueó la observación de la URL local por su política de seguridad. Los cambios visuales se contrastaron con las plantillas y el renderer originales y sus reglas CSS, pero no se certifica el aspecto en un navegador en ejecución.

No se pudo completar una inspección de navegador: la herramienta de navegador no logró conectarse al servidor local Vite y la política del entorno bloqueó la conexión HTTP desde el shell con `SocketException (10013)`. Por tanto, no se declara verificado mediante ejecución real el comportamiento de escritorio/móvil, los flujos de formularios en navegador, gestos táctiles ni la lectura y escritura con datos de una sesión de usuario real. La compresión de fotos y los controles del panel quedan también pendientes de una pasada interactiva en navegador.

## Pendientes concretos

1. Ejecutar una revisión interactiva de las siete rutas en un navegador accesible, incluyendo el menú en escritorio y móvil, registro de refugio, detalle y favoritos, filtros, solicitud, seguimiento, mensajería, paneles, persistencia y carga de fotografías.
2. Completar la comparación visual a partir de capturas en escritorio y móvil. La comparación de esta pasada fue con las fuentes originales, ya que la inspección del navegador quedó bloqueada.
3. Añadir pruebas de componente o de navegador para interacciones; las 18 pruebas disponibles son de Node y varias comprueban código fuente, no ejecución de componentes en un DOM.

La aplicación ya no depende de los controladores DOM originales para su funcionamiento normal, aunque estos archivos y los HTML de referencia aún se conservan en el árbol del proyecto.

## Revisión visual de Seguimiento y panel (2026-10-22)

- Las capturas revelaron que los bloques de estado, mascota, formulario e historial eran hijos directos de la grilla de dos columnas: cada bloque se colocaba en una celda distinta y el resumen se desbordaba a otra fila. Se agruparon esos cuatro bloques dentro de la columna `.follow-up-main`, dejando el resumen y la ayuda en la columna secundaria.
- El wrapper `.follow-up-section` tenía reglas de tarjeta que envolvían todo el contenido, y `.follow-up-summary` tenía reglas de grid de dos columnas aunque su contenido es un resumen vertical. Se resolvieron en sus reglas originales y se agregaron breakpoints para pasar a una columna, apilar las tarjetas de apoyo y reorganizar el formulario en móvil. El encabezado ahora comparte ancho y alineación con el contenedor de contenido.
- El título de Seguimiento reutiliza los tokens, colores, escala y pesos del encabezado de Inicio (`.hero h1`), con su escala móvil correspondiente.
- El CSS del estado del proceso esperaba `.current` y `.completed`, mientras que Vue emite `.is-current` y `.is-complete`; los marcadores y la etapa actual no recibían sus estilos. Se añadieron selectores para ambas clases Vue. También se acotó el selector del badge de estado para que no aplique fondo y padding al eyebrow dentro del encabezado.
- Se inspeccionaron los estilos y breakpoints del panel del proveedor y los encabezados compartidos. No se hicieron cambios en esas vistas porque esta revisión no confirmó otra diferencia concreta respaldada por las reglas inspeccionadas.
- La compilación de producción posterior a la corrección actual completó correctamente (53 módulos) y `node --test tests/*.test.js` pasa 18/18. `git diff --check` no reporta errores de whitespace; permanecen avisos LF/CRLF.
- La revisión visual interactiva del nuevo layout sigue pendiente: Vite no pudo enlazar `127.0.0.1:5173` (`listen EACCES`). No se afirma que el resultado final esté validado visualmente en escritorio o móvil.
