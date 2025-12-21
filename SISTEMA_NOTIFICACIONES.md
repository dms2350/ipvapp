# 📢 Sistema de Notificaciones en Tiempo Real

## Descripción General

Sistema completo para recibir y mostrar notificaciones desde el backend de manera automática cuando la aplicación está activa (en el menú principal o reproduciendo un canal).

## 🎯 Características Implementadas

### 1. **Polling Automático**
- Consulta al backend cada 30 segundos para obtener notificaciones activas
- Solo funciona cuando la app está abierta
- Se detiene automáticamente cuando se cierra la app

### 2. **Priorización de Notificaciones**
- **Bloqueadas (is_blocked: true)**: Color naranja, se muestran primero
- **Normales (is_blocked: false)**: Color azul, se muestran después

### 3. **Control de Visualización**
- Cada notificación se muestra según el tiempo definido en el backend (`display_duration`)
- Las notificaciones ya mostradas no se repiten
- Banner animado con entrada/salida suave
- Botón de cierre manual para descartar notificaciones

### 4. **Interfaz de Usuario**
- Banner flotante en la parte **inferior** de la pantalla
- Diseño adaptativo con Material Design 3
- Animaciones fluidas (entra/sale desde abajo)
- No interfiere con la navegación de la app
- **Azul** para notificaciones normales
- **Naranja** para servicios bloqueados

## 📋 Estructura de Implementación

### **Modelos de Datos**

#### NotificationDto.kt
```kotlin
data class NotificationsResponse(
    val is_blocked: Boolean,           // Si el servicio está bloqueado
    val message: String?,              // Mensaje adicional (se combina con notification)
    val notification: String?,         // Texto principal de la notificación
    val notification_duration: Int?    // Duración en segundos (null = 10 seg por defecto)
)
```

**La app procesa esta respuesta y la convierte en:**

#### Notification.kt (Dominio)
```kotlin
data class Notification(
    val id: Int,                       // Hash del mensaje completo (notification + message)
    val title: String,                 // "⚠️ Servicio Bloqueado" o "📢 Notificación"
    val message: String,               // Combinación de notification + "\n\n" + message (si existe)
    val displayDuration: Int,          // notification_duration del backend (o 10 seg por defecto)
    val isBlocked: Boolean,            // true = naranja (bloqueado), false = azul (normal)
    val isActive: Boolean,
    val createdAt: String?
)
```

### **Capa de Datos**

#### IPTVApi.kt
```kotlin
@GET("devices/check-status/{device_id}")
suspend fun getDeviceNotifications(@Path("device_id") deviceId: String): Response<NotificationsResponse>
```

#### NotificationRepository.kt
```kotlin
interface NotificationRepository {
    suspend fun getActiveNotifications(): Result<List<Notification>>
}
```

**Nota:** El repositorio obtiene automáticamente el `device_id` del dispositivo usando `Settings.Secure.ANDROID_ID`.

### **Servicio de Polling**

#### NotificationPollingService.kt
Características:
- Polling periódico cada 30 segundos
- Gestión automática de notificaciones mostradas
- Control de cola de notificaciones
- Métodos:
  - `startPolling()`: Inicia la consulta periódica
  - `stopPolling()`: Detiene la consulta
  - `dismissCurrentNotification()`: Descarta manualmente
  - `clearShownNotifications()`: Limpia el caché

### **Componente UI**

#### NotificationBanner.kt
- Banner animado con Material Design 3
- Colores según prioridad
- Botón de cierre
- Animaciones de entrada/salida
- Máximo 2 líneas de título, 3 de mensaje

### **ViewModel**

#### NotificationViewModel.kt
Expone:
- `currentNotification`: Notificación actual visible
- `dismissNotification()`: Descarta notificación manualmente

## 🔄 Flujo de Funcionamiento

```
1. App se abre → MainActivity inicia NotificationPollingService
2. Servicio consulta endpoint cada 30s
3. Backend devuelve notificaciones activas
4. Servicio filtra notificaciones nuevas (no mostradas)
5. Ordena por prioridad (HIGH > MEDIUM > LOW)
6. Muestra la primera notificación
7. Espera el tiempo de display_duration
8. Auto-oculta y marca como mostrada
9. Muestra siguiente (si hay más)
10. App se cierra → Detiene polling
```

## 🛠️ Endpoint del Backend

### **GET** `/devices/check-status/{device_id}`

#### Parámetros:
- `device_id` (Path): El ANDROID_ID del dispositivo (obtenido automáticamente por la app)

#### Request:
```
GET https://playtv-production.up.railway.app/devices/check-status/1a7dcf6ff688ccfe
```

#### Response exitoso (200):
```json
{
  "is_blocked": false,
  "message": null,
  "notification": "Su servicio esta proximo a vencer comuniquese con soporte.",
  "notification_duration": 15
}
```

**Cuando el servicio está bloqueado:**
```json
{
  "is_blocked": true,
  "message": null,
  "notification": "Su servicio ha sido bloqueado. Contacte a soporte.",
  "notification_duration": 20
}
```

**Cuando no hay notificaciones:**
```json
{
  "is_blocked": false,
  "message": null,
  "notification": null,
  "notification_duration": 0
}
```


**Nota:** La app obtiene automáticamente el `device_id` usando `Settings.Secure.ANDROID_ID` del dispositivo.

## 🎨 Colores de Notificaciones

| Tipo | Título | Color | Código Hex | Campo Backend |
|------|--------|-------|------------|---------------|
| Servicio Bloqueado | ⚠️ Servicio Bloqueado | Naranja | #FF9800 | `is_blocked: true` en `notification` |
| Notificación Normal | 📢 Notificación | Azul | #1976D2 | `is_blocked: false` en `notification` |
| Información Adicional | ℹ️ Información | Azul | #1976D2 | Campo `message` (siempre azul) |

## ⚙️ Configuración

### Intervalo de Polling
Para cambiar el intervalo de consulta, editar en `NotificationPollingService.kt`:

```kotlin
private val POLLING_INTERVAL = 30_000L // 30 segundos (en milisegundos)
```

### Tiempo de Visualización
Se controla desde el backend mediante el campo `display_duration` (en segundos).

## 📱 Ciclo de Vida

### Inicio (onCreate)
```kotlin
MainActivity.onCreate() → 
  initializeNotifications() → 
    notificationPollingService.startPolling()
```

### Cierre (onDestroy)
```kotlin
MainActivity.onDestroy() → 
  notificationPollingService.stopPolling()
```

## 🔧 Funciones Disponibles

### Descartar Notificación Manualmente
El usuario puede cerrar la notificación tocando el botón X:
```kotlin
NotificationBanner(
    onDismiss = { viewModel.dismissNotification() }
)
```

### Limpiar Caché de Mostradas
Para resetear qué notificaciones se han mostrado:
```kotlin
notificationPollingService.clearShownNotifications()
```

## 📝 Logging

El sistema incluye logging detallado para debugging:

```
NOTIFICATIONS: Iniciando servicio de polling de notificaciones
NotificationPolling: Consultando notificaciones activas...
NotificationPolling: Se obtuvieron 2 notificaciones
NotificationPolling: Mostrando notificación: Título
NotificationPolling: Notificación ocultada: Título
NOTIFICATIONS: Servicio de polling detenido
```

## ✅ Ventajas del Sistema

1. **No invasivo**: Solo consulta cuando la app está activa
2. **Eficiente**: Polling cada 30s, no sobrecarga el servidor
3. **Inteligente**: No repite notificaciones ya mostradas
4. **Priorizado**: Muestra primero las más importantes
5. **Automático**: No requiere intervención del usuario
6. **Configurable**: Tiempo y prioridad controlados desde backend

## 🚀 Uso desde el Backend

### Para enviar una notificación al dispositivo:

1. **Notificación Normal:**
   ```json
   {
     "is_blocked": false,
     "message": null,
     "notification": "Texto de la notificación",
     "notification_duration": 15
   }
   ```
   - Se mostrará con título "📢 Notificación"
   - Color **AZUL** (#1976D2)
   - Duración: 15 segundos (definido en el backend)

2. **Notificación de Servicio Bloqueado (con información adicional):**
   ```json
   {
     "is_blocked": true,
     "message": "Su factura ha vencido.",
     "notification": "Verificación de color",
     "notification_duration": 20
   }
   ```
   
   **Se mostrarán 2 notificaciones separadas:**
   
   **Primera notificación:**
   - Título: "⚠️ Servicio Bloqueado"
   - Mensaje: "Verificación de color"
   - Color: **NARANJA** (#FF9800)
   - Duración: 20 segundos
   
   **Segunda notificación:**
   - Título: "ℹ️ Información"
   - Mensaje: "Su factura ha vencido."
   - Color: **AZUL** (#1976D2)
   - Duración: 20 segundos
   - Se muestra después de la primera

3. **Sin notificaciones:**
   ```json
   {
     "is_blocked": false,
     "message": null,
     "notification": null,
     "notification_duration": 0
   }
   ```
   - No se muestra nada en la app

### Notas Importantes:

- **Notificaciones separadas:** Si el backend envía tanto `notification` como `message`, se crearán **2 notificaciones separadas**:
  - Primera: Basada en `notification` (color según `is_blocked`)
  - Segunda: Basada en `message` (siempre azul con título "ℹ️ Información")
- **Evitar duplicados:** La app usa un hash de cada campo como ID. Cada notificación se rastrea independientemente.
- **Actualizar notificación:** Para mostrar una nueva notificación, cambiar el texto del campo `notification` o `message`.
- **Duración configurable:** El valor `notification_duration` se aplica a **ambas** notificaciones. Si es `null` o `0`, se usarán 10 segundos por defecto.
- **Orden de visualización:** 
  1. Primero se muestra la notificación de `notification` (naranja si bloqueado, azul si normal)
  2. Luego se muestra la de `message` (siempre azul)
  3. Las bloqueadas tienen prioridad sobre las normales
- **Posición:** El banner aparece en la **parte inferior** de la pantalla con animación desde abajo.

## 🔐 Seguridad

- Solo se muestran notificaciones con `is_active = true`
- El servicio se detiene completamente cuando la app se cierra
- No se almacenan notificaciones localmente (solo IDs mostrados)

## 📦 Archivos Creados/Modificados

### Nuevos Archivos:
- `NotificationDto.kt`
- `Notification.kt`
- `NotificationRepository.kt`
- `NotificationRepositoryImpl.kt`
- `GetActiveNotificationsUseCase.kt`
- `NotificationPollingService.kt`
- `NotificationBanner.kt`
- `NotificationViewModel.kt`

### Modificados:
- `IPTVApi.kt` - Agregado endpoint
- `RepositoryModule.kt` - Agregado binding
- `MainActivity.kt` - Integrado servicio
- `MainScreen.kt` - Agregado banner
- `NetworkModule.kt` - Configuración SSL para Railway

---

**Desarrollado para PlayTV+** 
Sistema de notificaciones en tiempo real v1.0

