package io.github.markyav.drawbox.util

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

/**
 * This implementation will be used until official implementation comes.
 * Source: https://github.com/Kotlin/kotlinx.coroutines/issues/2631#issuecomment-870565860
 */

class DerivedStateFlow<T>(
    private val getValue: () -> T,
    private val flow: Flow<T>
) : StateFlow<T> {

    override val replayCache: List<T>
        get () = listOf(value)

    override val value: T
        get () = getValue()

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        coroutineScope { flow.distinctUntilChanged().stateIn(this).collect(collector) }
    }
}

fun <T1, R> StateFlow<T1>.mapState(transform: (a: T1) -> R): StateFlow<R> {
    return DerivedStateFlow(
        getValue = { transform(this.value) },
        flow = this.map { a -> transform(a) }
    )
}

fun <T1, T2, R> combineStates(flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (a: T1, b: T2) -> R): StateFlow<R> {
    return DerivedStateFlow(
        getValue = { transform(flow.value, flow2.value) },
        flow = combine(flow, flow2) { a, b -> transform(a, b) }
    )
}