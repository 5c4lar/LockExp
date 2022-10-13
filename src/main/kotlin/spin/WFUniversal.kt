package spin

import java.util.concurrent.atomic.AtomicReference

class WFUniversal<T>(private val n: Int, val objInit: () -> T): Universal<T> {
    class Node<T>( // method name and args
        var invoc: T.() -> Unit
    ) {
        var next // the next node
                : AtomicReference<Node<T>> = AtomicReference(null)
        var seq // sequence number
                : Int = 0

        companion object {
            fun<T> max(array: Array<Node<T>>): Node<T> {
                var max = array[0]
                for (i in 1 until array.size) {
                    if (max.seq < array[i].seq) {
                        max = array[i]
                    }
                }
                return max
            }
        }
    }
    private val announce: Array<Node<T>> = Array(n) { Node { } }
    private val head: Array<Node<T>> = Array(n) { Node() {} }
    private val tail: Node<T> = Node {}

    init {
        tail.seq = 1
        for (i in 0 until n) {
            head[i] = tail
        }
    }

    override fun apply(invoc: T.() -> Unit): T {
        val i = ThreadID.get()
        announce[i] = Node(invoc)
        head[i] = Node.max(head)
        while (announce[i].seq === 0) {
            val before: Node<T> = head[i]
            val help: Node<T> = announce[(before.seq + 1) % n]
            val prefer: Node<T> = if (help.seq == 0) {
                help
            } else {
                announce[i]
            }
            before.next.compareAndSet(null, prefer)
            val after = before.next.get()
            after.seq = before.seq + 1
            head[i] = after
        }
        val myObject = objInit()
        var current: Node<T> = tail.next.get()
        while (current !== announce[i]) {
            myObject.apply(current.invoc)
            current = current.next.get()
        }
        return myObject.apply(current.invoc)
    }
}