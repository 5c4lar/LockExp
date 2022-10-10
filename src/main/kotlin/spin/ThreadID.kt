/*
 * ThreadID.java
 *
 * Created on November 7, 2006, 5:27 PM
 *
 * From "The Art of Multiprocessor Programming",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

/**
 * Illustrates use of thread-local storage. Test by running main().
 * @author Maurice Herlihy
 */
object ThreadID {
  /**
   * The next thread ID to be assigned
   */
  @Volatile
  private var nextID = 0

  /**
   * My thread-local ID.
   */
  private val threadID = ThreadLocalID()
  fun get(): Int {
    return threadID.get()
  }

  /**
   * When running multiple tests, reset thread id state
   */
  fun reset() {
    nextID = 0
  }

  fun set(value: Int) {
    threadID.set(value)
  }

  val cluster: Int
    get() = threadID.get() / 2

  private class ThreadLocalID : ThreadLocal<Int>() {
    @Synchronized
    override fun initialValue(): Int {
      return nextID++
    }
  }
}