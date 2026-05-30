import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.markyav.drawbox.box.DrawBox
import io.github.markyav.drawbox.controller.DrawController

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        val controller = remember { DrawController(logicalSize = IntSize(1000, 1000)) }

        val canUndo by controller.canUndo.collectAsState()
        val canRedo by controller.canRedo.collectAsState()
        val strokeWidth by controller.strokeWidth.collectAsState()
        val opacity by controller.opacity.collectAsState()

        Column {
            Row {
                IconButton(onClick = controller::undo, enabled = canUndo) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "undo")
                }
                IconButton(onClick = controller::redo, enabled = canRedo) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "redo")
                }
                IconButton(onClick = controller::reset, enabled = canUndo || canRedo) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "reset")
                }
            }
            Row(modifier = Modifier.padding(end = 8.dp)) {
                Column(modifier = Modifier.weight(2f, false)) {
                    Text("Stroke width")
                    Slider(
                        value = strokeWidth,
                        onValueChange = { controller.strokeWidth.value = it },
                        valueRange = 1f..100f
                    )
                }
                Column(modifier = Modifier.weight(2f, false)) {
                    Text("Stroke opacity")
                    Slider(
                        value = opacity,
                        onValueChange = { controller.opacity.value = it },
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
            }
            DrawBox(
                controller = controller,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(100.dp)
                    .background(Color.Blue.copy(alpha = 0.15f))
                    .border(width = 1.dp, color = Color.Blue),
            )
        }
    }
}
