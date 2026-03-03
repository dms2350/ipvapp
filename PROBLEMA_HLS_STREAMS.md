# Problema con Streams HLS (.m3u8)

## Descripción del Problema
El reproductor VLC Android no reproduce ciertos streams HLS como:
- `https://streaming.medcom.com.pa/live/elem21/index_high.m3u8`

Mientras que VLC Desktop sí los reproduce correctamente.

## Causa Raíz Identificada

**ERROR ESPECÍFICO:**
```
Certificate verification failure: The certificate is NOT trusted. 
The certificate issuer is unknown.
TLS session handshake error
```

El problema es que VLC Android no confía en el certificado SSL del servidor `streaming.medcom.com.pa`. 

**¿Por qué VLC Desktop funciona?**
- VLC Desktop tiene un almacén de certificados más completo
- Usa la configuración SSL del sistema operativo
- VLC Android usa GnuTLS con verificación estricta por defecto

## Solución Implementada

### 1. Deshabilitar Verificación de Certificados SSL en VLC

**Configuración Global:**
```kotlin
options.add("--no-gnutls-system-trust")
options.add("--gnutls-priorities=NORMAL:+VERS-ALL:+CIPHER-ALL:+COMP-ALL:+RSA:+DHE-RSA:+DHE-DSS:+ANON-DH:+ANON-ECDH:%COMPAT")
```

**Configuración por Stream (HTTPS):**
```kotlin
if (streamUrl.startsWith("https://", ignoreCase = true)) {
    media.addOption(":no-gnutls-system-trust")
    media.addOption(":gnutls-priorities=NORMAL:+VERS-ALL:+CIPHER-ALL:+COMP-ALL:+RSA:+DHE-RSA:+DHE-DSS:+ANON-DH:+ANON-ECDH:%COMPAT")
}
```

### 2. Configuración HLS Optimizada
```kotlin
// Usar avformat demuxer (mismo que VLC Desktop)
options.add("--demux=avformat")

// User agent compatible
options.add("--http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

// Optimizaciones de frames
options.add("--no-drop-late-frames")
options.add("--no-skip-frames")

// Cache adicional para muxer
options.add("--sout-mux-caching=2000")
```

### 2. Configuración Específica por Stream
Para streams .m3u8 detectados automáticamente:
```kotlin
media.addOption(":network-caching=5000")  // Buffer más grande
media.addOption(":http-reconnect")        // Reconexión automática
media.addOption(":http-continuous")       // Mantener conexión abierta
```

### 3. Logs Detallados
Se agregaron logs extensivos para debugging:
- Estado de VLC al encontrar error
- Información del Media object
- Detección automática de streams HLS

## Limitaciones Conocidas de VLC Android

Algunos streams HLS pueden NO funcionar en VLC Android debido a:

1. **Protección DRM**: Streams con Widevine u otros DRM
2. **Segmentos fragmentados complejos**: Algunos formatos HLS avanzados
3. **Restricciones del servidor**: Geoblocking, verificación de headers específicos
4. **Codecs no soportados**: H.265/HEVC en dispositivos antiguos

## Alternativas si VLC No Funciona

Si después de estas optimizaciones el stream sigue sin funcionar, considerar:

1. **ExoPlayer**: Mejor soporte HLS nativo de Android
2. **IJKPlayer**: Fork de FFmpeg optimizado para móviles
3. **Verificar el stream**: Probar con `curl` o `wget` si el stream es accesible desde Android

## Pruebas Recomendadas

1. Compilar la app con los cambios
2. Intentar reproducir el stream problemático
3. Revisar logcat para ver los logs detallados:
   ```bash
   adb logcat | grep "VLC:"
   ```
4. Verificar si aparece "VLC: Stream HLS detectado"
5. Revisar el error específico en los logs

## Próximos Pasos si Persiste el Problema

Si el stream sigue sin funcionar:
1. Capturar logs completos de VLC
2. Verificar si el stream requiere autenticación
3. Probar con ExoPlayer como alternativa
4. Contactar al proveedor del stream para verificar compatibilidad móvil
