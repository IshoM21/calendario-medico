# CalendarioMédico — Contexto del proyecto

## Qué es

App Android nativa para seguimiento de medicamentos. Permite configurar tratamientos, medicamentos y horarios; envía notificaciones diarias; registra tomas y muestra historial de cumplimiento. Diseñada para adultos mayores con un modo cuidador protegido por PIN.

El prototipo visual de referencia está en `CalendarioMedico Standalone.html` (React, abre en navegador).
Los requerimientos completos están en `requerimientos_app_medicamentos_android.md`.
El plan de implementación por fases está en `plan_implementacion.md`.

## Stack

- **Lenguaje:** Kotlin 2.2.10
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM + Repository + Use Cases
- **DI:** Hilt (con KSP, no kapt)
- **Base de datos:** Room
- **Preferencias:** DataStore
- **Notificaciones:** AlarmManager (exactas) + WorkManager (pendientes)
- **Seguridad:** EncryptedSharedPreferences (PIN del cuidador)
- **Navegación:** Navigation Compose con rutas type-safe (@Serializable)

## Versiones clave

```toml
agp = "9.2.1"
kotlin = "2.2.10"
gradle-wrapper = "9.4.1"
compileSdk = 36          # AGP 9 syntax: version = release(36) { minorApiLevel = 1 }
minSdk = 30              # Android 11+
targetSdk = 36
jvmToolchain = 17
```

> `compileSdk` usa la nueva sintaxis de AGP 9 para Android 16. No usar `compileSdk = 36` plano.
> AGP 9.2.1 registra la extensión `kotlin` internamente. No aplicar `kotlin-android` en el módulo app — causa conflicto "extension already registered". El plugin correcto es solo `kotlin-compose` (junto con `ksp` y `hilt-android`).

## Package y nombres

```
applicationId:  com.codigomoo.calendariomedico
package:        com.codigomoo.calendariomedico
App class:      CalendarioMedicoApp   (no MedTrackerApp)
MainActivity:   MainActivity (@AndroidEntryPoint)
```

## Estructura de carpetas

```
app/src/main/java/com/codigomoo/calendariomedico/
├── core/
│   ├── date/          DateUtils (Locale es-MX)
│   └── notification/  NotificationHelper, ReminderAlarmReceiver, PendingDoseWorker
├── data/
│   ├── local/
│   │   ├── dao/       TreatmentDao, MedicationDao, ScheduleDao, IntakeDao
│   │   └── entity/    *Entity.kt (con toDomain() / fromDomain())
│   ├── preferences/   ReminderPreferences (DataStore)
│   └── repository/    TreatmentRepository, MedicationRepository, IntakeRepository
├── di/                AppModule.kt
├── domain/
│   ├── model/         Treatment, Medication, MedicationSchedule, MedicationIntake,
│   │                  TimeSlot, IntakeStatus
│   └── usecase/       GenerateDailyIntakesUseCase, GetTodayIntakesUseCase,
│                      MarkIntakeAsTakenUseCase, SkipIntakeUseCase,
│                      CheckPendingDosesUseCase, RescheduleRemindersUseCase
├── presentation/
│   ├── calendar/      CalendarScreen, CalendarViewModel
│   ├── caregiver/     PinLockScreen, CaregiverHubScreen, CaregiverViewModel
│   ├── history/       HistoryScreen, HistoryViewModel
│   ├── medication/    MedicationListScreen, MedicationDetailScreen,
│   │                  MedicationFormScreen, MedicationViewModel
│   ├── navigation/    AppNavGraph (rutas @Serializable)
│   ├── profile/       ProfileScreen, ProfileViewModel
│   ├── settings/      SettingsScreen, SettingsViewModel
│   ├── today/         TodayScreen, TodayViewModel, TodayUiState
│   └── treatment/     TreatmentListScreen, TreatmentFormScreen, TreatmentViewModel
├── security/          PinManager (EncryptedSharedPreferences, hash SHA-256)
├── CalendarioMedicoApp.kt
└── MainActivity.kt
```

## Convenciones

- Un archivo por clase. Sin múltiples clases públicas por archivo.
- Entidades Room viven en `data/local/entity/`. Modelos de dominio en `domain/model/`. Nunca mezclar.
- Cada ViewModel expone un único `StateFlow<XxxUiState>`.
- Rutas de navegación: `@Serializable` data objects/classes en `AppNavGraph.kt`.
- Fechas siempre con `java.time.*`. Formateo siempre vía `DateUtils` con `Locale("es", "MX")`.
- Strings en español directamente en código para MVP. No se usa `strings.xml` para textos de UI por ahora.
- Room usa `fallbackToDestructiveMigration()` durante desarrollo (fase 1-10). Remover antes de release.

## Permisos Android

```xml
POST_NOTIFICATIONS       <!-- Android 13+ -->
SCHEDULE_EXACT_ALARM
RECEIVE_BOOT_COMPLETED
```

## Colores principales

```kotlin
Primary:    #1F8A8A   (teal)
Background: #E8EEF0
Surface:    #F4F7F8
OnPrimary:  #FFFFFF
Text:       #0F2B33
TextSub:    #42606A
```

## Módulos de la app (resumen)

| Módulo | Descripción |
|--------|-------------|
| Tratamientos | Crear, editar, activar, archivar tratamientos con fechas |
| Medicamentos | CRUD con días específicos, doseOverride por día, obligatorio/opcional |
| Calendario de tomas | Generación automática, estados PENDING/TAKEN/SKIPPED/MISSED/OPTIONAL |
| Pantalla Hoy | Vista principal agrupada por horario (Mañana, Comida, Noche) |
| Notificaciones | AlarmManager por horario + WorkManager para pendientes |
| Historial | Cumplimiento diario con hora de confirmación |
| Modo Cuidador | PIN (SHA-256, EncryptedSharedPreferences); separa vista paciente de administración |
| Medicamentos opcionales | AS_NEEDED: sin pendientes automáticos, sin afectar cumplimiento |

## Tratamiento de referencia (datos de prueba)

```
Tratamiento: Mayo-Junio 2026  (9 may – 5 jun 2026)

1. Prednisona 5 mg        mañana    todos los días    obligatorio
2. Leflunomida 20 mg      comida    todos los días    obligatorio
3. Ácido Fólico           mañana    lun-vie           obligatorio
4. Metotrexato 2.5 mg     noche     sáb (3 tab) y dom (2 tab)   obligatorio
5. Sulindaco 200 mg       —         según necesidad   opcional
```
