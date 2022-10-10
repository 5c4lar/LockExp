import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomSmooth
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.concurrent.thread
import spin.*

//class TASLock :Lock {
//  private var state: AtomicBoolean = AtomicBoolean(false)
//
//  override fun lock() {
//    while (state.getAndSet(true)) {}
//  }
//
//  override fun unlock() {
//    state.set(false)
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//
//}
//
//class TTASLock :Lock {
//  private var state: AtomicBoolean = AtomicBoolean(false)
//
//  override fun lock() {
//    while (true) {
//      while (state.get()) {}
//      if (!state.getAndSet(true)) {
//        return
//      }
//    }
//  }
//
//  override fun unlock() {
//    state.set(false)
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//
//}
//
//class Backoff(val minDelay:Int, val maxDelay: Int) {
//  var limit: Int = minDelay
//
//  @Throws(InterruptedException::class)
//  fun backoff() {
//    val delay = ThreadLocalRandom.current().nextInt(limit)
//    limit = maxDelay.coerceAtMost(2 * limit)
//    Thread.sleep(delay.toLong())
//  }
//}
//
//class BackoffLock(val minDelay:Int, val maxDelay:Int): Lock {
//  private val state: AtomicBoolean = AtomicBoolean(false)
//
//  override fun lock() {
//    val backoff = Backoff(minDelay, maxDelay)
//    while (true) {
//      while(state.get()) {}
//      if (!state.getAndSet(true)) {
//        return;
//      } else {
//        backoff.backoff()
//      }
//    }
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun unlock() {
//    state.set(false)
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//
//}
//
//class ALock(private val capacity:Int) : Lock {
//  @Volatile
//  private var flag: BooleanArray = BooleanArray(capacity)
//  private val mySlotIndex:ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
//  private val tail: AtomicInteger = AtomicInteger(0)
//  private val size: Int = capacity
//  init {
//    flag[0] = true
//  }
//  override fun lock() {
//    val slot = tail.getAndIncrement() % size
//    mySlotIndex.set(slot)
//    while (!flag[slot]) {
//    }
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun unlock() {
//    val slot = mySlotIndex.get()
//    flag[slot] = false
//    flag[(slot + 1) % size] = true
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//
//}
//
//class CLHLock :Lock {
//  class QNode {
//    @Volatile
//    var locked: Boolean = false
//  }
//  private val tail: AtomicReference<QNode> = AtomicReference(QNode())
//  private var myPred: ThreadLocal<QNode> = ThreadLocal.withInitial { null }
//  private var myNode: ThreadLocal<QNode> = ThreadLocal.withInitial { QNode() }
//  override fun lock() {
//    val qnode = myNode.get()
//    qnode.locked = true
//    val pred = tail.getAndSet(qnode);
//    myPred.set(pred)
//    while (pred.locked) {}
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun unlock() {
//    val qnode = myNode.get()
//    qnode.locked = false
//    myNode.set(myPred.get())
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//}
//
//class MCSLock :Lock {
//  class QNode {
//    @Volatile
//    var locked: Boolean = false
//    @Volatile
//    var next: QNode? = null
//  }
//  var tail: AtomicReference<QNode> = AtomicReference(null)
//  var myNode: ThreadLocal<QNode> = ThreadLocal.withInitial { QNode() }
//  override fun lock() {
//    val qnode = myNode.get()
//    val pred = tail.getAndSet(qnode)
//    if (pred != null) {
//      qnode.locked = true
//      pred.next = qnode
//      while (qnode.locked) {}
//    }
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun unlock() {
//    val qnode = myNode.get()
//    if (qnode.next == null) {
//      if (tail.compareAndSet(qnode, null)) {
//        return
//      }
//      while (qnode.next == null) {}
//    }
//    qnode.next!!.locked = false
//    qnode.next = null
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//}
//
//class TOLock : Lock {
//  class QNode {
//    @Volatile
//    var pred: QNode? = null
//  }
//
//  companion object {
//    val AVAILABLE: QNode = QNode()
//  }
//  var tail:AtomicReference<QNode> = AtomicReference(null)
//  var myNode:ThreadLocal<QNode> = ThreadLocal.withInitial { QNode() }
//  override fun lock() {
//    TODO("Not yet implemented")
//  }
//
//  override fun lockInterruptibly() {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(): Boolean {
//    TODO("Not yet implemented")
//  }
//
//  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
//    val startTime = System.currentTimeMillis()
//    val patience = TimeUnit.MILLISECONDS.convert(time, unit)
//    val qnode = QNode()
//    myNode.set(qnode)
//    qnode.pred = null
//    var myPred = tail.getAndSet(qnode)
//    if (myPred == null || myPred.pred == AVAILABLE) {
//      return true
//    }
//    while (System.currentTimeMillis() - startTime < patience) {
//      val predPred = myPred.pred
//      if (predPred == AVAILABLE) {
//        return false
//      } else if (predPred != null) {
//        myPred = predPred
//      }
//    }
//    if (!tail.compareAndSet(qnode, myPred))
//      qnode.pred = myPred
//    return false
//  }
//
//  override fun unlock() {
//    val qnode = myNode.get()
//    if (!tail.compareAndSet(qnode, null))
//      qnode.pred = AVAILABLE
//  }
//
//  override fun newCondition(): Condition {
//    TODO("Not yet implemented")
//  }
//
//}

class Counter(private val lock: Lock, private val limit: Int = 1_000_000) {
  var counter = 0
    private set

  fun reachLimit() {
    var current = counter
    while (current < limit) {
      lock.lock()
      try {
        counter++
        current = counter
      } finally {
        lock.unlock()
      }
    }
  }
}

fun testForThreads(numThreads:Int, lock:Lock, limit:Int = 1_000_000):Long {
  val counter = Counter(lock, limit)
  val start = System.currentTimeMillis()
  val threads = List<Thread>(numThreads) {
    thread {
      counter.reachLimit()
    }
  }
  threads.forEach { it.join() }
  val end = System.currentTimeMillis()
  val totalTime =  end - start
  assert(counter.counter == limit)
  return totalTime
}

fun createPlot(data: Map<String, Any>) : Plot {
  var plot = letsPlot(data) { x="numThreads"; y="time"; color="name" }
  plot += geomPoint {color="name"}
  plot += geomSmooth(method="loess", span=0.5, size=1.0) { color="name"; group="name" }
  return plot
}

fun main() {
  val minThreads = 1
  val maxThreads = 4
  val data = mutableMapOf<String, MutableList<Any>>()
  for (numThreads in minThreads..maxThreads){
    for (lockFactory in listOf<()-> Lock>(
      { ALock(numThreads) },
      { BackoffLock() },
      { CLHLock() },
      { CompositeLock() },
      { CompositeFastPathLock() },
      { HBOLock() },
      { HCLHLock() },
      { MCSLock() },
      { TASLock() },
      { TTASLock() },
      { TOLock() },
    )) {
      repeat(10) {
        ThreadID.reset()
        val lock = lockFactory()
        val name = lock.javaClass.simpleName
        val result = testForThreads(numThreads, lock, 1_000_000)
        data.getOrPut("time") { mutableListOf<Any>() }.add(result)
        data.getOrPut("numThreads") { mutableListOf<Any>() }.add(numThreads)
        data.getOrPut("name") { mutableListOf<Any>() }.add(name)
        println("Test for $numThreads with $name takes $result milliseconds!")
      }
    }
  }
  val fig = createPlot(data)
  ggsave(fig, "plot.png")
}