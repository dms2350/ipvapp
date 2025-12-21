# Sistema de Bloqueo de Servicio Mejorado

## Fecha de Implementación
Diciembre 21, 2025

## Resumen de Cambios

Se ha implementado un sistema completo de bloqueo de servicio que detiene completamente la reproducción y permite al usuario salir de la app cuando el servicio está bloqueado.

---

## 1. Componentes Modificados

### 1.1. `NotificationViewModel.kt`

**Cambios principales:**
- ✅ Ahora inyecta `VLCPlayerManager` para controlar el reproductor
- ✅ Agrega `NavController` para navegación programática
- ✅ Mantiene estado de bloqueo con `_isServiceBlocked: StateFlow<Boolean>`
- ✅ **Libera completamente el reproductor** cuando se bloquea el servicio (método `release()`)
- ✅ Navega automáticamente a la pantalla de canales cuando se bloquea

**Comportamiento:**
```kotlin
// Cuando se detecta bloqueo (is_blocked: true)
1. Llama a vlcPlayerManager.release() // Destruye MediaPlayer y LibVLC
2. Navega a "channels" (pantalla principal)
3. Marca _isServiceBlocked = true

// Cuando se desbloquea (is_blocked: false)
1. Marca _isServiceBlocked = false
2. El usuario puede usar la app normalmente
```

### 1.2. `BlockedServiceModal.kt`

**Cambios principales:**
- ✅ Agregado **botón "Salir de la aplicación"**
- ✅ Icono de salida con `Icons.Default.ExitToApp`
- ✅ Botón blanco con texto naranja para contraste
- ✅ Cierra completamente la app con `Activity.finishAffinity()`

**Funcionalidad del botón:**
- **Título:** "Salir de la aplicación"
- **Acción:** Cierra la app completamente (no solo minimiza)
- **Beneficio:** Permite al usuario usar su TV/móvil para otras cosas

### 1.3. `MainScreen.kt`

**Cambios principales:**
- ✅ Pasa `navController` al `NotificationViewModel` usando `LaunchedEffect`
- ✅ Permite que el ViewModel navegue cuando se bloquea el servicio

---

## 2. Flujo de Bloqueo Completo

### Escenario: Backend Bloquea el Servicio

**1. Backend Envía:**
```json
{
  "is_blocked": true,
  "message": "Su factura ha vencido.",
  "notification": "SERVICIO BLOQUEADO",
  "notification_duration": 86400
}
```

**2. App Detecta (máximo 30 segundos - intervalo de polling):**
- NotificationPollingService recibe la respuesta
- Crea una notificación de bloqueo
- Emite la notificación en `currentNotification` StateFlow

**3. NotificationViewModel Reacciona:**
```
handleNotificationChange() detecta is_blocked = true
  ↓
vlcPlayerManager.release() // Destruye reproductor completamente
  ↓
Navega a "channels" // Pantalla principal de canales
  ↓
_isServiceBlocked = true
```

**4. UI Muestra:**
- ✅ **Modal naranja de pantalla completa** aparece
- ✅ Título: "SERVICIO BLOQUEADO" (o lo que envíe el backend)
- ✅ Mensaje: "Su factura ha vencido." (o lo que envíe el backend)
- ✅ Mensaje de contacto: "Contacte con soporte para reactivar su servicio"
- ✅ **Botón "Salir de la aplicación"**
- ✅ No se puede cerrar con back button o tocando fuera
- ✅ No hay audio/video reproduciéndose de fondo

**5. Usuario Puede:**
- **Opción A:** Esperar a que se reactive el servicio desde el backend
- **Opción B:** Presionar "Salir de la aplicación" para cerrar completamente la app

---

## 3. Flujo de Desbloqueo

### Escenario: Backend Desbloquea el Servicio

**1. Backend Envía:**
```json
{
  "is_blocked": false,
  "message": null,
  "notification": null
}
```

**2. App Detecta (máximo 30 segundos):**
- NotificationPollingService detecta que no hay notificaciones de bloqueo activas
- Limpia `_currentNotification = null`
- Limpia el caché de notificaciones mostradas

**3. NotificationViewModel Reacciona:**
```
handleNotificationChange() detecta is_blocked = false
  ↓
_isServiceBlocked = false
  ↓
Modal desaparece automáticamente
```

**4. UI Muestra:**
- ✅ Modal desaparece
- ✅ Usuario está en pantalla de canales
- ✅ Puede seleccionar un canal y reproducir normalmente

---

## 4. Persistencia de Bloqueo

### Si el usuario sale y vuelve a entrar

**Mientras el servicio esté bloqueado en el backend:**
1. Usuario cierra la app (con botón "Salir" o manualmente)
2. Usuario abre la app nuevamente
3. App inicia polling de notificaciones
4. Backend responde con `is_blocked: true`
5. **Modal de bloqueo aparece inmediatamente** (máximo 30 segundos)
6. Usuario no puede usar la app hasta que se desbloquee desde el backend

---

## 5. Ventajas de la Implementación

### 5.1. Detención Completa del Reproductor
- ✅ **`release()` en lugar de `stop()`**
  - Destruye `MediaPlayer`
  - Libera `LibVLC`
  - Limpia memoria completamente
  - No hay fugas de audio/video

### 5.2. Navegación Automática
- ✅ Lleva al usuario a la pantalla principal
- ✅ No queda en pantalla de reproductor sin video
- ✅ Experiencia más limpia

### 5.3. Botón de Salida
- ✅ Usuario puede cerrar la app completamente
- ✅ Usa su dispositivo para otras cosas
- ✅ `finishAffinity()` - Cierre real, no minimización

### 5.4. Persistencia del Bloqueo
- ✅ Si vuelve a entrar, modal reaparece
- ✅ No puede burlar el bloqueo
- ✅ Solo se desbloquea desde el backend

### 5.5. Sin Reactivación Automática
- ✅ Al desbloquear, NO se reanuda reproducción automáticamente
- ✅ Usuario debe seleccionar canal manualmente
- ✅ Más seguro y controlado

---

## 6. Logs para Debugging

### Logs Importantes:

**Cuando se bloquea:**
```
IPTV: Servicio bloqueado - destruyendo reproductor y navegando a inicio
```

**Cuando se desbloquea:**
```
IPTV: Servicio desbloqueado - usuario puede usar la app normalmente
```

**Cuando el modal desaparece:**
```
NotificationPolling: Servicio desbloqueado - limpiando modal de bloqueo
```

---

## 7. Archivos Modificados

1. **NotificationViewModel.kt**
   - Inyección de VLCPlayerManager
   - Manejo de navegación
   - Estado de bloqueo
   - Liberación completa del reproductor

2. **BlockedServiceModal.kt**
   - Botón "Salir de la aplicación"
   - Importación de Activity
   - finishAffinity() para cierre completo

3. **MainScreen.kt**
   - LaunchedEffect para pasar navController
   - Integración con NotificationViewModel

4. **NotificationPollingService.kt** (cambios previos)
   - Detección de desbloqueo automático
   - Limpieza de modal al desbloquear

5. **NotificationRepositoryImpl.kt** (cambios previos)
   - Combinación de notification y message en bloqueos
   - Manejo separado según is_blocked

---

## 8. Testing Recomendado

### Caso 1: Bloqueo Durante Reproducción
1. ✅ Reproducir un canal
2. ✅ Bloquear desde backend
3. ✅ Verificar que audio/video se detiene
4. ✅ Verificar que modal aparece
5. ✅ Verificar que está en pantalla de canales (no player)

### Caso 2: Botón Salir
1. ✅ Bloquear servicio
2. ✅ Presionar "Salir de la aplicación"
3. ✅ Verificar que app se cierra completamente

### Caso 3: Volver a Entrar Bloqueado
1. ✅ Servicio bloqueado
2. ✅ Cerrar app
3. ✅ Volver a abrir app
4. ✅ Verificar que modal aparece nuevamente

### Caso 4: Desbloqueo
1. ✅ Servicio bloqueado, modal visible
2. ✅ Desbloquear desde backend
3. ✅ Esperar máximo 30 segundos
4. ✅ Verificar que modal desaparece
5. ✅ Verificar que puede seleccionar canales

### Caso 5: Bloqueo en Pantalla Principal
1. ✅ Estar en lista de canales (no reproduciendo)
2. ✅ Bloquear desde backend
3. ✅ Verificar que modal aparece
4. ✅ Verificar que puede salir

---

## 9. Configuración del Backend Requerida

### Endpoint: `/devices/check-status/{device_id}`

**Respuesta para Bloqueo:**
```json
{
  "is_blocked": true,
  "message": "Mensaje detallado del bloqueo (ej: Su factura ha vencido)",
  "notification": "Título del modal (ej: SERVICIO BLOQUEADO)",
  "notification_duration": 86400  // Segundos (24 horas recomendado)
}
```

**Respuesta para Desbloqueo:**
```json
{
  "is_blocked": false,
  "message": null,
  "notification": null,
  "notification_duration": 0
}
```

**Respuesta para Notificación Informativa:**
```json
{
  "is_blocked": false,
  "message": "Información adicional",
  "notification": "Mensaje principal",
  "notification_duration": 10  // Segundos
}
```

---

## 10. Consideraciones Importantes

### Tiempo de Respuesta
- **Polling:** 30 segundos
- **Detección de cambios:** Máximo 30 segundos después del cambio en backend
- Para detección más rápida, reducir `POLLING_INTERVAL` en NotificationPollingService

### Memoria
- `release()` libera completamente el reproductor
- No hay memory leaks
- VLC se reinicia cuando se selecciona un nuevo canal

### Navegación
- Siempre navega a "channels" al bloquear
- No afecta el backstack de forma incorrecta
- `popUpTo` con `inclusive = true` limpia correctamente

### UX
- Modal no se puede cerrar (by design)
- Usuario solo puede salir de la app
- Experiencia clara: "servicio bloqueado, contacte soporte o salga"

---

## 11. Mejoras Futuras Posibles

1. **Notificación push** para detección instantánea de bloqueo
2. **Botón "Contactar Soporte"** con intent a WhatsApp/Teléfono
3. **Timer visual** mostrando cuándo se volverá a verificar el estado
4. **Modo offline** con cache de último estado conocido
5. **Mensaje personalizado** por tipo de bloqueo (factura, TOS, etc.)

---

## Implementado por
- Sistema de notificaciones y bloqueo mejorado
- Fecha: Diciembre 21, 2025
- Estado: ✅ Completado y Funcional

