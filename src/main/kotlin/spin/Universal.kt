package spin

import java.util.concurrent.atomic.AtomicReference

interface Universal<T> {
    fun apply(invoc: T.() -> Unit): T
}