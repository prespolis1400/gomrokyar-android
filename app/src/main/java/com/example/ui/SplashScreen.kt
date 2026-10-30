package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CyanNeon = Color(0xFF00F3FF)
val YellowNeon = Color(0xFFFFEA00)

fun DrawScope.drawNeonCircle(color: Color, radius: Float, strokeWidth: Float, pathEffect: PathEffect? = null) {
    // Outer glow
    drawCircle(
        color = color.copy(alpha = 0.4f),
        radius = radius,
        style = Stroke(width = strokeWidth * 4, pathEffect = pathEffect)
    )
    // Core line
    drawCircle(
        color = color,
        radius = radius,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect)
    )
}

fun DrawScope.drawNeonArc(color: Color, startAngle: Float, sweepAngle: Float, strokeWidth: Float) {
    // Outer glow
    drawArc(
        color = color.copy(alpha = 0.3f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth * 4)
    )
    // Core line
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth)
    )
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Float HUD
    val floatRotateX by infiniteTransition.animateFloat(
        initialValue = 60f, targetValue = 65f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val floatRotateY by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val floatTranslateY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // Spin 1 (140px, dashed, CW, 8s)
    val spin1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart)
    )

    // Spin 2 (100px, yellow, CCW, 3s)
    val spin2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart)
    )

    // Spin 3 (60px, cyan, CW, 2.5s cubic-bezier)
    val spin3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = CubicBezierEasing(0.68f, -0.55f, 0.265f, 1.55f)), 
            RepeatMode.Restart
        )
    )

    // Core Pulse (1s)
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // Text Pulse (1.5s -> 750ms each way)
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    cameraDistance = 8 * density
                    rotationX = floatRotateX
                    rotationY = floatRotateY
                    translationY = floatTranslateY
                },
            contentAlignment = Alignment.Center
        ) {
            // Ring 1 (140px, Dashed, Cyan)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = spin1 }
            ) {
                drawNeonCircle(
                    color = CyanNeon, 
                    radius = size.width / 2, 
                    strokeWidth = 2.dp.toPx(), 
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f))
                )
            }
            
            // Ring 2 (100px, Yellow, Top/Right/Bottom)
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { rotationZ = spin2 }
            ) {
                drawNeonArc(
                    color = YellowNeon, 
                    startAngle = -90f, 
                    sweepAngle = 270f, 
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Ring 3 (60px, Cyan, Left/Bottom)
            Canvas(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { rotationZ = spin3 }
            ) {
                drawNeonArc(
                    color = CyanNeon, 
                    startAngle = 90f, 
                    sweepAngle = 90f, 
                    strokeWidth = 4.dp.toPx()
                )
            }

            // Core
            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer {
                        scaleX = coreScale
                        scaleY = coreScale
                        alpha = coreAlpha
                    }
            ) {
                drawCircle(YellowNeon.copy(alpha = 0.6f), radius = size.width * 1.5f)
                drawCircle(YellowNeon, radius = size.width / 2f)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "در حال ارتباط با مرکز مبادله و گمرک...",
            color = CyanNeon,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
            style = TextStyle(
                shadow = Shadow(
                    color = CyanNeon.copy(alpha = 0.6f),
                    blurRadius = 8f
                ),
                letterSpacing = 0.5.sp
            )
        )
    }
}
