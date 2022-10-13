package spin

import java.util.concurrent.atomic.AtomicReference

class WFUniversalBack<T>(private val n: Int, val objInit: () -> T, val copy: (T) -> T): Universal<T> {
    class Node<T>( // method name and args
        var invoc: T.() -> Unit
    ) {
        var next // the next node
                : AtomicReference<Node<T>> = AtomicReference(null)
        var seq // sequence number
                : Int = 0
        var pred: Node<T>? = null

        var state: AtomicReference<T> = AtomicReference(null)

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
            after.pred = before
            after.seq = before.seq + 1
            head[i] = after
        }
        val prefer = announce[i]
        var current: Node<T> = prefer
        while (current.pred != tail && current.state.get() == null) {
            current = current.pred!!
        }
        var myObject: T
        if (current.pred == tail) {
            myObject = objInit()
            current = tail.next.get()
        } else {
            myObject = copy(current.state.get()!!)
        }
        while (current !== prefer) {
            myObject.apply(current.invoc)
            current.state.compareAndSet(null, myObject)
            myObject = copy(current.state.get()!!)
            current = current.next.get()
        }
        myObject.apply(current.invoc)
        current.state.compareAndSet(null, myObject)
        return myObject
    }
}