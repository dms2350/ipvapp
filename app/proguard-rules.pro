# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== NO OFUSCAR NADA DE LA APP (temporal para debugging) =====
-keep class com.dms2350.iptvapp.** { *; }
-keepclassmembers class com.dms2350.iptvapp.** { *; }

# ===== VLC LibVLC =====
# Mantener todas las clases de VLC (necesarias para JNI)
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# ===== Retrofit & OkHttp =====
# Retrofit hace uso de reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# CRÍTICO: Mantener información de tipos genéricos para Retrofit
-keepattributes Exceptions
-keepattributes *Annotation*

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Mantener interfaces de API SIN OFUSCAR
-keep,allowobfuscation,allowshrinking interface com.dms2350.iptvapp.data.api.** { *; }

# Mantener clases de Retrofit y sus tipos genéricos
-keep class retrofit2.** { *; }
-keep class retrofit2.Response { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Mantener tipos de retorno de Retrofit (Response<T>)
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn retrofit2.-KotlinExtensions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ===== Gson =====
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Mantener TODAS las clases de datos (DTOs y modelos) SIN OFUSCAR
-keep,allowobfuscation class com.dms2350.iptvapp.data.api.dto.** { *; }
-keep,allowobfuscation class com.dms2350.iptvapp.domain.model.** { *; }
-keep,allowobfuscation class com.dms2350.iptvapp.data.database.entities.** { *; }

# Prevenir que Gson use reflection en clases de datos
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Mantener constructores de clases de datos
-keepclassmembers class com.dms2350.iptvapp.data.api.dto.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.dms2350.iptvapp.domain.model.** {
    <init>(...);
    <fields>;
}

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Mantener todas las clases DAO
-keep class com.dms2350.iptvapp.data.database.dao.** { *; }
-keep class com.dms2350.iptvapp.data.database.** { *; }

# ===== Hilt =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.Provides class * { *; }

# Mantener todos los módulos de inyección
-keep class com.dms2350.iptvapp.di.** { *; }

# Mantener ViewModels
-keep class com.dms2350.iptvapp.presentation.**.ViewModel { *; }
-keep class com.dms2350.iptvapp.presentation.**.ViewModel$* { *; }

# Mantener repositorios
-keep class com.dms2350.iptvapp.data.repository.** { *; }
-keep interface com.dms2350.iptvapp.domain.repository.** { *; }

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===== Compose =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===== Coil (Image Loading) =====
-keep class coil.** { *; }
-dontwarn coil.**

# ===== DataStore =====
-keep class androidx.datastore.*.** { *; }

# ===== Mantener información de línea para debugging =====
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== Mantener recursos del launcher (iconos) =====
-keep class **.R
-keep class **.R$* {
    <fields>;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Mantener recursos de mipmap (iconos del launcher)
-keep class * extends android.content.res.Resources
-keepclassmembers class * extends android.content.res.Resources {
    *;
}

# ===== Mantener toda la estructura de la app (temporal para debugging) =====
# Mantener Application class
-keep class com.dms2350.iptvapp.IPTVApplication { *; }

# Mantener todas las Activities
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class com.dms2350.iptvapp.presentation.MainActivity { *; }

# Mantener todos los Composables (funciones @Composable)
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Mantener Navigation
-keep class androidx.navigation.** { *; }

# Mantener UseCase
-keep class com.dms2350.iptvapp.domain.usecase.** { *; }