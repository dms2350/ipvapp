# Implementación de Registro de Usuario en PlayTV+

## 📋 Resumen

Se ha implementado una funcionalidad completa de registro de usuario que solicita **nombre** y **cédula** al instalar la app por primera vez, con opción de omitir.

## ✅ Archivos Creados

### 1. **UserPreferences.kt** - Gestión de preferencias del usuario
- **Ubicación**: `app/src/main/java/com/dms2350/iptvapp/data/local/UserPreferences.kt`
- **Función**: Guarda y recupera información del usuario usando SharedPreferences
- **Datos almacenados**:
  - Nombre del usuario
  - Cédula del usuario
  - Estado de registro completado
  - Estado de registro omitido

### 2. **RegistrationViewModel.kt** - Lógica de negocio del registro
- **Ubicación**: `app/src/main/java/com/dms2350/iptvapp/presentation/ui/registration/RegistrationViewModel.kt`
- **Función**: Maneja la validación y guardado de datos del usuario
- **Validaciones**:
  - Nombre: mínimo 3 caracteres
  - Cédula: mínimo 5 dígitos, solo números

### 3. **RegistrationScreen.kt** - Interfaz de usuario
- **Ubicación**: `app/src/main/java/com/dms2350/iptvapp/presentation/ui/registration/RegistrationScreen.kt`
- **Características**:
  - Diseño moderno con Material3
  - Campos de texto con validación en tiempo real
  - Botón "Continuar" para registrar
  - Botón "Omitir por ahora" para saltarse el registro
  - Mensajes de error informativos

## 🔄 Archivos Modificados

### 1. **DeviceInfoDto.kt**
- Agregados campos opcionales:
  - `user_full_name`: String?
  - `user_id_number`: String?

### 2. **DeviceHeartbeatService.kt**
- Inyecta `UserPreferences`
- Incluye información del usuario en el heartbeat si está disponible
- Envía datos completos a la API

### 3. **IPTVNavigation.kt**
- Agregada ruta `"registration"` como primera pantalla
- Recibe `UserPreferences` para verificar si mostrar registro
- Define `startDestination` dinámicamente según estado de registro

### 4. **MainActivity.kt**
- Inyecta `UserPreferences`
- Pasa `UserPreferences` a `MainScreen`

### 5. **MainScreen.kt**
- Recibe `UserPreferences` como parámetro
- Pasa `UserPreferences` a `IPTVNavigation`

## 📡 Integración con API

### Endpoint utilizado
```
POST /devices/heartbeat
```

### Datos enviados (JSON)
```json
{
  "device_id": "android_device_id",
  "device_type": "TV_BOX | TV | PHONE | TABLET | OTHER",
  "manufacturer": "Samsung",
  "model": "SM-G960F",
  "os_version": "11",
  "sdk_int": 30,
  "app_version": "1.0.0",
  "user_full_name": "Juan Pérez",     // Opcional
  "user_id_number": "12345678"        // Opcional
}
```

## 🎯 Flujo de Usuario

1. **Primera instalación**:
   - Usuario abre la app
   - Se muestra pantalla de registro
   - Usuario ingresa nombre y cédula O presiona "Omitir"
   - Se guarda en SharedPreferences
   - Navega automáticamente a la pantalla de canales

2. **Siguientes aperturas**:
   - App verifica si el registro fue completado
   - Si fue completado (registro o omitido), va directo a canales
   - No vuelve a mostrar la pantalla de registro

3. **Envío de datos**:
   - Cada 60 segundos el servicio envía un heartbeat
   - Si el usuario registró sus datos, se incluyen en el heartbeat
   - Si omitió, solo se envían datos del dispositivo

## 🔧 Características Técnicas

### Validaciones implementadas
- **Nombre**: No vacío, mínimo 3 caracteres
- **Cédula**: Solo números, mínimo 5 dígitos

### Manejo de estados
- Loading state durante guardado
- Estados de error con mensajes específicos
- Navegación automática al completar

### Persistencia
- Usa SharedPreferences Android
- Datos persisten entre sesiones
- Se puede borrar con `userPreferences.clearUserInfo()`

## 📱 Interfaz de Usuario

### Diseño
- Fondo oscuro (#1A1A1A)
- Card elevada con contenido (#2A2A2A)
- Colores del tema Material3
- Adaptado para TV y móvil

### Componentes
- Título: "¡Bienvenido a PlayTV+!"
- Subtítulo informativo
- 2 campos de texto (Nombre y Cédula)
- Botón primario: "Continuar"
- Botón secundario: "Omitir por ahora"
- Texto informativo: "Estos datos son opcionales..."

## 🧪 Casos de Uso

### Caso 1: Usuario completa el registro
```kotlin
// El usuario ingresa:
Nombre: "Juan Pérez"
Cédula: "12345678"

// Se guarda en SharedPreferences
userPreferences.userName = "Juan Pérez"
userPreferences.userCedula = "12345678"
userPreferences.isRegistrationCompleted = true

// Se envía en cada heartbeat
```

### Caso 2: Usuario omite el registro
```kotlin
// El usuario presiona "Omitir por ahora"

// Se guarda en SharedPreferences
userPreferences.isRegistrationSkipped = true
userPreferences.isRegistrationCompleted = true

// No se envían user_full_name ni user_id_number (null)
```

### Caso 3: Factory Reset
```kotlin
// Usuario hace factory reset del dispositivo
// SharedPreferences se borran
// Al abrir la app nuevamente, verá la pantalla de registro
```

## 🔐 Privacidad

- Los datos son **opcionales**
- El usuario puede **omitir** el registro
- Se informa claramente el uso de los datos
- Los datos se almacenan **localmente** en el dispositivo
- Solo se envían al backend si el usuario los proporciona

## 🚀 Próximos Pasos (Opcional)

1. Agregar opción en configuración para editar datos
2. Implementar sincronización con el backend
3. Agregar más campos (email, teléfono, etc.)
4. Implementar analytics sobre registros completados vs omitidos

---

**Fecha de implementación**: 2025-01-18
**Desarrollado por**: GitHub Copilot
**Proyecto**: PlayTV+ IPTV App

