# Pendientes y ajustes por implementar

## 1. Modal de recordatorio activo (in-app)

Cuando la alarma dispara y la app está en foreground, mostrar un modal/dialog sobre la pantalla actual en lugar de solo la notificación del sistema.

**Componentes necesarios:**
- `ReminderAlarmReceiver` envía un broadcast adicional que la app escucha si está activa
- Nueva pantalla/Activity en modo dialog con:
  - Nombre del medicamento y dosis
  - Botón "Ya la tomé" → marca como TAKEN → navega a pantalla de confirmación
  - Botón "Recordar en 10 min" → reprograma alarma con AlarmManager +10 min
- El modal solo aparece si la app está en foreground; si no, cae al flujo de notificación normal

**Archivos a tocar:** `ReminderAlarmReceiver.kt`, nueva `ReminderModalScreen.kt` o Activity dialog, `AppNavGraph.kt`

**Esfuerzo estimado:** medio (~3-4 h)

---

## 2. Pantalla de confirmación de toma

Después de marcar una toma como tomada (desde el modal o desde TodayScreen), mostrar una pantalla de confirmación antes de regresar.

**Contenido:**
- Ícono de check grande
- "¡Listo!" + "Registramos tu [Medicamento] [dosis] a las [hora]"
- Si existe una próxima toma programada hoy → mostrar card "Tu próxima toma: [Medicamento] · [horario]"
- Botón "Listo" para cerrar

**Sin racha** — no implementar contador de días consecutivos.

**Archivos a tocar:** nueva `ConfirmationScreen.kt`, query a `IntakeRepository` para próxima toma, `AppNavGraph.kt`

**Esfuerzo estimado:** medio (parte del mismo bloque que el modal)

---

## 3. Rediseño de ProfileScreen

Cambiar el diseño actual de la pantalla de Perfil para que coincida con el prototipo visual.

**Nuevo diseño:**
- Header: label "PERFIL" + título "Ajustes"
- Card del paciente: avatar circular con inicial del nombre (teal), nombre completo (sin edad)
- Card "Modo cuidador": fondo teal oscuro, ícono candado, descripción corta "Historial completo, agregar medicinas, editar dosis. Requiere PIN.", chevron → navega a PinLock
- Sección "APLICACIÓN" con filas tipo lista:
  - Sonido y volumen → `SettingsScreen`
  - Permisos de notificación → sistema de permisos existente
  - Acerca de → pantalla/dialog simple con versión e info

**Sin campo de edad** — solo rediseño visual, sin cambios en el modelo de datos ni DataStore.

**Decisión pendiente:** ¿La gestión de horarios de notificaciones y PIN sigue en `SettingsScreen` al que lleva la fila "Sonido y volumen", o se divide en subsecciones?

**Archivos a tocar:** `ProfileScreen.kt`, `ProfileViewModel.kt`

**Esfuerzo estimado:** bajo-medio (~1-2 h)

---

## 4. Rediseño de CalendarScreen

Rediseño completo de la pantalla de calendario para que coincida con el prototipo visual. Reemplaza el SegmentedButton Semana/Mes por una vista combinada donde ambas secciones coexisten.

**Nueva estructura de la pantalla (de arriba a abajo):**

### 4a. Header
- Label "CALENDARIO" + título "Mayo 2026" (mes/año del día seleccionado)

### 4b. Strip semanal (siempre visible)
- Fila horizontal con los 7 días de la semana activa
- Cada celda: letra del día (L/M/M/J/V/S/D) + número
- Día seleccionado: fondo teal sólido, texto blanco
- Hoy sin seleccionar: borde teal
- Debajo de cada número: dots de colores según estado de tomas del día (solo días pasados/hoy; días futuros sin dots)
- Swipe o flechas para navegar entre semanas

### 4c. Card de detalle del día seleccionado
- Título: "Martes 5 de mayo" + subtítulo "X medicinas · Y momentos del día"
- Lista de tomas del día ordenadas por hora:
  - `[hora] • [color] [nombre medicamento]` + badge de estado:
    - TAKEN → ✓ y texto tachado
    - Badge "Próxima" → la siguiente toma pendiente más cercana
    - MISSED → sin badge (texto normal en rojo o gris)
    - PENDING futuro → sin badge
- Si el día no tiene tomas: mensaje vacío

### 4d. Sección "Mes"
- Título "Mes" + leyenda de colores: ● Completo ● Pendiente ● Olvido
- Grid mensual completo
- Cada celda del día: **fondo de color** en lugar de dots:
  - Verde (#4CAF50 aprox.) → día pasado con todas las tomas completadas
  - Rojo (#E53935 aprox.) → día pasado con alguna toma olvidada
  - Neutro/sin color → día futuro o sin tomas
  - Día seleccionado: borde o fondo teal
  - Hoy: indicador visual distintivo

**Datos necesarios:**
- Tomas del día seleccionado con hora y estado → ya disponible en `IntakeRepository`
- Cumplimiento por día del mes → requiere query de agregación (ya existe lógica en `HistoryViewModel`, reutilizable)
- "Próxima toma" → primer intake PENDING de hoy con hora futura

**Archivos a tocar:** `CalendarScreen.kt`, `CalendarViewModel.kt`, posiblemente nuevo composable `DayDetailCard.kt`

**Esfuerzo estimado:** medio-alto (~4-6 h) — rediseño significativo del layout y lógica de presentación

---

## 5. Rediseño de TodayScreen

Rediseño completo de la pantalla principal para que coincida con el prototipo visual. Mayor cambio en la estructura visual y lógica de presentación de los momentos del día.

**Nueva estructura de la pantalla (de arriba a abajo):**

### 5a. Header
- Label con fecha: "MARTES, 5 DE MAYO" (mayúsculas)
- Saludo: "Hola, [patientName]" — si no hay nombre configurado: "Hola"
- Subtítulo: "Hoy te tocan X medicinas." donde X = total de tomas obligatorias del día (mismo med en mañana + noche = 2)

### 5b. Barra de progreso
- Título "Progreso del día" + contador "X de Y" (X=tomadas, Y=total obligatorias)
- Barra teal con fill proporcional

### 5c. Card "Tu próxima toma"
- Fondo teal oscuro, ícono campana, label "TU PRÓXIMA TOMA"
- Muestra el **primer intake PENDING del día** ordenado por hora de notificación del slot
- Contenido: nombre del medicamento, "En X min · HH:MM · [instrucciones]"
- Botón "Marcar como tomado":
  - Si el slot de ese intake tiene **solo 1 med PENDING** → marca directamente
  - Si tiene **más de 1 med PENDING** → abre modal para elegir cuál (solo muestra PENDING)
- Cuando el día está completo (todos TAKEN/SKIPPED): reemplazar la card por mensaje "¡Día completo!" con ícono de check
- Ocultar card si no hay tratamiento activo

### 5d. Secciones de momentos (dropdown/colapsable)
Cada slot con al menos un medicamento programado hoy se muestra como sección colapsable. Slots sin medicamentos se ocultan.

**Header de cada sección:**
- Ícono según slot: ☀️ Mañana, 🍽 Comida, 🌙 Noche
- Nombre del momento + rango de hora inferido:
  - Mañana: desde hora configurada de Mañana hasta 1 min antes de la hora de Comida
  - Comida: desde hora de Comida hasta 1 min antes de la hora de Noche
  - Noche: desde hora de Noche hasta 23:59
- Badge numérico derecho = total de medicamentos del slot (tomados + pendientes)
- Tapping en el header expande/colapsa la lista

**Estado inicial:** todos los momentos inician colapsados.

**Contenido expandido (por cada medicamento del slot):**
- Ícono de píldora (con color del medicamento si tiene `colorHex`)
- Nombre + "X pastilla/s · HH:MM"
- Instrucciones si existen (ícono + texto)
- Círculo de estado en el lado derecho: vacío = PENDING, check verde = TAKEN, "-" gris = SKIPPED/MISSED

**Ordenamiento de secciones por hora actual:**
- El slot cuyo rango incluye la hora actual va primero
- Slots futuros siguen en orden cronológico
- Slots pasados (rango ya terminado) se mueven al final de la lista
- Este reordenamiento ocurre **en tiempo real**: el ViewModel emite un ticker cada minuto (`flow { while(true) { emit(LocalTime.now()); delay(60_000) } }`) que se combina con el resto del estado; cuando la hora cruza el límite de un slot, la lista se reordena automáticamente sin que el usuario tenga que navegar

**Datos necesarios:**
- Horas de notificación configuradas (Mañana/Comida/Noche) → ya en `ReminderPreferences`
- Intakes del día agrupados por slot → ya en `TodayUiState.intakesBySlot`
- Nombre del paciente → ya en `ReminderPreferences`

**Archivos a tocar:** `TodayScreen.kt`, `TodayViewModel.kt`, `TodayUiState.kt`

**Esfuerzo estimado:** medio-alto (~4-5 h) — rediseño completo del layout + nueva lógica de rangos horarios y ordenamiento

---

## 6. Notificaciones con sonido en modo silencio/vibración

Usar `USAGE_ALARM` + `STREAM_ALARM` en el canal de notificación para que los recordatorios suenen aunque el teléfono esté en silencio, igual que una alarma de reloj.

**Consideraciones:**
- Requiere recrear el canal de notificación con un nuevo ID (Android no permite modificar canales existentes)
- El usuario controla el volumen con el slider de "Alarma", no con el de "Notificaciones"
- Justificado para app de medicamentos (mismo comportamiento que Medisafe)
- Ningún permiso adicional requerido

**Archivos a tocar:** `NotificationHelper.kt`

**Esfuerzo estimado:** bajo (~15 min)
