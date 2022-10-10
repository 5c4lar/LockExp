/*
 * MCSLock.java
 *
 * Created on January 20, 2006, 11:41 PM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

/**
 * Mellor-Crummy Scott Lock
 * @author Maurice Herlihy
 */
class MCSLock : Lock {
  var queue: AtomicReference<QNode?>
  var myNode: ThreadLocal<QNode>

  init {
    queue = AtomicReference(null)
    // initialize thread-local variable
    myNode = object : ThreadLocal<QNode>() {
      override fun initialValue(): QNode {
        return QNode()
      }
    }
  }

  override fun lock() {
    val qnode = myNode.get()
    val pred = queue.getAndSet(qnode)
    if (pred != null) {
      qnode.locked = true
      pred.next = qnode
      while (qnode.locked) {
      } // spin
    }
  }

  override fun unlock() {
    val qnode = myNode.get()
    if (qnode.next == null) {
      if (queue.compareAndSet(qnode, null)) return
      while (qnode.next == null) {
      } // spin
    }
    qnode.next!!.locked = false
    qnode.next = null
  }

  // any class implementing Lock must provide these methods
  override fun newCondition(): Condition {
    throw UnsupportedOperationException()
  }

  @Throws(InterruptedException::class)
  override fun tryLock(
    time: Long,
    unit: TimeUnit
  ): Boolean {
    throw UnsupportedOperationException()
  }

  override fun tryLock(): Boolean {
    throw UnsupportedOperationException()
  }

  @Throws(InterruptedException::class)
  override fun lockInterruptibly() {
    throw UnsupportedOperationException()
  }

  class QNode {
    // Queue node inner class
    var locked = false
    var next: QNode? = null
  }
}