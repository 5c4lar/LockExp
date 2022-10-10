/*
 * CLHLock.java
 *
 * Created on January 20, 2006, 11:35 PM
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
 * Craig-Hagersten-Landin Lock
 * @author Maurice Herlihy
 */
class CLHLock : Lock {
  // most recent lock holder
  var tail: AtomicReference<QNode>

  // thread-local variables
  var myNode: ThreadLocal<QNode>
  var myPred: ThreadLocal<QNode?>

  /**
   * Constructor
   */
  init {
    tail = AtomicReference(QNode())
    // initialize thread-local variables
    myNode = object : ThreadLocal<QNode>() {
      override fun initialValue(): QNode {
        return QNode()
      }
    }
    myPred = object : ThreadLocal<QNode?>() {
      override fun initialValue(): QNode? {
        return null
      }
    }
  }

  override fun lock() {
    val qnode = myNode.get() // use my node
    qnode.locked = true // announce start
    // Make me the new tail, and find my predecessor
    val pred = tail.getAndSet(qnode)
    myPred.set(pred) // remember predecessor
    while (pred.locked) {
    } // spin
  }

  override fun unlock() {
    val qnode = myNode.get() // use my node
    qnode.locked = false // announce finish
    myNode.set(myPred.get()) // reuse predecessor
  }

  // any class that implements lock must provide these methods
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
  }
}