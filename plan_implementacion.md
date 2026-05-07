# Plan de Implementación — CalendarioMédico

## Estado general

| Fase | Descripción | Estado |
|------|-------------|--------|
| 0 | Configuración base del proyecto | ✅ Completa |
| 1 | Capa de datos (Room + DataStore) | ✅ Completa |
| 2 | Capa de dominio (Use Cases) | ✅ Completa |
| 3 | Navegación y shell de UI | ✅ Completa |
| 4 | Pantalla Hoy — MVP funcional | ✅ Completa |
| 5 | Modo Cuidador y gestión de medicamentos | ✅ Completa |
| 6 | Calendario, Historial y Detalle | ✅ Completa |
| 7 | Perfil del paciente | ✅ Completa |
| 8 | Notificaciones | ✅ Completa |
| 9 | Ajustes | ✅ Completa |
| 10 | Medicamentos opcionales (AS_NEEDED) | ✅ Completa |
| 11 | Pulido y casos borde | ✅ Completa |

---

## Fase 0 — Configuración base del proyecto ✅

> Dejar el proyecto listo para construir sobre él. Sin esta fase nada compila.

- [ ] **0.1** Actualizar `gradle/libs.versions.toml` con versiones y librerías nuevas
  - KSP `2.2.10-2.0.2` (versión estable confirmada para Kotlin 2.2.10)
  - Hilt `2.59.2` (mínimo requerido para AGP 9.x — versiones anteriores usan BaseExtension removida)
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

## Fase 1 — Capa de datos (Room + DataStore) ✅

> La app puede persistir y leer datos. Sin lógica de UI aún.

- [x] **1.1** Modelos de dominio en `domain/model/`
  - `TimeSlot.kt` — enum: `MORNING`, `NOON`, `NIGHT`, `AS_NEEDED`
  - `IntakeStatus.kt` — enum: `PENDING`, `TAKEN`, `SKIPPED`, `MISSED`, `OPTIONAL`
  - `Treatment.kt`
  - `Medication.kt`
  - `MedicationSchedule.kt`
  - `MedicationIntake.kt`
- [x] **1.2** Entidades Room en `data/local/entity/`
  - `TreatmentEntity.kt`
  - `MedicationEntity.kt`
  - `MedicationScheduleEntity.kt` (con `dayOfWeek` y `doseOverride`)
  - `MedicationIntakeEntity.kt` (con `status`, `confirmedAt`, `notes`)
  - Cada entidad con `fun toDomain()` y `companion fun fromDomain()`
- [x] **1.3** DAOs en `data/local/dao/`
  - `TreatmentDao.kt` — CRUD + Flow de tratamiento activo
  - `MedicationDao.kt` — CRUD + query por treatmentId
  - `ScheduleDao.kt` — query por medicationId + dayOfWeek
  - `IntakeDao.kt` — queries por fecha, rango, estado; update de status
- [x] **1.4** `AppDatabase.kt` — Room Database con las 4 entidades, versión 1, TypeConverters para `LocalDate`, `LocalTime`, `LocalDateTime` y enums; usar `fallbackToDestructiveMigration()` durante desarrollo
- [x] **1.5** Repositorios en `data/repository/`
  - `TreatmentRepository.kt`
  - `MedicationRepository.kt`
  - `IntakeRepository.kt`
- [x] **1.6** `ReminderPreferences.kt` — DataStore con `morningTime`, `noonTime`, `nightTime`, `pendingAlertDelayMinutes`, `notificationsEnabled`
- [x] **1.7** `AppModule.kt` en `di/` — proveer `AppDatabase`, repositorios y `ReminderPreferences` con Hilt
- [x] **1.8** `DateUtils.kt` en `core/date/` — centralizar formateo de fechas con `Locale("es", "MX")` para que toda la app muestre fechas en español independientemente del locale del dispositivo

---

## Fase 2 — Capa de dominio (Use Cases) ✅

> Encapsular la lógica de negocio antes de tocar UI.

- [x] **2.1** `GenerateDailyIntakesUseCase` — genera registros de `MedicationIntake` para una fecha; aplica rango del tratamiento, días de semana, doseOverride; idempotente
- [x] **2.2** `GetTodayIntakesUseCase` — asegura tomas del día y retorna Flow agrupado por `TimeSlot`
- [x] **2.3** `MarkIntakeAsTakenUseCase` — status → `TAKEN`, guarda `confirmedAt`
- [x] **2.4** `SkipIntakeUseCase` — status → `SKIPPED` con nota opcional
- [x] **2.5** `CheckPendingDosesUseCase` — tomas `PENDING` cuyo horario pasó hace más de `pendingAlertDelayMinutes`
- [x] **2.6** `RescheduleRemindersUseCase` — cancela y reprograma todas las alarmas con AlarmManager según horarios configurados
  - `AlarmScheduler` interface en `core/notification/`; `NoOpAlarmScheduler` como stub; `NotificationModule` con `@Binds` — se sustituye la implementación real en Fase 8

---

## Fase 3 — Navegación y shell de UI ✅

> La app navega entre pantallas aunque estén vacías.

- [x] **3.1** Destinos de navegación — `@Serializable` data objects/classes en `AppNavGraph.kt`: `Today`, `Calendar`, `History`, `TreatmentList`, `TreatmentForm(id?)`, `MedicationList(treatmentId)`, `MedicationForm(treatmentId, medicationId?)`, `MedicationDetail(medicationId)`, `Profile`, `PinLock(mode)`, `CaregiverHub`, `Settings`
- [x] **3.2** `AppNavGraph.kt` — `NavHost` con todos los destinos registrados, pantalla inicial `Today`; bottom bar oculta en pantallas secundarias via `hasRoute`
- [x] **3.3** `AppBottomBar` — 3 pestañas: Hoy, Calendario, Perfil con íconos Material3; `launchSingleTop + restoreState`
- [x] **3.4** `MainActivity.kt` — reemplazado placeholder, `AppNavGraph` dentro del tema, `@AndroidEntryPoint`
- [x] Stubs de todas las pantallas en sus respectivos paquetes `presentation/`

---

## Fase 4 — Pantalla Hoy — MVP funcional ✅

> El usuario puede ver y marcar sus medicamentos del día. Corazón de la app.

- [x] **4.1** `TodayUiState.kt` — fecha, treatmentName, intakesBySlot, totalRequired/Taken, progressFraction, isAllDone
- [x] **4.2** `TodayViewModel.kt` — `@HiltViewModel`; combina Flow de tratamiento activo + `GetTodayIntakesUseCase`; acciones `markAsTaken` y `skipIntake`
- [x] **4.3** `TodayScreen.kt`
  - Header card con fecha, tratamiento, LinearProgressIndicator y conteo "X de Y tomados"
  - Secciones por TimeSlot (Mañana/Comida/Noche) con barra de acento de color
  - `IntakeCard` con borde de color según estado, nombre, dosis, botón "Tomar ahora" + "Omitir"
  - `IntakeStatusBadge`: ✓ con hora (TAKEN), "Omitida" (SKIPPED), "No tomada" (MISSED)
  - Dialog de confirmación para omitir
  - `AllDoneCard` — mensaje "¡Todo al día!" cuando no quedan PENDING
  - `EmptyTreatmentState` — cuando no hay tratamiento activo
- [x] Dependencia `lifecycle-runtime-compose` añadida para `collectAsStateWithLifecycle`

---

## Fase 5 — Modo Cuidador y gestión de medicamentos ✅

> El cuidador puede crear y editar el tratamiento. El paciente no puede.

- [x] **5.1** `PinManager.kt` — EncryptedSharedPreferences; `hasPin/setPin/verifyPin/clearPin`; hash SHA-256
- [x] **5.2** `CaregiverViewModel.kt` — `PinMode(SET/CONFIRM/ENTER)`, buffer de dígitos, `onDigit/onDelete/processPin`; navega a CaregiverHub en `isSuccess=true`
- [x] **5.3** `PinLockScreen.kt` — teclado 4×3 con círculos OutlinedButton, 4 dots con color de error, LaunchedEffect para init y navegación
- [x] **5.4** `CaregiverHubScreen.kt` + `CaregiverHubViewModel` — tarjetas clicables: Tratamientos, Medicamentos, Agregar, Historial, Ajustes; muestra tratamiento activo; botón "Cerrar sesión"
- [x] **5.5** `TreatmentViewModel.kt` + `TreatmentFormScreen.kt` — nombre, descripción, DatePickerDialog inicio/fin, switch activo; `TreatmentListScreen` con FAB y confirmación de borrado
- [x] **5.6** `MedicationViewModel.kt` + `MedicationFormScreen.kt` — SegmentedButton para TimeSlot, FilterChips días, doseOverride por día seleccionado, switch obligatorio, selector de color con 8 colores preset
- [x] **5.7** `MedicationListScreen.kt` — lista con borde de color, FAB agregar, confirmación de borrado
- [x] `ProfileScreen.kt` actualizado con botón "Modo Cuidador" → `PinLock("enter")`

---

## Fase 6 — Calendario, Historial y Detalle ✅

- [x] **6.1** `CalendarScreen.kt` + `CalendarViewModel.kt`
  - Vista semanal centrada en hoy
  - Puntos de color por medicamento según estado
  - Al tocar día: expande lista de tomas con estado
  - Navegación semana anterior / siguiente
- [x] **6.2** `HistoryScreen.kt` + `HistoryViewModel.kt`
  - DatePicker selector de fecha
  - Lista de tomas con estado, hora de confirmación y nota
  - Porcentaje de cumplimiento del día (solo obligatorios)
- [x] **6.3** `MedicationDetailScreen.kt` + `MedicationDetailViewModel.kt`
  - Información completa del medicamento
  - Estado del día actual
  - Grid de últimos 7 días (tomado / vencido / no aplica)

---

## Fase 7 — Perfil del paciente ✅

- [x] **7.1** `ProfileScreen.kt` + `ProfileViewModel.kt`
  - Nombre del paciente (editable, persiste en DataStore)
  - Tratamiento activo: nombre y fechas
  - Resumen de cumplimiento últimos 7 días
  - Botón "Modo Cuidador" que dispara flujo de PIN

---

## Fase 8 — Notificaciones ✅

> La app recuerda al usuario aunque esté cerrada.

- [x] **8.1** `NotificationHelper.kt` — canal `medication_reminders`; notificaciones con acciones "Tomado ✓" y "En 10 min ⏰"; alerta de pendientes
- [x] **8.2** `ReminderAlarmReceiver.kt` + `ReminderAlarmScheduler.kt` — `@AndroidEntryPoint`; consulta intakes del slot; reagenda siguiente día; encola `PendingDoseWorker`
- [x] **8.3** `BootReceiver.kt` — `BOOT_COMPLETED`; llama `RescheduleRemindersUseCase` con `goAsync()`
- [x] **8.4** `PendingDoseWorker.kt` — `@HiltWorker`; llama `CheckPendingDosesUseCase`; muestra alerta si hay pendientes
- [x] **8.5** Integración — `TreatmentViewModel.save()` llama `RescheduleRemindersUseCase`; `NotificationActionReceiver` maneja "Tomado" y "Snooze"
- [x] **8.6** `POST_NOTIFICATIONS` solicitado en `MainActivity` (Android 13+)
- [x] **8.7** Deep link `calendariomedico://today` en `AppNavGraph` + intent-filter en `AndroidManifest.xml`; `HiltWorkerFactory` configurado en `CalendarioMedicoApp`

---

## Fase 9 — Ajustes ✅

- [x] **9.1** `SettingsScreen.kt` + `SettingsViewModel.kt`
  - Time pickers para mañana, comida y noche (actualiza DataStore + reprograma alarmas)
  - Toggle de notificaciones
  - Campo para minutos de alerta de pendiente (1–120 min, validado)
  - Opción "Cambiar PIN" (lanza PinLock en modo SET)
  - Campo para nombre del paciente

---

## Fase 10 — Medicamentos opcionales (AS_NEEDED) ✅

- [x] **10.1** Sección "Si lo necesitas" en `TodayScreen` — colapsable con animación de chevron, `AsNeededCard` visualmente distinta, `AlertDialog` con campo de nota opcional; AS_NEEDED separado del flujo regular y del `isAllDone`
- [x] **10.2** Advertencia de intervalo mínimo — consulta `minIntervalHours` del medicamento y `getLastTaken` del repositorio; muestra aviso en rojo en el dialog; botón cambia a "Registrar igualmente"

---

## Fase 11 — Pulido y casos borde ✅

- [x] **11.1** Bienvenida en primer lanzamiento — `OnboardingState` en `TodayScreen` cuando `isFirstLaunch && !hasActiveTreatment`; flag `hasCompletedOnboarding` en DataStore; botón "Configurar tratamiento" → `PinLock("set")`
- [x] **11.2** Aviso legal — mostrado en `OnboardingState` (primer uso) y en sección "Acerca de" de `SettingsScreen`
- [x] **11.3** `SCHEDULE_EXACT_ALARM` denegado — `SettingsScreen` muestra banner de error con botón "Abrir ajustes del sistema" cuando `canScheduleExactAlarms()` retorna false (guarda con `>= S`)
- [x] **11.4** Estados de carga — `CalendarScreen` y `ProfileScreen` muestran `CircularProgressIndicator` mientras `isLoading = true`
- [x] **11.5** Estados vacíos — `HistoryScreen`, `CalendarScreen`, `TreatmentListScreen`, `MedicationListScreen` tienen texto vacío; `EmptyTreatmentState` en `TodayScreen`
- [x] **11.6** Tratamiento vencido — `TodayUiState.isTreatmentExpired` + `ExpiredTreatmentBanner` en `TodayScreen` con botón "Crear nuevo tratamiento" → modo cuidador
- [x] **11.7** Eliminar medicamento — dialog actualizado: "Se borrarán todas sus tomas, incluido el historial pasado" (FK cascade ya existía)
- [x] **11.8** Editar tratamiento — `TreatmentViewModel.save()` llama `intakeRepository.deleteFuturePending(treatmentId, today)` antes de reprogramar; nueva query `@Query DELETE ... status IN ('PENDING','OPTIONAL')` en `IntakeDao`
- [x] **11.9** minSdk 30 verificado — `canScheduleExactAlarms()` guarda con `>= S`, `POST_NOTIFICATIONS` con `>= TIRAMISU`, todas las demás APIs disponibles desde API 23
