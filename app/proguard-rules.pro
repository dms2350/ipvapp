# ProGuard Rules for IPTV Android App
# Generated: 2025-11-18
# Description: Reglas para mantener compatibilidad con Hilt, Room, Retrofit, VLC y Compose

# ================================
# CONFIGURACIÓN GENERAL
# ================================

# Mantener información de líneas para stack traces legibles
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Mantener anotaciones
-keepattributes *Annotation*

# Mantener información de genéricos para Retrofit y Gson
-keepattributes Signature

# Mantener información de excepciones
-keepattributes Exceptions

# ================================
# HILT / DAGGER (Dependency Injection)
# ================================

-dontwarn com.google.errorprone.annotations.**

# Mantener todas las clases de Dagger/Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Mantener componentes generados por Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_Impl { *; }

# Mantener clases inyectadas
-keep class com.dms2350.iptvapp.di.** { *; }

# ================================
# ROOM DATABASE
# ================================

# Mantener Database y DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Mantener entities del proyecto
-keep class com.dms2350.iptvapp.data.database.entities.** { *; }
-keep class com.dms2350.iptvapp.data.database.dao.** { *; }
-keep class com.dms2350.iptvapp.data.database.IPTVDatabase { *; }

-dontwarn androidx.room.paging.**

# ================================
# RETROFIT (HTTP Client)
# ================================

# Mantener Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Mantener interfaces de API
-keep interface com.dms2350.iptvapp.data.api.** { *; }

# Platform calls Class.forName on types which do not exist on Android to determine platform
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8

# Retain generic type information for use by reflection by converters and adapters
-keepattributes Signature

# Retain declared checked exceptions for use by a Proxy instance
-keepattributes Exceptions

# ================================
# GSON (JSON Serialization)
# ================================

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Mantener campos con @SerializedName
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Mantener DTOs y Models
-keep class com.dms2350.iptvapp.data.api.dto.** { *; }
-keep class com.dms2350.iptvapp.domain.model.** { *; }

# ================================
# OKHTTP (HTTP Client Base)
# ================================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ================================
# LIBVLC (Video Player)
# ================================

# Mantener todas las clases de VLC (usa JNI - código nativo)
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.medialibrary.** { *; }
-keep interface org.videolan.libvlc.** { *; }

-dontwarn org.videolan.**

# ================================
# KOTLIN
# ================================

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ================================
# KOTLINX COROUTINES
# ================================

-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ================================
# JETPACK COMPOSE
# ================================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# ================================
# ANDROIDX / JETPACK
# ================================

# Lifecycle
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ================================
# COIL (Image Loading)
# ================================

-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ================================
# CLASES DEL PROYECTO
# ================================

# Mantener Application class
-keep class com.dms2350.iptvapp.IPTVApplication { *; }

# Mantener MainActivity
-keep class com.dms2350.iptvapp.presentation.MainActivity { *; }

# Mantener ViewModels (usados por Hilt)
-keep class com.dms2350.iptvapp.presentation.**.ViewModel** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Mantener Repositories
-keep class com.dms2350.iptvapp.data.repository.** { *; }
-keep class com.dms2350.iptvapp.domain.repository.** { *; }

# Mantener UseCases
-keep class com.dms2350.iptvapp.domain.usecase.** { *; }

# Mantener VLCPlayerManager (Singleton crítico)
-keep class com.dms2350.iptvapp.presentation.ui.player.VLCPlayerManager { *; }

# ================================
# ENUMS
# ================================

# Mantener enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================
# SERIALIZABLE
# ================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================
# PARCELABLE
# ================================

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ================================
# DEBUGGING
# ================================

# Remover logs en release (opcional - descomentar si quieres)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# ================================
# WARNINGS A IGNORAR
# ================================

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# ================================
# FIN DE REGLAS
# ================================

