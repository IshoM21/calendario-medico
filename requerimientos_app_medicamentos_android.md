# Documento de Requerimientos — App Android Nativa para Seguimiento de Medicamentos

## 1. Contexto

Actualmente se cuenta con un calendario de medicamentos en formato HTML, CSS y JavaScript. Este calendario muestra un tratamiento médico para mayo-junio de 2026, con medicamentos, dosis, días de toma, leyenda visual, instrucciones generales y estilos preparados para impresión.

El objetivo de este documento es transformar esa idea en una **app Android nativa**, no solo como una visualización estática, sino como una herramienta funcional para seguimiento diario de medicamentos.

La app deberá permitir:

- Crear tratamientos personalizados.
- Configurar medicamentos y dosis.
- Definir días específicos de toma.
- Enviar notificaciones en horarios clave.
- Confirmar que el medicamento ya fue tomado.
- Consultar historial diario.
- Detectar dosis pendientes.
- Alertar al usuario cuando una toma no ha sido confirmada.

---

## 2. Objetivo general

Desarrollar una aplicación Android nativa para gestionar tratamientos médicos de forma local, permitiendo configurar medicamentos, horarios, días de toma y seguimiento diario mediante notificaciones, confirmaciones e historial.

---

## 3. Alcance inicial del MVP

El MVP deberá cubrir las siguientes capacidades:

1. Crear tratamiento nuevo.
2. Elegir fecha de inicio y fecha de fin.
3. Configurar medicamentos.
4. Configurar días específicos de toma.
5. Asignar medicamentos a horarios: mañana, comida o noche.
6. Generar automáticamente las tomas diarias según el tratamiento.
7. Enviar notificaciones para cada horario.
8. Marcar medicamento como tomado.
9. Consultar historial diario.
10. Alertar dosis pendientes.
11. Manejar medicamentos opcionales, como aquellos que solo se toman si hay dolor.

---

## 4. Stack tecnológico recomendado

### 4.1 Plataforma

- **Android nativo**
- **Lenguaje:** Kotlin
- **IDE:** Android Studio

### 4.2 UI

- **Jetpack Compose**

Motivo:

- Permite construir interfaces modernas de forma declarativa.
- Facilita crear tarjetas, listas, calendarios y estados visuales.
- Es el estándar moderno recomendado para nuevas apps Android.

### 4.3 Persistencia local

- **Room Database**

Motivo:

- Permite guardar tratamientos, medicamentos, horarios e historial.
- Funciona 100% local.
- Se integra bien con Kotlin, Flow y arquitectura MVVM.

### 4.4 Preferencias de usuario

- **DataStore**

Uso sugerido:

- Guardar horarios por defecto.
- Guardar preferencias de notificaciones.
- Guardar ajustes de tema o configuración general.

### 4.5 Notificaciones y recordatorios

- **AlarmManager** para alarmas exactas.
- **WorkManager** para validaciones de pendientes o tareas diferidas.
- **NotificationCompat** para mostrar notificaciones.

Uso sugerido:

- AlarmManager: disparar recordatorios en horarios concretos.
- WorkManager: revisar si una toma quedó pendiente después de cierto tiempo.

### 4.6 Arquitectura

- **MVVM**
- **Repository Pattern**
- **Use Cases** opcionales si la lógica crece.

Capas sugeridas:

```text
UI → ViewModel → UseCase/Repository → Room DAO → Database
```

### 4.7 Librerías sugeridas

```kotlin
// UI
Jetpack Compose
Material 3
Navigation Compose

// Persistencia
Room
DataStore

// Asincronía
Kotlin Coroutines
Kotlin Flow

// Notificaciones
WorkManager
AlarmManager
NotificationCompat

// Fechas
java.time.LocalDate
java.time.LocalTime
java.time.LocalDateTime

// Seguridad
androidx.security:security-crypto  // EncryptedSharedPreferences para el PIN del cuidador
```

---

## 5. Módulos principales de la app

## 5.1 Módulo de tratamientos

Permite crear y administrar tratamientos médicos.

### Funciones

- Crear tratamiento nuevo.
- Editar tratamiento existente.
- Activar o desactivar tratamiento.
- Elegir fecha de inicio.
- Elegir fecha de fin.
- Consultar detalle del tratamiento.
- Archivar tratamiento finalizado.

### Campos sugeridos

```kotlin
data class Treatment(
    val id: Long,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### Ejemplo

```text
Tratamiento: Mayo-Junio 2026
Inicio: 9 mayo 2026
Fin: 5 junio 2026
Estado: Activo
```

---

## 5.2 Módulo de medicamentos

Permite configurar los medicamentos asociados a un tratamiento.

### Funciones

- Agregar medicamento.
- Editar medicamento.
- Eliminar medicamento.
- Definir dosis general.
- Definir instrucciones.
- Definir horario de toma.
- Configurar días específicos.
- Configurar si es obligatorio u opcional.
- Configurar dosis diferente según el día.

### Campos sugeridos

```kotlin
data class Medication(
    val id: Long,
    val treatmentId: Long,
    val name: String,
    val dose: String,
    val instructions: String?,
    val timeSlot: TimeSlot,
    val isRequired: Boolean,
    val colorHex: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### Enum de horarios

```kotlin
enum class TimeSlot {
    MORNING,
    NOON,
    NIGHT,
    AS_NEEDED
}
```

### Ejemplo

```text
Medicamento: Prednisona 5 mg
Dosis: 1 tableta
Horario: Mañana
Días: Todos los días
Instrucción: Con el desayuno
Obligatorio: Sí
```

---

## 5.3 Módulo de calendario de tomas

Este módulo genera las tomas esperadas según las reglas del tratamiento y medicamento.

### Funciones

- Generar tomas diarias.
- Mostrar medicamentos correspondientes al día actual.
- Mostrar tomas agrupadas por horario.
- Consultar tomas de una fecha específica.
- Mostrar si una toma está pendiente, tomada, omitida o vencida.

### Estados de una toma

```kotlin
enum class IntakeStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    OPTIONAL
}
```

### Modelo sugerido

```kotlin
data class MedicationIntake(
    val id: Long,
    val treatmentId: Long,
    val medicationId: Long,
    val date: LocalDate,
    val scheduledTimeSlot: TimeSlot,
    val scheduledTime: LocalTime?,
    val dose: String,
    val status: IntakeStatus,
    val confirmedAt: LocalDateTime?,
    val notes: String?
)
```

---

## 5.4 Módulo de horarios

La app debe permitir configurar horarios globales para los bloques principales.

### Horarios por defecto sugeridos

```text
Mañana: 8:00 AM
Comida: 2:00 PM
Noche: 9:00 PM
```

### Modelo sugerido

```kotlin
data class ReminderSettings(
    val morningTime: LocalTime,
    val noonTime: LocalTime,
    val nightTime: LocalTime,
    val pendingAlertDelayMinutes: Int
)
```

### Reglas

- Cada medicamento programado debe pertenecer a un bloque horario.
- Los medicamentos opcionales no deben generar alertas pendientes obligatorias.
- El usuario puede cambiar los horarios globales.
- Opcionalmente, un medicamento podría tener un horario personalizado.

---

## 5.5 Módulo de notificaciones

La app deberá enviar recordatorios para los horarios configurados.

### Notificación en la mañana

Ejemplo:

```text
Hora de medicamento
Prednisona 5 mg - 1 tableta con el desayuno
```

### Notificación con la comida

Ejemplo:

```text
Hora de medicamento
Leflunomida 20 mg - 1 tableta con la comida
```

### Notificación en la noche

Ejemplo:

```text
Hora de medicamento
Metotrexato 2.5 mg - 3 tabletas con la cena
```

### Acciones sugeridas desde la notificación

- Marcar como tomado.
- Recordar después.
- Abrir detalle del día.

### Reglas

- La notificación debe enviarse solo si hay medicamentos programados para ese horario.
- Si hay varios medicamentos en el mismo horario, la notificación puede agruparlos.
- Si el usuario confirma desde la notificación, la toma debe actualizarse en el historial.
- Si no confirma después de cierto tiempo, se debe generar una alerta de dosis pendiente.

---

## 5.6 Módulo de historial diario

Permite consultar el cumplimiento del tratamiento por día.

### Funciones

- Ver medicamentos tomados.
- Ver medicamentos pendientes.
- Ver medicamentos omitidos.
- Ver hora de confirmación.
- Ver porcentaje de cumplimiento diario.

### Ejemplo

```text
9 mayo 2026

Tomados:
✓ Prednisona - 8:15 AM
✓ Leflunomida - 2:05 PM

Pendientes:
! Metotrexato - Noche

Cumplimiento del día: 66%
```

---

## 5.7 Módulo de pendientes

Permite detectar y alertar tomas no confirmadas.

### Reglas

- Una toma se considera pendiente si llegó su horario y no ha sido marcada como tomada.
- Una toma puede pasar a vencida si termina el día sin confirmación.
- La app debe alertar después de un tiempo configurable.
- Los medicamentos opcionales no deben afectar el cumplimiento diario.

### Ejemplo de alerta

```text
Dosis pendiente
Tienes 1 medicamento sin confirmar de la mañana.
```

---

## 5.8 Módulo de Modo Cuidador

Permite a un cuidador o familiar administrar el tratamiento sin exponer la configuración al paciente en la pantalla principal.

### Concepto

El paciente usa la app normalmente desde las pestañas Hoy, Calendario y Perfil. El acceso a funciones administrativas (agregar medicamentos, ver historial completo, ajustar recordatorios) requiere desbloquear el Modo Cuidador mediante un PIN de 4 dígitos.

### Flujos de acceso

```text
Primera vez:
Perfil → Modo Cuidador → Crear PIN → Confirmar PIN → Acceso concedido

Siguientes veces:
Perfil → Modo Cuidador → Ingresar PIN → Acceso concedido

PIN incorrecto:
Ingresar PIN → Error con mensaje → Reintentar (sin bloqueo temporal en MVP)
```

### Funciones disponibles en Modo Cuidador

- Acceder a historial completo del tratamiento.
- Ver y gestionar lista de medicamentos activos.
- Agregar nuevo medicamento.
- Editar medicamento existente.
- Ajustar horarios de recordatorio.
- Cerrar sesión de cuidador (vuelve a vista de paciente).

### Almacenamiento del PIN

- Guardar el PIN con `EncryptedSharedPreferences` o en `DataStore` con cifrado.
- El PIN se guarda solo en el dispositivo, sin sincronización externa.
- Si el usuario desinstala la app, el PIN se pierde junto con los datos.

### Reglas

- Si no existe PIN creado, el primer acceso inicia el flujo de creación.
- El PIN tiene exactamente 4 dígitos.
- Al salir del Modo Cuidador (manualmente o al cambiar de pestaña), la sesión debe cerrarse.
- Un error de PIN muestra mensaje de retroalimentación pero no bloquea el intento en el MVP.

### Modelo sugerido

```kotlin
data class CaregiverSettings(
    val pinHash: String,
    val createdAt: LocalDateTime
)
```

---

## 6. Configuración de días específicos

La app debe permitir elegir los días de toma de cada medicamento.

### Selector sugerido

```text
[Dom] [Lun] [Mar] [Mié] [Jue] [Vie] [Sáb]
```

### Modelo sugerido

```kotlin
data class MedicationSchedule(
    val id: Long,
    val medicationId: Long,
    val dayOfWeek: DayOfWeek,
    val doseOverride: String?
)
```

### Caso de uso: dosis diferente por día

Ejemplo:

```text
Metotrexato 2.5 mg
Sábado: 3 tabletas
Domingo: 2 tabletas
Horario: Noche
```

Este caso no se debería resolver duplicando medicamentos, sino usando `doseOverride` por día.

---

## 7. Medicamentos opcionales

Algunos medicamentos no forman parte de una toma obligatoria diaria, sino que se usan solo en caso necesario.

Ejemplo:

```text
Sulindaco 200 mg
Solo si hay dolor
Máximo cada 12 horas
No tomar en ayunas
```

### Reglas

- No debe generar pendientes automáticos.
- No debe afectar negativamente el porcentaje de cumplimiento.
- Debe poder registrarse manualmente si se tomó.
- Debe permitir guardar hora y nota opcional.
- Puede mostrar una advertencia sobre intervalo mínimo entre tomas, si se configura.

---

## 8. Pantallas sugeridas

## 8.1 Pantalla principal — Hoy

Debe ser la pantalla más importante de la app.

### Contenido

- Fecha actual.
- Tratamiento activo.
- Medicamentos agrupados por horario.
- Botón para marcar como tomado.
- Indicador de pendientes.
- Acceso rápido a historial.

### Ejemplo

```text
Hoy, sábado 9 mayo 2026

Mañana
[ ] Prednisona - 1 tableta - Con desayuno

Comida
[ ] Leflunomida - 1 tableta - Con comida

Noche
[ ] Metotrexato - 3 tabletas - Con cena

Pendientes: 3
Tomados: 0
```

---

## 8.2 Pantalla de calendario

La app puede tomar como referencia visual el calendario HTML actual, pero en móvil conviene usar tarjetas por día en lugar de una tabla grande.

### Ejemplo

```text
Sábado 9 mayo
- Prednisona 1 tableta
- Leflunomida 1 tableta
- Metotrexato 3 tabletas

Domingo 10 mayo
- Prednisona 1 tableta
- Leflunomida 1 tableta
- Metotrexato 2 tabletas
```

### Nota

La vista tipo tabla puede conservarse como vista de impresión o exportación PDF, pero no debería ser la vista principal en celular.

---

## 8.3 Pantalla de historial

### Contenido

- Selector de fecha.
- Lista de tomas del día.
- Estado de cada toma.
- Hora de confirmación.
- Resumen de cumplimiento.

---

## 8.4 Pantalla de detalle de medicamento

Muestra la información completa de un medicamento individual. Accesible tocando cualquier medicamento desde la pantalla Hoy o desde la lista de medicamentos.

### Contenido

- Nombre y dosis.
- Ícono o color visual del medicamento.
- Instrucción de toma (ej. "con la comida").
- Días de toma programados.
- Horario asignado.
- Tipo: obligatorio u opcional.
- Estado del día actual: pendiente, tomado, omitido.
- Historial reciente de tomas (últimos 7 días).

---

## 8.5 Pantalla de perfil del paciente

Pestaña "Perfil" visible siempre en la barra de navegación inferior. Es la entrada a configuración personal y al Modo Cuidador.

### Contenido

- Nombre del paciente.
- Tratamiento activo (nombre y fechas).
- Resumen de cumplimiento semanal.
- Botón "Modo Cuidador" — visible siempre, pero protegido por PIN.
- Opción para ajustes generales de la app.

---

## 8.6 Pantalla de PIN y acceso al Modo Cuidador

### Estados de la pantalla

**Crear PIN (primera vez):**
- Título: "Crear PIN de cuidador".
- Teclado numérico de 4 dígitos.
- Segundo paso: confirmar PIN.
- Error si los PIN no coinciden (reinicia desde el primer paso).

**Ingresar PIN:**
- Título: "Modo Cuidador".
- Teclado numérico de 4 dígitos.
- Mensaje de error si el PIN es incorrecto.
- Botón "Cancelar" para volver al perfil.

**Panel del cuidador (CaregiverHub):**
- Accesos directos a: Historial, Mis medicamentos, Agregar medicamento.
- Ajustes de recordatorios.
- Botón "Cerrar modo cuidador".

---

## 8.7 Pantalla de creación de tratamiento

### Campos

- Nombre del tratamiento.
- Descripción opcional.
- Fecha de inicio.
- Fecha de fin.
- Horarios generales.
- Estado activo/inactivo.

---

## 8.8 Pantalla de creación de medicamento

### Campos

- Nombre.
- Dosis general.
- Instrucciones.
- Horario.
- Días de toma.
- Dosis diferente por día.
- Obligatorio/opcional.
- Color visual.

---

## 8.9 Pantalla de ajustes

### Opciones

- Cambiar horario de mañana.
- Cambiar horario de comida.
- Cambiar horario de noche.
- Activar o desactivar notificaciones.
- Definir tiempo para alerta de pendiente.
- Exportar datos.
- Limpiar historial, si se permite.
- Cambiar o restablecer PIN del Modo Cuidador.

---

## 9. Flujo principal de usuario

## 9.1 Crear tratamiento

```text
Inicio → Crear tratamiento → Capturar datos → Guardar tratamiento
```

## 9.2 Agregar medicamentos

```text
Tratamiento → Agregar medicamento → Configurar dosis/días/horario → Guardar
```

## 9.3 Uso diario

```text
Abrir app → Ver pantalla Hoy → Revisar medicamentos → Marcar como tomado → Consultar pendientes
```

## 9.4 Notificación

```text
Llega notificación → Usuario toca “Tomado” → App actualiza historial → Toma queda confirmada
```

## 9.5 Pendiente

```text
Llega horario → No se confirma toma → Espera configurable → App genera alerta pendiente
```

## 9.6 Acceso al Modo Cuidador (primera vez)

```text
Perfil → Modo Cuidador → Pantalla crear PIN → Ingresar PIN → Confirmar PIN → Panel cuidador
```

## 9.7 Acceso al Modo Cuidador (usos posteriores)

```text
Perfil → Modo Cuidador → Pantalla ingresar PIN → Verificar PIN → Panel cuidador
```

## 9.8 Error de PIN

```text
Ingresar PIN → PIN incorrecto → Mensaje de error → Limpiar dígitos → Reintentar
```

## 9.9 Cerrar Modo Cuidador

```text
Panel cuidador → Cerrar modo cuidador → Vuelve a pantalla de Perfil como paciente
```

---

## 10. Reglas de negocio principales

1. Un tratamiento debe tener fecha de inicio y fecha de fin.
2. Un medicamento debe pertenecer a un tratamiento.
3. Un medicamento puede estar programado para uno o varios días de la semana.
4. Un medicamento puede tener dosis diferente según el día.
5. Una toma se genera solo si la fecha está dentro del rango del tratamiento.
6. Una toma se genera solo si el medicamento aplica para ese día.
7. Una toma obligatoria inicia como pendiente.
8. Una toma opcional no debe considerarse pendiente automáticamente.
9. Una toma marcada como tomada debe guardar fecha y hora de confirmación.
10. Una toma no confirmada puede marcarse como omitida o vencida.
11. Las notificaciones deben respetar los horarios configurados.
12. Si no hay medicamentos para un horario, no debe enviarse notificación.
13. Los cambios en un tratamiento deben recalcular las próximas tomas.
14. El historial pasado no debería modificarse automáticamente sin confirmación del usuario.
15. La app debe funcionar sin conexión a internet.
16. El Modo Cuidador solo es accesible desde la pestaña Perfil, no desde las pestañas Hoy ni Calendario.
17. La sesión del Modo Cuidador se cierra al cambiar de pestaña o al salir de la app.
18. El PIN debe almacenarse cifrado en el dispositivo, nunca en texto plano.
19. La función de agregar o editar medicamentos solo es accesible dentro del Modo Cuidador.
20. El paciente puede ver y marcar tomas sin necesidad de ingresar al Modo Cuidador.

---

## 11. Estructura de carpetas sugerida

```text
app/
 └─ src/main/java/com/example/medtracker/
    ├─ MainActivity.kt
    ├─ MedTrackerApp.kt
    │
    ├─ core/
    │  ├─ notification/
    │  │  ├─ NotificationHelper.kt
    │  │  ├─ ReminderAlarmReceiver.kt
    │  │  └─ PendingDoseWorker.kt
    │  ├─ date/
    │  │  └─ DateUtils.kt
    │  └─ ui/
    │     └─ Theme.kt
    │
    ├─ data/
    │  ├─ local/
    │  │  ├─ AppDatabase.kt
    │  │  ├─ dao/
    │  │  │  ├─ TreatmentDao.kt
    │  │  │  ├─ MedicationDao.kt
    │  │  │  ├─ ScheduleDao.kt
    │  │  │  └─ IntakeDao.kt
    │  │  └─ entity/
    │  │     ├─ TreatmentEntity.kt
    │  │     ├─ MedicationEntity.kt
    │  │     ├─ MedicationScheduleEntity.kt
    │  │     └─ MedicationIntakeEntity.kt
    │  │
    │  ├─ repository/
    │  │  ├─ TreatmentRepository.kt
    │  │  ├─ MedicationRepository.kt
    │  │  └─ IntakeRepository.kt
    │  │
    │  └─ preferences/
    │     └─ ReminderPreferences.kt
    │
    ├─ domain/
    │  ├─ model/
    │  │  ├─ Treatment.kt
    │  │  ├─ Medication.kt
    │  │  ├─ MedicationSchedule.kt
    │  │  ├─ MedicationIntake.kt
    │  │  ├─ TimeSlot.kt
    │  │  └─ IntakeStatus.kt
    │  │
    │  └─ usecase/
    │     ├─ GenerateDailyIntakesUseCase.kt
    │     ├─ MarkIntakeAsTakenUseCase.kt
    │     ├─ GetTodayIntakesUseCase.kt
    │     ├─ CheckPendingDosesUseCase.kt
    │     └─ RescheduleRemindersUseCase.kt
    │
    ├─ presentation/
    │  ├─ navigation/
    │  │  └─ AppNavGraph.kt
    │  │
    │  ├─ today/
    │  │  ├─ TodayScreen.kt
    │  │  ├─ TodayViewModel.kt
    │  │  └─ TodayUiState.kt
    │  │
    │  ├─ calendar/
    │  │  ├─ CalendarScreen.kt
    │  │  └─ CalendarViewModel.kt
    │  │
    │  ├─ history/
    │  │  ├─ HistoryScreen.kt
    │  │  └─ HistoryViewModel.kt
    │  │
    │  ├─ treatment/
    │  │  ├─ TreatmentListScreen.kt
    │  │  ├─ TreatmentFormScreen.kt
    │  │  └─ TreatmentViewModel.kt
    │  │
    │  ├─ medication/
    │  │  ├─ MedicationListScreen.kt
    │  │  ├─ MedicationDetailScreen.kt
    │  │  ├─ MedicationFormScreen.kt
    │  │  └─ MedicationViewModel.kt
    │  │
    │  ├─ profile/
    │  │  ├─ ProfileScreen.kt
    │  │  └─ ProfileViewModel.kt
    │  │
    │  ├─ caregiver/
    │  │  ├─ PinLockScreen.kt
    │  │  ├─ CaregiverHubScreen.kt
    │  │  └─ CaregiverViewModel.kt
    │  │
    │  └─ settings/
    │     ├─ SettingsScreen.kt
    │     └─ SettingsViewModel.kt
    │
    ├─ di/
    │  └─ AppModule.kt
    │
    └─ security/
       └─ PinManager.kt
```

---

## 12. Permisos Android requeridos

En Android moderno, las notificaciones requieren permiso explícito.

### Permisos sugeridos

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

### Consideraciones

- `POST_NOTIFICATIONS` es necesario desde Android 13.
- `SCHEDULE_EXACT_ALARM` puede requerir configuración adicional según versión de Android.
- Si no se conceden alarmas exactas, se puede usar WorkManager como respaldo, aunque con menor precisión.

---

## 13. Estrategia de generación de tomas

Hay dos enfoques posibles:

## 13.1 Generar tomas por adelantado

Cuando se crea el tratamiento, se generan todos los registros de tomas entre fecha inicio y fecha fin.

### Ventajas

- Historial y calendario son fáciles de consultar.
- Se pueden programar notificaciones desde el inicio.

### Desventajas

- Si se edita el tratamiento, hay que recalcular tomas futuras.

## 13.2 Generar tomas bajo demanda

Cada día, la app calcula qué medicamentos corresponden.

### Ventajas

- Menos registros en base de datos.
- Más flexible ante cambios.

### Desventajas

- Requiere lógica más cuidadosa para historial y pendientes.

## Recomendación

Usar enfoque mixto:

- Generar o asegurar tomas del día al abrir la app.
- Generar tomas futuras necesarias para programar notificaciones.
- No modificar historial pasado automáticamente.

---

## 14. Ejemplo de tratamiento inicial basado en el calendario actual

El calendario actual puede convertirse en una plantilla inicial dentro de la app.

```text
Tratamiento Médico Mayo-Junio 2026

Inicio: 9 mayo 2026
Fin: 5 junio 2026

Medicamentos:

1. Prednisona 5 mg
   - Dosis: 1 tableta
   - Días: todos los días
   - Horario: mañana
   - Instrucción: con el desayuno
   - Obligatorio: sí

2. Leflunomida 20 mg
   - Dosis: 1 tableta
   - Días: todos los días
   - Horario: comida
   - Instrucción: con la comida
   - Obligatorio: sí

3. Ácido Fólico
   - Dosis: 1 tableta
   - Días: lunes a viernes
   - Horario: mañana
   - Instrucción: 30 minutos después del desayuno
   - Obligatorio: sí

4. Metotrexato 2.5 mg
   - Días: sábado y domingo
   - Sábado: 3 tabletas
   - Domingo: 2 tabletas
   - Horario: noche
   - Instrucción: con la cena
   - Obligatorio: sí

5. Sulindaco 200 mg
   - Uso: solo si hay dolor
   - Frecuencia: máximo cada 12 horas
   - Instrucción: no tomar en ayunas
   - Obligatorio: no
   - Tipo: opcional / según necesidad
```

---

## 15. Criterios de aceptación del MVP

### CA-01 Crear tratamiento

Dado que el usuario quiere iniciar un nuevo tratamiento, cuando capture nombre, fecha de inicio y fecha de fin, entonces la app debe guardar el tratamiento localmente.

### CA-02 Configurar medicamento

Dado un tratamiento existente, cuando el usuario agregue un medicamento con dosis, días y horario, entonces la app debe asociarlo al tratamiento.

### CA-03 Días específicos

Dado un medicamento configurado, cuando el usuario seleccione días específicos de toma, entonces la app solo debe generar tomas en esos días.

### CA-04 Dosis por día

Dado un medicamento con dosis distinta por día, cuando se genere la toma diaria, entonces la dosis mostrada debe corresponder al día configurado.

### CA-05 Pantalla Hoy

Dado que existen medicamentos programados para el día actual, cuando el usuario abra la app, entonces debe ver las tomas agrupadas por mañana, comida y noche.

### CA-06 Notificación

Dado que hay medicamentos programados para un horario, cuando llegue la hora configurada, entonces la app debe mostrar una notificación al usuario.

### CA-07 Confirmar toma

Dado que existe una toma pendiente, cuando el usuario marque “Tomado”, entonces la app debe registrar la fecha y hora de confirmación.

### CA-08 Historial diario

Dado que existen tomas registradas, cuando el usuario consulte una fecha, entonces la app debe mostrar tomas tomadas, pendientes, omitidas o vencidas.

### CA-09 Dosis pendiente

Dado que una toma no fue confirmada después del tiempo configurado, cuando se ejecute la revisión de pendientes, entonces la app debe mostrar una alerta.

### CA-10 Medicamento opcional

Dado un medicamento marcado como opcional, cuando llegue el día actual, entonces no debe generar pendiente automática ni afectar el porcentaje de cumplimiento.

### CA-11 Crear PIN de cuidador

Dado que no existe un PIN configurado, cuando el usuario acceda a Modo Cuidador, entonces la app debe mostrar el flujo de creación y confirmación de PIN antes de conceder acceso.

### CA-12 Ingresar PIN de cuidador

Dado que ya existe un PIN configurado, cuando el usuario acceda a Modo Cuidador e ingrese el PIN correcto, entonces la app debe conceder acceso al panel de cuidador.

### CA-13 PIN incorrecto

Dado que el usuario ingresa un PIN incorrecto, cuando lo confirme, entonces la app debe mostrar un mensaje de error y permitir reintentar sin bloquear el acceso.

### CA-14 Sesión de cuidador

Dado que el cuidador está autenticado, cuando cambie de pestaña o salga de la app, entonces la sesión debe cerrarse y requerir PIN nuevamente en el próximo acceso.

### CA-15 Restricción de edición al cuidador

Dado que el usuario es el paciente (sin sesión de cuidador activa), cuando navegue por la app, entonces no debe tener acceso a las pantallas de agregar o editar medicamentos.

---

## 16. Consideraciones de seguridad y salud

La app debe incluir un aviso visible:

```text
Esta aplicación es una herramienta de apoyo para recordar medicamentos. No sustituye la indicación, diagnóstico ni seguimiento de un profesional de la salud.
```

También se recomienda:

- No sugerir cambios de dosis.
- No sugerir suspensión de medicamentos.
- No interpretar síntomas.
- Permitir que el usuario registre notas, pero no dar recomendaciones médicas automáticas.

---

## 17. Roadmap sugerido

## Fase 1 — MVP funcional

- Proyecto Android base.
- Pantalla Hoy.
- Crear tratamiento.
- Crear medicamentos.
- Configurar días y horarios.
- Room Database.
- Marcar como tomado.
- Historial diario.

## Fase 2 — Notificaciones

- Permiso de notificaciones.
- AlarmManager.
- Notificaciones por horario.
- Acción rápida “Tomado”.
- Revisión de pendientes.

## Fase 3 — Modo Cuidador y mejoras de experiencia

- Modo Cuidador protegido por PIN.
- Pantalla de perfil del paciente.
- Panel del cuidador con acceso a historial, lista y agregar medicamento.
- PinManager con `EncryptedSharedPreferences`.
- Vista calendario mensual.
- Colores por medicamento.
- Filtros de historial.
- Resumen semanal.
- Exportación PDF.
- Compartir calendario.

## Fase 4 — Funciones avanzadas

- Múltiples pacientes.
- Modo cuidador.
- Respaldo local.
- Importar/exportar tratamiento.
- Foto del medicamento.
- Reporte de adherencia.

---

## 18. Recomendación final

La app no debería ser una simple conversión del HTML a WebView, porque las funciones requeridas implican persistencia, notificaciones, historial y lógica diaria.

El HTML actual debe usarse como:

- Referencia visual.
- Plantilla inicial de tratamiento.
- Base para futura exportación o impresión.

La app real debe construirse como Android nativo usando:

```text
Kotlin + Jetpack Compose + Room + DataStore + AlarmManager + WorkManager
```

Con esa base, la app podrá funcionar 100% local, enviar recordatorios, registrar tomas y evolucionar hacia un sistema más completo de seguimiento de medicamentos.
