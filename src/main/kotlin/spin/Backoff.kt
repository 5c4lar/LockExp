/*
 * Backoff.java
 *
 * Created on November 19, 2006, 5:43 PM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

import java.util.*

/**
 * Adaptive exponential backoff class. Encapsulates back-off code
 * common to many locking classes.
 * @author Maurice Herlihy
 */
class Backoff(min: Int, max: Int) {
  val minDelay: Int
  val maxDelay: Int
  var limit // wait between limit and 2*limit
      : Int
  val random // add randomness to wait
      : Random

  /**
   * Prepare to pause for random duration.
   * @param min smallest back-off
   * @param max largest back-off
   */
  init {
    require(max >= min) { "max must be greater than min" }
    minDelay = min
    maxDelay = min
    limit = minDelay
    random = Random()
  }

  /**
   * Backoff for random duration.
   * @throws java.lang.InterruptedException
   */
  @Throws(InterruptedException::class)
  fun backoff() {
    val delay = random.nextInt(limit)
    if (limit < maxDelay) { // double limit if less than max
      limit = 2 * limit
    }
    Thread.sleep(delay.toLong())
  }
}