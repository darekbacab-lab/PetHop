# Modelo de datos

PostgreSQL usa UUID, nombres `snake_case`, restricciones explícitas, timestamps UTC y migraciones Flyway. Hibernate corre con `ddl-auto: validate`; la aplicación no genera ni modifica el esquema por su cuenta.

| Migración | Tablas y relación |
|---|---|
| V1 | `users`; `shelter_profiles` (1:1 con usuario refugio). |
| V2 | `pets` (pertenece a refugio) y `pet_images` (archivos fuera de PostgreSQL; máximo 6 por mascota en servicio). |
| V3 | `pet_health_records` (historial sanitario inmutable). |
| V4 | `adoption_applications`, `application_answers` y `application_status_history`; índice parcial evita solicitudes activas duplicadas por adoptante/mascota. |
| V5 | `adoptions` (una por solicitud y una por mascota) y `follow_up_reports`/`follow_up_media`. |
| V6 | `favorites`, único por usuario/mascota. |
| V7 | `conversations`, `conversation_participants` y `messages`; conversación puede asociarse a una solicitud o a una mascota. |
| V8 | `notifications`, asociadas a usuario y referencia opcional. |

Los archivos no se guardan como BLOB. `FileStorage` permite sustituir el almacenamiento local por un proveedor de objetos. Los objetos usan identificadores aleatorios, MIME real comprobado por firma, lista permitida JPEG/PNG/WebP y límite de 10 MiB.

No se creó `ai_triage_sessions`: el asistente actual responde con contenido local predefinido y no implementa triaje clínico ni proveedor de IA. Guardar eso como una sesión clínica real daría una impresión incorrecta; la decisión está detallada en la auditoría.
