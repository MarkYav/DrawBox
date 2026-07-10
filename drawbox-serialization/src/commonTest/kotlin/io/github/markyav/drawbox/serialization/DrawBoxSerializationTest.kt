package io.github.markyav.drawbox.serialization

import io.github.markyav.drawbox.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawBoxSerializationTest {

    @Test
    fun testEncodeAndDecodeActionHistory() {
        val originalHistory = ActionHistory(
            actions = listOf(
                BrushAction(
                    id = "brush-1",
                    color = 0xFFFF0000,
                    strokeWidth = 10f,
                    points = listOf(NormPoint(0.1f, 0.2f), NormPoint(0.3f, 0.4f))
                ),
                EraserAction(
                    id = "eraser-1",
                    strokeWidth = 20f,
                    points = listOf(NormPoint(0.5f, 0.6f))
                ),
                RemoveAction(
                    id = "remove-1",
                    removedActionIds = listOf("brush-1")
                ),
                FillAction(
                    id = "fill-1",
                    color = 0xFF00FF00,
                    tolerance = 32f,
                    point = NormPoint(0.8f, 0.9f),
                    spans = listOf(FillSpan(10, 20, 15))
                )
            )
        )

        val jsonString = encodeActionHistory(originalHistory)
        
        // Assert some known keys exist in the string to ensure it serialized properly
        assertTrue(jsonString.contains("BrushAction"))
        assertTrue(jsonString.contains("EraserAction"))
        assertTrue(jsonString.contains("RemoveAction"))
        assertTrue(jsonString.contains("FillAction"))
        
        val decodedHistory = decodeActionHistory(jsonString)

        assertEquals(originalHistory, decodedHistory)
    }
}
