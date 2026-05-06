# Plan de Implementación — CalendarioMédico

## Estado general

| Fase | Descripción | Estado |
|------|-------------|--------|
| 0 | Configuración base del proyecto | ✅ Completa |
| 1 | Capa de datos (Room + DataStore) | ⬜ Pendiente |
| 2 | Capa de dominio (Use Cases) | ⬜ Pendiente |
| 3 | Navegación y shell de UI | ⬜ Pendiente |
| 4 | Pantalla Hoy — MVP funcional | ⬜ Pendiente |
| 5 | Modo Cuidador y gestión de medicamentos | ⬜ Pendiente |
| 6 | Calendario, Historial y Detalle | ⬜ Pendiente |
| 7 | Perfil del paciente | ⬜ Pendiente |
| 8 | Notificaciones | ⬜ Pendiente |
| 9 | Ajustes | ⬜ Pendiente |
| 10 | Medicamentos opcionales (AS_NEEDED) | ⬜ Pendiente |
| 11 | Pulido y casos borde | ⬜ Pendiente |

---

## Fase 0 — Configuración base del proyecto ✅

> Dejar el proyecto listo para construir sobre él. Sin esta fase nada compila.

- [ ] **0.1** Actualizar `gradle/libs.versions.toml` con versiones y librerías nuevas
  - KSP `2.2.10-2.0.2` (versión estable confirmada para Kotlin 2.2.10)
  - Hilt + hilt-navigation-compose + hilt-work
  - Room (runtime, ktx, compiler)
  - Navigation Compose
  - DataStore Preferences
  - WorkManager
  - Security Crypto (EncryptedSharedPreferences)
  - lifecycle-viewmodel-compose
  - kotlinx-serialization-json
- [ ] **0.2** Actualizar `build.gradle.kts` raíz — agregar plugins nuevos con `apply false`
- [ ] **0.3** Actualizar `app/build.gradle.kts`
  - Agregar plugins: kotlin-android, ksp, hilt-android, kotlin-serialization
  - Agregar todas las dependencias nuevas
  - Reemplazar bloque `compileOptions` por `kotlin { jvmToolchain(17) }` (cubre tanto Java como Kotlin JVM target en un solo lugar; forma idiomática en Kotlin 2.x)
- [ ] **0.3b** Actualizar `gradle.properties`
  - Descomentar `org.gradle.parallel=true`
  - Agregar `org.gradle.caching=true`
- [ ] **0.4** Actualizar `AndroidManifest.xml` con permisos
  - `POST_NOTIFICATIONS`
  - `SCHEDULE_EXACT_ALARM`
  - `RECEIVE_BOOT_COMPLETED`
- [ ] **0.5** Crear `CalendarioMedicoApp.kt` con `@HiltAndroidApp` y registrarla en el Manifest
- [ ] **0.6** Actualizar tema y colores
  - Reemplazar paleta por defecto (púrpuras) con teal `#1F8A8A`
  - Ajustar `Color.kt` y `Theme.kt` con esquemas claro y oscuro
- [ ] **0.7** Crear estructura de carpetas vacía
  - `core/notification/`, `core/date/`
  - `data/local/dao/`, `data/local/entity/`
  - `data/repository/`, `data/preferences/`
  - `domain/model/`, `domain/usecase/`
  - `presentation/navigation/`, `presentation/today/`
  - `presentation/calendar/`, `presentation/history/`
  - `presentation/treatment/`, `presentation/medication/`
  - `presentation/profile/`, `presentation/caregiver/`
  - `presentation/settings/`
  - `di/`, `security/`

---

## Fase 1 — Capa de datos (Room + DataStore) ⬜

> La app puede persistir y leer datos. Sin lógica de UI aún.

- [ ] **1.1** Modelos de dominio en `domain/model/`
  - `TimeSlot.kt` — enum: `MORNING`, `NOON`, `NIGHT`, `AS_NEEDED`
  - `IntakeStatus.kt` — enum: `PENDING`, `TAKEN`, `SKIPPED`, `MISSED`, `OPTIONAL`
  - `Treatment.kt`
  - `Medication.kt`
  - `MedicationSchedule.kt`
  - `MedicationIntake.kt`
- [ ] **1.2** Entidades Room en `data/local/entity/`
  - `TreatmentEntity.kt`
  - `MedicationEntity.kt`
  - `MedicationScheduleEntity.kt` (con `dayOfWeek` y `doseOverride`)
  - `MedicationIntakeEntity.kt` (con `status`, `confirmedAt`, `notes`)
  - Cada entidad con `fun toDomain()` y `companion fun fromDomain()`
- [ ] **1.3** DAOs en `data/local/dao/`
  - `TreatmentDao.kt` — CRUD + Flow de tratamiento activo
  - `MedicationDao.kt` — CRUD + query por treatmentId
  - `ScheduleDao.kt` — query por medicationId + dayOfWeek
  - `IntakeDao.kt` — queries por fecha, rango, estado; update de status
- [ ] **1.4** `AppDatabase.kt` — Room Database con las 4 entidades, versión 1, TypeConverters para `LocalDate`, `LocalTime`, `LocalDateTime` y enums; usar `fallbackToDestructiveMigration()` durante desarrollo
- [ ] **1.5** Repositorios en `data/repository/`
  - `TreatmentRepository.kt`
  - `MedicationRepository.kt`
  - `IntakeRepository.kt`
- [ ] **1.6** `ReminderPreferences.kt` — DataStore con `morningTime`, `noonTime`, `nightTime`, `pendingAlertDelayMinutes`, `notificationsEnabled`
- [ ] **1.7** `AppModule.kt` en `di/` — proveer `AppDatabase`, repositorios y `ReminderPreferences` con Hilt
- [ ] **1.8** `DateUtils.kt` en `core/date/` — centralizar formateo de fechas con `Locale("es", "MX")` para que toda la app muestre fechas en español independientemente del locale del dispositivo

---

## Fase 2 — Capa de dominio (Use Cases) ⬜

> Encapsular la lógica de negocio antes de tocar UI.

- [ ] **2.1** `GenerateDailyIntakesUseCase` — genera registros de `MedicationIntake` para una fecha; aplica rango del tratamiento, días de semana, doseOverride; idempotente
- [ ] **2.2** `GetTodayIntakesUseCase` — asegura tomas del día y retorna Flow agrupado por `TimeSlot`
- [ ] **2.3** `MarkIntakeAsTakenUseCase` — status → `TAKEN`, guarda `confirmedAt`
- [ ] **2.4** `SkipIntakeUseCase` — status → `SKIPPED` con nota opcional
- [ ] **2.5** `CheckPendingDosesUseCase` — tomas `PENDING` cuyo horario pasó hace más de `pendingAlertDelayMinutes`
- [ ] **2.6** `RescheduleRemindersUseCase` — cancela y reprograma todas las alarmas con AlarmManager según horarios configurados

---

## Fase 3 — Navegación y shell de UI ⬜

> La app navega entre pantallas aunque estén vacías.

- [ ] **3.1** Destinos de navegación — `@Serializable` data objects para: `Today`, `Calendar`, `History`, `TreatmentList`, `TreatmentForm(id?)`, `MedicationList(treatmentId)`, `MedicationForm(treatmentId, medicationId?)`, `MedicationDetail(medicationId)`, `Profile`, `PinLock(mode)`, `CaregiverHub`, `Settings`
- [ ] **3.2** `AppNavGraph.kt` — `NavHost` con todos los destinos registrados, pantalla inicial `Today`
- [ ] **3.3** `TabBar` — bottom navigation con 3 pestañas: Hoy, Calendario, Perfil con íconos Material3
- [ ] **3.4** `MainActivity.kt` — reemplazar placeholder, configurar `AppNavGraph` dentro del tema, anotar con `@AndroidEntryPoint`

---

## Fase 4 — Pantalla Hoy — MVP funcional ⬜

> El usuario puede ver y marcar sus medicamentos del día. Corazón de la app.

- [ ] **4.1** `TodayUiState.kt` — data class con fecha, nombre del tratamiento, tomas agrupadas por `TimeSlot`, contadores, loading/empty/error
- [ ] **4.2** `TodayViewModel.kt`
  - Observa `GetTodayIntakesUseCase` con Flow
  - Expone `StateFlow<TodayUiState>`
  - Acción `markAsTaken(intakeId)`
  - Acción `skipIntake(intakeId, note)`
- [ ] **4.3** `TodayScreen.kt`
  - Header con fecha y saludo con nombre del paciente
  - Barra de progreso del día
  - Sección por cada `TimeSlot` con tarjetas de medicamento
  - Tarjeta: nombre, dosis, instrucción, color, botón "Tomar"
  - Estado vacío si no hay tratamiento activo
  - Estado "todo completado" al final del día

---

## Fase 5 — Modo Cuidador y gestión de medicamentos ⬜

> El cuidador puede crear y editar el tratamiento. El paciente no puede.

- [ ] **5.1** `PinManager.kt` en `security/` — `EncryptedSharedPreferences`; métodos: `hasPin()`, `setPin(raw)`, `verifyPin(raw): Boolean`; PIN guardado como hash SHA-256
- [ ] **5.2** `CaregiverViewModel.kt` — estado `pinMode` (`NONE`, `SET`, `CONFIRM`, `ENTER`), `isUnlocked`, `error`; acciones `startSetPin()`, `submitPin(digits)`, `lock()`; resetea al ir a background
- [ ] **5.3** `PinLockScreen.kt` — teclado numérico 4 dígitos, dots de progreso, modos crear/confirmar/ingresar, error animado, botón cancelar
- [ ] **5.4** `CaregiverHubScreen.kt` — tarjetas de acceso: Historial, Mis Medicamentos, Agregar Medicamento; botón cerrar sesión
- [ ] **5.5** `TreatmentFormScreen.kt` + `TreatmentViewModel.kt` — campos: nombre, descripción, DatePicker inicio/fin, estado activo; validación fecha fin > inicio
- [ ] **5.6** `MedicationFormScreen.kt` + `MedicationViewModel.kt` — campos: nombre, dosis, instrucciones, selector `TimeSlot`, chips de días, `doseOverride` por día, toggle obligatorio/opcional, selector de color
- [ ] **5.7** `MedicationListScreen.kt` — lista de medicamentos del tratamiento activo, FAB agregar, swipe-to-delete con confirmación

---

## Fase 6 — Calendario, Historial y Detalle ⬜

- [ ] **6.1** `CalendarScreen.kt` + `CalendarViewModel.kt`
  - Vista semanal centrada en hoy
  - Puntos de color por medicamento según estado
  - Al tocar día: expande lista de tomas con estado
  - Navegación semana anterior / siguiente
- [ ] **6.2** `HistoryScreen.kt` + `HistoryViewModel.kt`
  - DatePicker selector de fecha
  - Lista de tomas con estado, hora de confirmación y nota
  - Porcentaje de cumplimiento del día (solo obligatorios)
- [ ] **6.3** `MedicationDetailScreen.kt`
  - Información completa del medicamento
  - Estado del día actual
  - Grid de últimos 7 días (tomado / vencido / no aplica)

---

## Fase 7 — Perfil del paciente ⬜

- [ ] **7.1** `ProfileScreen.kt` + `ProfileViewModel.kt`
  - Nombre del paciente
  - Tratamiento activo: nombre y fechas
  - Resumen de cumplimiento últimos 7 días
  - Botón "Modo Cuidador" que dispara flujo de PIN

---

## Fase 8 — Notificaciones ⬜

> La app recuerda al usuario aunque esté cerrada.

- [ ] **8.1** `NotificationHelper.kt` — crear canal `MEDICATION_REMINDERS` al iniciar app; métodos para construir y lanzar notificaciones agrupadas con acciones (Tomado, Recordar en 10 min)
- [ ] **8.2** `ReminderAlarmReceiver.kt` — `BroadcastReceiver` que recibe alarma, consulta tomas del horario y lanza notificación; declarado en Manifest
- [ ] **8.3** `BootReceiver.kt` — `BroadcastReceiver` para `BOOT_COMPLETED`; llama a `RescheduleRemindersUseCase`; declarado en Manifest
- [ ] **8.4** `PendingDoseWorker.kt` — `CoroutineWorker` que corre ~30 min después del horario; llama a `CheckPendingDosesUseCase` y envía alerta de pendiente si aplica
- [ ] **8.5** Integración en ViewModels — al marcar tomado: cancelar Worker de pendiente; al crear/editar tratamiento: llamar `RescheduleRemindersUseCase`
- [ ] **8.6** Permiso `POST_NOTIFICATIONS` (Android 13+) — solicitar en `MainActivity` al primer lanzamiento
- [ ] **8.7** Deep links desde notificaciones — configurar `NavDeepLink` en `AppNavGraph` para que las acciones de notificación (Tomado, Ver detalle) abran la app y naveguen a la pantalla correcta sin pasar por `MainActivity` desde cero; registrar el `Intent` en `AndroidManifest.xml`

---

## Fase 9 — Ajustes ⬜

- [ ] **9.1** `SettingsScreen.kt` + `SettingsViewModel.kt`
  - Time pickers para mañana, comida y noche (actualiza DataStore + reprograma alarmas)
  - Toggle de notificaciones
  - Campo para minutos de alerta de pendiente
  - Opción "Cambiar PIN" (lanza PinLock en modo SET)
  - Campo para nombre del paciente

---

## Fase 10 — Medicamentos opcionales (AS_NEEDED) ⬜

- [ ] **10.1** Sección "Si lo necesitas" en `TodayScreen` — colapsable, botón "Registrar toma", sheet con hora y nota, no genera pendientes, no cuenta en porcentaje
- [ ] **10.2** Advertencia de intervalo mínimo — si ya se tomó hace menos de `minIntervalHours`, mostrar aviso antes de confirmar

---

## Fase 11 — Pulido y casos borde ⬜

- [ ] **11.1** Pantalla de bienvenida al primer lanzamiento (sin tratamiento activo)
- [ ] **11.2** Aviso legal de salud en primer uso y en pantalla de ajustes
- [ ] **11.3** Manejo de `SCHEDULE_EXACT_ALARM` denegado — fallback a WorkManager con advertencia al usuario
- [ ] **11.4** Estados de carga en todas las pantallas (Skeleton o CircularProgress)
- [ ] **11.5** Estados vacíos con ilustración y call-to-action en cada pantalla
- [ ] **11.6** Tratamiento vencido — aviso en pantalla Hoy con opción de crear nuevo
- [ ] **11.7** Eliminar medicamento — advertencia de que borra tomas futuras
- [ ] **11.8** Editar tratamiento — recalcular tomas futuras sin modificar historial pasado
- [ ] **11.9** Verificar que la app compila y corre limpia en minSdk 30 (Android 11)
