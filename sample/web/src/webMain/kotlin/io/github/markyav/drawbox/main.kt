package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import io.github.markyav.drawbox.box.DrawBox
import io.github.markyav.drawbox.controller.DrawController
import io.github.markyav.drawbox.model.ImageMode

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        val controller = remember { DrawController(IntSize(1000, 1000)) }
        val bitmap by controller.image(ImageMode.Live).collectAsState()
        val bitmapFinishDrawingUpdate by controller.image(ImageMode.Committed).collectAsState()

        val enableUndo by controller.canUndo.collectAsState()
        val enableRedo by controller.canRedo.collectAsState()

        val settings by controller.settings.collectAsState()
        var canvasOpacity by remember { mutableStateOf(0.5f) }
        var backgroundAlpha by remember { mutableStateOf(0.15f) }

        Row {
            Column(modifier = Modifier.weight(2f, false)) {
                Row {
                    IconButton(onClick = controller::undo, enabled = enableUndo) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "undo")
                    }
                    IconButton(onClick = controller::redo, enabled = enableRedo) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = "redo")
                    }
                    IconButton(onClick = controller::clear, enabled = enableUndo || enableRedo) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "clear")
                    }
                }
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Stroke width")
                        Slider(
                            value = settings.strokeWidth,
                            onValueChange = { controller.setStrokeWidth(it) },
                            valueRange = 1f..100f
                        )
                    }
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Canvas opacity")
                        Slider(
                            value = canvasOpacity,
                            onValueChange = { canvasOpacity = it },
                            valueRange = 0f..1f
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Column(modifier = Modifier.weight(1f, true)) {
                        Text("Tool")
                        Row {
                            TextButton(onClick = { controller.setTool(io.github.markyav.drawbox.model.DrawTool.Brush) }) {
                                Text("Brush")
                            }
                            TextButton(onClick = { controller.setTool(io.github.markyav.drawbox.model.DrawTool.Eraser) }) {
                                Text("Eraser")
                            }
                            TextButton(onClick = { controller.setTool(io.github.markyav.drawbox.model.DrawTool.ActionEraser) }) {
                                Text("ActionEraser")
                            }
                            TextButton(onClick = { controller.setTool(io.github.markyav.drawbox.model.DrawTool.ColorFill) }) {
                                Text("Fill")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Column(modifier = Modifier.weight(2f, true)) {
                        Text("Color")
                        Row {
                            TextButton(onClick = { controller.setColor(Color.Red.toArgb().toLong()) }) {
                                Text("Red")
                            }
                            TextButton(onClick = { controller.setColor(Color.Green.toArgb().toLong()) }) {
                                Text("Green")
                            }
                            TextButton(onClick = { controller.setColor(Color.Yellow.toArgb().toLong()) }) {
                                Text("Yellow")
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Background opacity")
                        Slider(
                            value = backgroundAlpha,
                            onValueChange = { backgroundAlpha = it },
                            valueRange = 0f..1f
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).padding(100.dp)) {
                    // Simulating the background functionality locally
                    Spacer(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha).border(1.dp, Color.Blue))

                    DrawBox(
                        controller = controller,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(canvasOpacity)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f, false)) {
                Text("Live:")
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmap,
                    contentDescription = "live bitmap",
                    modifier = Modifier.size(200.dp).border(width = 1.dp, color = Color.Red),
                )

                Spacer(modifier = Modifier.height(50.dp))

                Text("Committed:")
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmapFinishDrawingUpdate,
                    contentDescription = "committed bitmap",
                    modifier = Modifier.size(200.dp).border(width = 1.dp, color = Color.Red),
                )
            }
        }
    }
}