package flashcards

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.properties.ReadOnlyProperty

fun <T> suspendLazy(supplier: suspend () -> T) = SuspendLazy(supplier)
class SuspendLazy<T>(private val supplier: suspend () -> T) {
    private val lock = Mutex()
    private var value: T? = null
    private var init = false

    @Suppress("UNCHECKED_CAST")
    suspend fun getValue(): T {
        if (init) return value as T

        return lock.withLock {
            if (init) return value as T
            val result = supplier()
            value = result
            init = true
            result
        }
    }
}
