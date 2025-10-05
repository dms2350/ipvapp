# IPTV Android App

Una aplicación IPTV moderna para Android desarrollada con Kotlin, Jetpack Compose y LibVLC, diseñada para ofrecer streaming de alta calidad con compatibilidad universal de códecs.

## 🚀 Características

### ✨ Reproductor Universal
- **LibVLC Player** con soporte completo de códecs (mpga, AAC, AC-3, DTS)
- **Compatibilidad universal** con dispositivos Xiaomi, Samsung, TV Box
- **Streaming HTTP/HTTPS** sin restricciones
- **Aceleración por hardware** automática

### 📺 Funcionalidades IPTV
- **Lista de canales** con categorías y filtros
- **Navegación fluida** entre canales (flechas, botones)
- **Información del canal** con overlay auto-ocultable
- **Controles táctiles** para dispositivos móviles
- **Soporte TV/TV Box** con control remoto

### 🎮 Controles Intuitivos
- **Dispositivos táctiles**: Toque en pantalla para mostrar controles
- **TV/TV Box**: Control remoto (Centro/OK, flechas, espacio)
- **Auto-ocultar**: Controles se ocultan automáticamente después de 3 segundos
- **Pausa/Play**: Control de reproducción integrado

### 🏗️ Arquitectura Moderna
- **MVVM** con ViewModels y StateFlow
- **Dependency Injection** con Hilt
- **Room Database** para caché offline
- **Retrofit** para comunicación con API
- **Jetpack Compose** para UI declarativa

## 🛠️ Stack Tecnológico

### Frontend (Android)
- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI moderna y declarativa
- **LibVLC** - Reproductor universal de video
- **Hilt** - Inyección de dependencias
- **Room** - Base de datos local
- **Retrofit** - Cliente HTTP
- **Coil** - Carga de imágenes

### Backend (API)
- **FastAPI** - Framework web de alto rendimiento
- **PostgreSQL** - Base de datos principal
- **Redis** - Caché y sesiones
- **SQLAlchemy** - ORM
- **Pydantic** - Validación de datos

## 📱 Requisitos

- **Android 7.0** (API level 24) o superior
- **2GB RAM** mínimo recomendado
- **Conexión a internet** para streaming
- **Almacenamiento**: 100MB para la app

## 🚀 Instalación

### Desarrollo
1. **Clonar repositorio**
```bash
git clone https://github.com/dms2350/ipvapp.git
cd ipvapp
```

2. **Abrir en Android Studio**
- Importar proyecto
- Sync Gradle
- Configurar emulador o dispositivo físico

3. **Configurar API**
```kotlin
// En Constants.kt
const val BASE_URL = "https://tu-api.com/"
```

4. **Ejecutar**
- Build → Run 'app'

### Producción
- Descargar APK desde [Releases](https://github.com/dms2350/ipvapp/releases)
- Instalar en dispositivo Android
- Configurar fuentes IPTV

## 🎯 Uso

### Configuración Inicial
1. **Abrir aplicación**
2. **Configurar fuentes** (M3U URLs)
3. **Seleccionar categorías** de interés
4. **¡Comenzar a ver!**

### Navegación
- **Lista de canales**: Toque para reproducir
- **Cambiar canal**: Flechas izquierda/derecha
- **Controles**: Toque en pantalla (móvil) o Centro/OK (TV)
- **Pausa/Play**: Espacio o botón de pausa
- **Información**: Se muestra automáticamente al cambiar canal

### Dispositivos Soportados
- **Smartphones** Android (táctil)
- **Tablets** Android (táctil)
- **TV Box** Android (control remoto)
- **Android TV** (control remoto)
- **Dispositivos Xiaomi** (compatibilidad especial)

## 🔧 Configuración Avanzada

### Fuentes IPTV
```kotlin
// Formatos soportados
- M3U/M3U8 playlists
- HTTP/HTTPS streams
- Códecs: mpga, AAC, AC-3, DTS, MP3, Opus
```

### Optimizaciones por Dispositivo
- **Xiaomi**: Configuración especial para códecs mpga
- **TV Box**: Optimización para control remoto
- **Samsung**: Configuración estándar
- **Emuladores**: Buffer optimizado

## 🐛 Solución de Problemas

### Audio no funciona
- ✅ **Solucionado**: LibVLC soporta todos los códecs
- Especialmente en dispositivos Xiaomi con streams HTTP

### Navegación de canales
- ✅ **Solucionado**: Índice de canales corregido
- Navegación fluida entre canales

### Controles no aparecen
- **Solución**: Toque en pantalla (móvil) o Centro/OK (TV)
- Se ocultan automáticamente después de 3 segundos

### Performance
- **Caché**: Datos almacenados localmente
- **Streaming**: Optimizado para conexiones lentas
- **Memoria**: Gestión automática de recursos

## 🤝 Contribuir

1. **Fork** el repositorio
2. **Crear branch** para feature (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. **Push** al branch (`git push origin feature/nueva-funcionalidad`)
5. **Crear Pull Request**

### Desarrollo Local
```bash
# Instalar dependencias
./gradlew build

# Ejecutar tests
./gradlew test

# Generar APK
./gradlew assembleDebug
```



## 🔄 Changelog

### v1.0.0 (2024-01-XX)
- ✅ Migración de ExoPlayer a LibVLC
- ✅ Soporte universal de códecs
- ✅ Compatibilidad con dispositivos Xiaomi
- ✅ Controles táctiles y de TV
- ✅ Auto-ocultar información y controles
- ✅ Navegación de canales corregida

### Próximas Versiones
- 🔄 EPG (Guía de programación)
- 🔄 Favoritos sincronizados
- 🔄 Picture-in-Picture
- 🔄 Modo oscuro/claro
- 🔄 Configuraciones avanzadas

---
