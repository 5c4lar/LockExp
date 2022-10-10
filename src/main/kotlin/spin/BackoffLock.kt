/*
 * BackoffLock.java
 *
 * Created on January 20, 2006, 11:02 PM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

/**
 * Exponential backoff lock
 * @author Maurice Herlihy
 */
class BackoffLock : Lock {
  private val backoff: Backoff? = null
  private val random = Random()
  private val state = AtomicBoolean(false)
  override fun lock() {
    val backoff = Backoff(MIN_DELAY, MAX_DELAY)
    while (true) {
      while (state.get()) {
      }
      // spin
      if (!state.getAndSet(true)) { // try to acquire lock
        return
      } else {      // backoff on failure
        try {
          backoff.backoff()
        } catch (ex: InterruptedException) {
        }
      }
    }
  }

  override fun unlock() {
    state.set(false)
  }

  // Any class implementing Lock must provide these methods
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
    private const val MIN_DELAY = 32
    private const val MAX_DELAY = 1024
  }
}