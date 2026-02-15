package com.dms2350.iptvapp.presentation.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dms2350.iptvapp.domain.model.Notification

/**
 * Modal de bloqueo de servicio que cubre toda la pantalla
 * e impide el uso de la aplicación cuando el servicio está bloqueado
 */
@Composable
fun BlockedServiceModal(
    notification: Notification?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = notification != null && notification.isBlocked,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        notification?.let {
            // Diálogo de pantalla completa sin permitir descartarlo
            Dialog(
                onDismissRequest = { /* No permitir cerrar el modal */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9800) // Naranja - Servicio bloqueado
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 16.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Icono de advertencia
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Servicio bloqueado",
                                tint = Color.White,
                                modifier = Modifier.size(80.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Título
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mensaje
                            Text(
                                text = it.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Mensaje adicional de contacto
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Contacte con soporte para reactivar su servicio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Botón para salir de la app
                            Button(
                                onClick = {
                                    // Cerrar la aplicación completamente
                                    (context as? Activity)?.finishAffinity()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.9f),
                                    contentColor = Color(0xFFFF9800)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Salir",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Salir de la aplicación",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

