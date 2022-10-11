/*
 * HCLHLock.java
 *
 * Created on April 13, 2006, 9:28 PM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2006 Elsevier Inc. All rights reserved.
 */
package spin

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

/**
 * Hierarchical CLH Lock
 * @author Maurice Herlihy
 */
class HCLHLock : Lock {
  /**
   * List of local queues, one per cluster
   */
  @Volatile
  var localQueues: MutableList<AtomicReference<QNode?>> = ArrayList(MAX_CLUSTERS)

  /**
   * single global queue
   */
  @Volatile
  var globalQueue: AtomicReference<QNode?>

  /**
   * My current QNode
   */
  @Volatile
  var currNode: ThreadLocal<QNode> = ThreadLocal.withInitial { QNode() }

  /**
   * My predecessor QNode
   */
  @Volatile
  var predNode: ThreadLocal<QNode?> = ThreadLocal.withInitial { null }

  /** Creates a new instance of HCLHLock  */
  init {
    for (i in 0 until MAX_CLUSTERS) {
      localQueues.add(AtomicReference())
    }
    val head = QNode()
    globalQueue = AtomicReference(head)
  }

  override fun lock() {
    val myNode = currNode.get()
    // Original code has a bug here! The reset done in QNode.unlock sets a
    // wrong value for clusterID, when used by another cluster, this will
    // be problematic. The fix is to set the clusterID in the lock method.
    myNode.clusterID = ThreadID.cluster
    val localQueue = localQueues[ThreadID.cluster]

    // splice my QNode into local queue
    var myPred: QNode?
    do {
//      myPred = localQueue.compareAndExchange(myNode, null)
      myPred = localQueue.get()
    } while (!localQueue.compareAndSet(myPred, myNode))
    if (myPred != null) {
      val iOwnLock = myPred.waitForGrantOrClusterMaster()
      if (iOwnLock) {
        // I have the lock. Save QNode just released by previous leader
        predNode.set(myPred)
        return
      }
    }
    // At this point I am the cluster master.
    // Splice local queue into global queue.
    var localTail: QNode?
    do {
      myPred = globalQueue.get()
      localTail = localQueue.get()
//      myPred = globalQueue.compareAndExchange(myNode, localTail)
    } while (!globalQueue.compareAndSet(myPred, localTail))
    // inform successor it is the new master
    localTail!!.isTailWhenSpliced = true
    // wait for predecessor to release lock
    while (myPred!!.isSuccessorMustWait) {
    }
    // I have the lock. Save QNode just released by previous leader
    predNode.set(myPred)
    return
  }

  override fun unlock() {
    val myNode = currNode.get()
    val node = predNode.get()
    node!!.unlock()
    // promote pred node to current
    currNode.set(node)
    predNode.set(null)
    myNode!!.isSuccessorMustWait = false
  }

  class QNode {
    @Volatile
    var state: AtomicInteger = AtomicInteger(0)

    fun waitForGrantOrClusterMaster(): Boolean {
      val myCluster = ThreadID.cluster
      while (true) {
        if (clusterID == myCluster &&
          !isTailWhenSpliced &&
          !isSuccessorMustWait
        ) {
          return true
        } else if (clusterID != myCluster || isTailWhenSpliced) {
          return false
        }
      }
    }

    fun unlock() {
      var oldState: Int
      var newState = ThreadID.cluster
      // successorMustWait = true;
      newState = newState or SMW_MASK
      // tailWhenSpliced = false;
      newState = newState and TWS_MASK.inv()
      do {
        oldState = state.get()
      } while (!state.compareAndSet(oldState, newState))
    }

    var clusterID: Int
      get() = state.get() and CLUSTER_MASK
      set(clusterID) {
        var oldState: Int
        var newState: Int
        do {
          oldState = state.get()
          newState = oldState and CLUSTER_MASK.inv() or clusterID
        } while (!state.compareAndSet(oldState, newState))
      }
    var isSuccessorMustWait: Boolean
      get() = state.get() and SMW_MASK != 0
      set(successorMustWait) {
        var oldState: Int
        var newState: Int
        do {
          oldState = state.get()
          newState = if (successorMustWait) {
            oldState or SMW_MASK
          } else {
            oldState and SMW_MASK.inv()
          }
        } while (!state.compareAndSet(oldState, newState))
      }
    var isTailWhenSpliced: Boolean
      get() = state.get() and TWS_MASK != 0
      set(tailWhenSpliced) {
        var oldState: Int
        var newState: Int
        do {
          oldState = state.get()
          newState = if (tailWhenSpliced) {
            oldState or TWS_MASK
          } else {
            oldState and TWS_MASK.inv()
          }
        } while (!state.compareAndSet(oldState, newState))
      }

    companion object {
      // private boolean tailWhenSpliced;
      private const val TWS_MASK = -0x80000000

      // private boolean successorMustWait= false;
      private const val SMW_MASK = 0x40000000

      // private int clusterID;
      private const val CLUSTER_MASK = 0x3FFFFFFF
    }
  }

  // superfluous declarations needed to satisfy lock interface
  @Throws(InterruptedException::class)
  override fun lockInterruptibly() {
    throw UnsupportedOperationException()
  }

  override fun tryLock(): Boolean {
    throw UnsupportedOperationException()
  }

  @Throws(InterruptedException::class)
  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
    throw UnsupportedOperationException()
  }

  override fun newCondition(): Condition {
    throw UnsupportedOperationException()
  }

  companion object {
    /**
     * Max number of clusters
     */
    const val MAX_CLUSTERS = 32
  }
}