package io.github.markyav.drawbox.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface Action {
    val id: String
}

@OptIn(ExperimentalUuidApi::class)
data class BrushAction(
    override val id: String = Uuid.random().toString(),
    val color: Long,
    val strokeWidth: Float,
    val points: List<NormPoint>
) : Action

@OptIn(ExperimentalUuidApi::class)
data class EraserAction(
    override val id: String = Uuid.random().toString(),
    val strokeWidth: Float,
    val points: List<NormPoint>
) : Action

@OptIn(ExperimentalUuidApi::class)
data class RemoveAction(
    override val id: String = Uuid.random().toString(),
    val removedActionIds: List<String>
) : Action

@OptIn(ExperimentalUuidApi::class)
data class FillAction(
    override val id: String = Uuid.random().toString(),
    val color: Long,
    val tolerance: Float,
    val point: NormPoint,
    val spans: List<FillSpan> = emptyList()
) : Action

data class FillSpan(val lx: Int, val rx: Int, val y: Int)
