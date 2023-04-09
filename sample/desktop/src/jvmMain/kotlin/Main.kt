import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.markyav.drawbox.box.DrawBox
import io.github.markyav.drawbox.controller.DrawBoxBackground
import io.github.markyav.drawbox.controller.DrawBoxSubscription
import io.github.markyav.drawbox.controller.DrawController

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val controller = remember { DrawController() }
        val coroutineSubscription = rememberCoroutineScope()
        val bitmap by remember { controller.getBitmap(250, DrawBoxSubscription.DynamicUpdate, coroutineSubscription) }.collectAsState()
        val bitmapFinishDrawingUpdate by remember { controller.getBitmap(250, DrawBoxSubscription.FinishDrawingUpdate, coroutineSubscription) }.collectAsState()

        LaunchedEffect(Unit) {
            controller.background.value = DrawBoxBackground.ColourBackground(color = Color.Blue, alpha = 0.15f)
            controller.canvasOpacity.value = 0.5f
        }

        Row {
            Column(modifier = Modifier.weight(2f, false)) {
                Row {
                    val enableUndo by remember { derivedStateOf { controller.undoCount > 0 } }
                    val enableRedo by remember { derivedStateOf { controller.redoCount > 0 } }
                    IconButton(onClick = controller::undo, enabled = enableUndo) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "undo")
                    }
                    IconButton(onClick = controller::redo, enabled = enableRedo) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "redo")
                    }
                    IconButton(onClick = controller::reset, enabled = enableUndo || enableRedo) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "reset")
                    }
                }
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Stroke width")
                        Slider(
                            value = controller.strokeWidth.value,
                            onValueChange = { controller.strokeWidth.value = it },
                            valueRange = 1f..100f
                        )
                    }
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Canvas opacity")
                        Slider(
                            value = controller.canvasOpacity.value,
                            onValueChange = { controller.canvasOpacity.value = it },
                            valueRange = 0f..1f
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    Column(modifier = Modifier.weight(2f, true)) {
                        Text("Color")
                        Row {
                            TextButton(onClick = { controller.color.value = Color.Red }) {
                                Text("Red")
                            }
                            TextButton(onClick = { controller.color.value = Color.Green }) {
                                Text("Green")
                            }
                            TextButton(onClick = { controller.color.value = Color.Yellow }) {
                                Text("Yellow")
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(2f, false)) {
                        Text("Background opacity")
                        Slider(
                            value = (controller.background as? DrawBoxBackground.ColourBackground)?.alpha ?: 0f,
                            onValueChange = { controller.background.value = DrawBoxBackground.ColourBackground(color = Color.Blue, alpha = it) },
                            valueRange = 0f..1f
                        )
                    }
                }
                DrawBox(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .padding(100.dp)
                        .border(width = 1.dp, color = Color.Blue),
                )
            }
            Column(modifier = Modifier.weight(1f, false)) {
                Text("DynamicUpdate:")
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmap,
                    contentDescription = "drawn bitmap",
                    modifier = Modifier.size(200.dp).border(width = 1.dp, color = Color.Red),
                )

                Spacer(modifier = Modifier.height(50.dp))

                Text("FinishDrawingUpdate:")
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmapFinishDrawingUpdate,
                    contentDescription = "drawn bitmap",
                    modifier = Modifier.size(200.dp).border(width = 1.dp, color = Color.Red),
                )
            }
        }
    }
}
