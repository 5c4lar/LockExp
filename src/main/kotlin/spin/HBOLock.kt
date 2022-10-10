/*
 * HBOLock.java
 *
 * Created on November 12, 2006, 8:58 PM
 *
 * From "The Art of Multiprocessor Programming",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

class HBOLock : Lock {
  var state: AtomicInteger

  /**
   * Hierarchical backoff lock
   * @author Maurice Herlihy
   */
  init {
    state = AtomicInteger(FREE)
  }

  override fun lock() {
    val myCluster = ThreadID.cluster
    val localBackoff = Backoff(LOCAL_MIN_DELAY, LOCAL_MAX_DELAY)
    val remoteBackoff = Backoff(REMOTE_MIN_DELAY, REMOTE_MAX_DELAY)
    while (true) {
      if (state.compareAndSet(FREE, myCluster)) {
        return
      }
      val lockState = state.get()
      try {
        if (lockState == myCluster) {
          localBackoff.backoff()
        } else {
          remoteBackoff.backoff()
        }
      } catch (ex: InterruptedException) {
      }
    }
  }

  override fun unlock() {
    state.set(FREE)
  }

  // Any class that implents Lock must provide these methods.
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

  companion object {
    private const val LOCAL_MIN_DELAY = 8
    private const val LOCAL_MAX_DELAY = 256
    private const val REMOTE_MIN_DELAY = 256
    private const val REMOTE_MAX_DELAY = 1024
    private const val FREE = -1
  }
}