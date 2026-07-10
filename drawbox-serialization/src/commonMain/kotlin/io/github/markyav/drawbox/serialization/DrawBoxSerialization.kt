package io.github.markyav.drawbox.serialization

import io.github.markyav.drawbox.model.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("NormPoint")
internal data class NormPointDto(val x: Float, val y: Float) {
    fun toModel() = NormPoint(x, y)
}

internal fun NormPoint.toDto() = NormPointDto(x, y)

@Serializable
@SerialName("FillSpan")
internal data class FillSpanDto(val lx: Int, val rx: Int, val y: Int) {
    fun toModel() = FillSpan(lx, rx, y)
}

internal fun FillSpan.toDto() = FillSpanDto(lx, rx, y)

@Serializable
internal sealed class ActionDto {
    abstract fun toModel(): Action
}

@Serializable
@SerialName("BrushAction")
internal data class BrushActionDto(
    val id: String,
    val color: Long,
    val strokeWidth: Float,
    val points: List<NormPointDto>
) : ActionDto() {
    override fun toModel() = BrushAction(id, color, strokeWidth, points.map { it.toModel() })
}

@Serializable
@SerialName("EraserAction")
internal data class EraserActionDto(
    val id: String,
    val strokeWidth: Float,
    val points: List<NormPointDto>
) : ActionDto() {
    override fun toModel() = EraserAction(id, strokeWidth, points.map { it.toModel() })
}

@Serializable
@SerialName("RemoveAction")
internal data class RemoveActionDto(
    val id: String,
    val removedActionIds: List<String>
) : ActionDto() {
    override fun toModel() = RemoveAction(id, removedActionIds)
}

@Serializable
@SerialName("FillAction")
internal data class FillActionDto(
    val id: String,
    val color: Long,
    val tolerance: Float,
    val point: NormPointDto,
    val spans: List<FillSpanDto>
) : ActionDto() {
    override fun toModel() = FillAction(id, color, tolerance, point.toModel(), spans.map { it.toModel() })
}

internal fun Action.toDto(): ActionDto = when (this) {
    is BrushAction -> BrushActionDto(id, color, strokeWidth, points.map { it.toDto() })
    is EraserAction -> EraserActionDto(id, strokeWidth, points.map { it.toDto() })
    is RemoveAction -> RemoveActionDto(id, removedActionIds)
    is FillAction -> FillActionDto(id, color, tolerance, point.toDto(), spans.map { it.toDto() })
    else -> throw IllegalArgumentException("Unknown Action type: ${this::class.simpleName}")
}

@Serializable
internal data class ActionHistoryDto(
    val actions: List<ActionDto>
) {
    fun toModel() = ActionHistory(actions.map { it.toModel() })
}

internal fun ActionHistory.toDto() = ActionHistoryDto(actions.map { it.toDto() })

private val drawBoxSerializersModule = SerializersModule {
    polymorphic(ActionDto::class) {
        subclass(BrushActionDto::class)
        subclass(EraserActionDto::class)
        subclass(RemoveActionDto::class)
        subclass(FillActionDto::class)
    }
}

private val drawBoxJson = Json {
    serializersModule = drawBoxSerializersModule
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

/**
 * Converts an [ActionHistory] to a JSON string.
 */
fun encodeActionHistory(history: ActionHistory): String {
    return drawBoxJson.encodeToString(history.toDto())
}

/**
 * Parses a JSON string back into an [ActionHistory].
 * Throws SerializationException if the JSON is malformed.
 */
fun decodeActionHistory(json: String): ActionHistory {
    return drawBoxJson.decodeFromString<ActionHistoryDto>(json).toModel()
}
