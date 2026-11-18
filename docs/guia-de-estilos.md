# Guía de estilos de Pethop

La personalidad visual de Pethop es una sola y sale del inicio de sesión
(`AccessView.vue` + `css/acceso.css`): foto grande, titular fuerte con una
palabra en turquesa, mucho aire, radios grandes y sombra por elemento. Cada
apartado nuevo debe parecerse al login, no inventar lo suyo.

## Tokens (`css/styles.css`, `:root`)

No se escriben valores sueltos: todo color, radio y sombra es un token.

### Color

| Token | Valor | Uso |
| --- | --- | --- |
| `--primary` | `#3EB2BA` | Turquesa base. Nunca como fondo de texto blanco (2,54:1). |
| `--primary-strong` | `#167F87` | Botones primarios, enlaces, acentos. Blanco sobre él: 4,75:1. |
| `--primary-hover` | `#61BAC1` | Solo hover de elementos no textuales. |
| `--primary-light` | `#72C7CC` | Detalles y palabra en turquesa claro del titular. |
| `--hero-title-dark` | `#12515A` | Color de los titulares. |
| `--primary-dark` | `#2C5357` | Bandas oscuras (`DarkBand.vue`) y pie de página. |
| `--secondary` | `#90D6CD` | Turquesa pálido de superficies. |
| `--background` / `--primary-soft` | `#F3FEFF` | Fondo de página y superficies pálidas. |
| `--white` / `--surface` | `#FFFFFF` | Tarjetas y paneles. |
| `--text` | `#313434` | Texto principal (12,22:1 sobre el fondo). |
| `--text-soft` | `#4A5556` | Texto secundario. |
| `--border` | `#DDE8EB` | Bordes de tarjetas y paneles. |
| `--border-strong` | `#6E969B` | Bordes de controles de formulario (3,23:1). |
| `--tint` | `#EAF5F4` | Tinte turquesa pálido genérico. |
| `--success` / `--success-bg` | `#287451` / `#E6F5EC` | Estados positivos. |
| `--warning` / `--warning-bg` | `#8A5B12` / `#FFF4D9` | Avisos. `--warning-border: #F0C9A8`. |
| `--danger` / `--danger-bg` | `#B42318` / `#FFE5E3` | Errores. |

No existen azul (`#28628a`) ni morado (`#6652a6`) en la paleta: los estados
son turquesa, ámbar, verde o rojo, y nada más.

### Radios

`--radius-xs: 8px`, `--radius-sm: 12px`, `--radius-md: 18px`,
`--radius-lg: 26px`, `--radius-xl: 32px`. Las pastillas usan `99px` o
`999px`. No hay otros valores.

### Sombras (escala Figma)

- `--shadow-sm: 0 8px 11px rgba(0,0,0,0.25)` — botones y tarjetas en reposo.
- `--shadow-md: 0 12px 20px rgba(0,0,0,0.30)` — hover y paneles.
- `--shadow-lg: 0 24px 48px rgba(0,0,0,0.28)` — tarjeta del login y modales.

La sombra va en cada botón y tarjeta, nunca en todo el bloque o sección.

### Tipografía

- Titulares: Poppins 800, `clamp(2rem, 4vw, 3.3rem)`, `letter-spacing:
  -0.045em`, color `var(--hero-title-dark)`. Una sola escala para todos
  los `h1` de página, incluido el hero y el login. Las media queries
  ajustan el tamaño en móvil, no la escala.
- Cuerpo: Montserrat. Texto secundario mínimo `0.75rem` (12 px).
- Ancho de página: `--container-width: 1320px`.

## Los 3 ejemplos

### 1. Inicio de sesión (la referencia)

Tarjeta partida: a un lado la foto (`/img/mascotas-hero-v2.png`) en un
panel con radio `--radius-lg`, sombra `--shadow-md` y círculos decorativos
turquesa; al otro, titular enorme con la palabra clave en
`--hero-title-light`, inputs con radio `--radius-sm` y anillo de foco
turquesa, y botón primario `--primary-strong` con texto blanco, radio
`--radius-sm` y sombra `--shadow-sm`. Mucho aire entre los elementos.

### 2. Tarjeta de mascota (`PetCard.vue`)

Foto real de la mascota (viene en sus datos, nunca por el nombre),
radio `--radius-md`, sombra `--shadow-sm` que sube a `--shadow-md` al
pasar el cursor. El corazón de favorito solo se muestra a adoptantes o
sin sesión: las acciones se ocultan según el rol, no se explican.

### 3. Botón primario

Altura mínima 48 px, radio `--radius-sm`, fondo `--primary-strong`,
texto blanco, sombra `--shadow-sm`; al hover sube y pasa a
`--shadow-md`. La clase es `.primary-button` (y `.button-primary` en
vistas con el sistema de botones). No hay más sistemas de botón primario.

## Reglas permanentes

1. **Sin fondos azulados.** El fondo es `#F3FEFF` o blanco.
2. **Sin acciones fuera de rol.** Si una acción no le toca a ese rol, se
   oculta (`v-if`), no se muestra con un mensaje que diga que no puede.
3. **Fotos por dato, no por nombre.** La foto de una mascota viene en
   sus datos (`images`); nunca se asigna una foto por el nombre en CSS.
4. **Ritmo oscuro/claro.** Cada vista principal tiene al menos una banda
   oscura: el componente `DarkBand.vue` (`--primary-dark`, texto blanco,
   botón blanco con texto `--primary-strong`) al final del contenido.
5. **Imágenes reales.** Fotos de mascotas y fotos reales del proyecto;
   nada que parezca generado.
6. **Contraste.** El texto interactivo usa `--primary-strong`, no
   `--primary`; los bordes de formulario usan `--border-strong`.

## Al añadir una vista nueva

1. Copiar la estructura del login: titular con palabra en turquesa,
   foto o banda visual con radio `--radius-lg` y sombra `--shadow-md`,
   aire generoso.
2. Usar solo tokens de esta guía; si falta un color, añadir el token en
   `:root` y documentarlo aquí, no escribir el valor suelto.
3. Titular con la escala única; texto secundario de `0.75rem` o más.
4. Añadir una `DarkBand` al final del contenido.
5. Ocultar las acciones por rol con `v-if`.
6. Añadir un test en `tests/app-coherence.test.js` que afirme lo anterior
   y correr `npm test` y `npm run build`.
