package io.github.markyav.drawbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.DrawTool

@Composable
fun DrawBoxControls(controller: DrawController, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp)) {
        DrawBoxToolPicker(controller)
        Spacer(modifier = Modifier.height(16.dp))
        DrawBoxColorPicker(controller)
        Spacer(modifier = Modifier.height(16.dp))
        DrawBoxStrokeWidthPicker(controller)
    }
}

@Composable
fun DrawBoxToolPicker(controller: DrawController, modifier: Modifier = Modifier) {
    val settings by controller.settings.collectAsState()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawTool.entries.forEach { tool ->
            val isSelected = settings.tool == tool
            Button(
                onClick = { controller.setTool(tool) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                    contentColor = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Text(tool.name)
            }
        }
    }
}

@Composable
fun DrawBoxColorPicker(controller: DrawController, modifier: Modifier = Modifier) {
    val settings by controller.settings.collectAsState()
    val defaultColors = listOf(
        0xFFFF0000, // Red
        0xFF00FF00, // Green
        0xFF0000FF, // Blue
        0xFFFFFF00, // Yellow
        0xFFFF00FF, // Magenta
        0xFF00FFFF, // Cyan
        0xFF000000, // Black
        0xFFFFFFFF  // White
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        defaultColors.forEach { colorLong ->
            val color = Color(colorLong)
            val isSelected = settings.color == colorLong

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colors.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { controller.setColor(colorLong) }
            )
        }
    }
}

@Composable
fun DrawBoxStrokeWidthPicker(controller: DrawController, modifier: Modifier = Modifier) {
    val settings by controller.settings.collectAsState()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Size: ${settings.strokeWidth.toInt()}", modifier = Modifier.width(64.dp))
        Slider(
            value = settings.strokeWidth,
            onValueChange = { controller.setStrokeWidth(it) },
            valueRange = 1f..100f,
            modifier = Modifier.weight(1f)
        )
    }
}
