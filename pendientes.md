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

## Implementado

- ✅ **Notificaciones con sonido en silencio** — canal `medication_reminders_v2` con `USAGE_ALARM`
- ✅ **Rediseño TodayScreen** — header, progreso, próxima toma, slots colapsables, reordenamiento en tiempo real
- ✅ **Rediseño ProfileScreen** — avatar con inicial, card modo cuidador, sección APLICACIÓN
- ✅ **Rediseño CalendarScreen** — franja semanal + tarjeta de detalle + cuadrícula mensual con colores
- ✅ **Primer día de semana configurable** — opción en Ajustes, todos los calendarios se adaptan
