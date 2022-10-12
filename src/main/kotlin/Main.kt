import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.delimiter
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.geom.geomSmooth
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import spin.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock

//import javaspin.*

class Counter(private val lock: Lock, private val limit: Int = 1_000_000) {
  var counter = 0
    private set

  fun reachLimit() {
    while (counter < limit) {
      lock.lock()
      try {
        if (counter < limit) {
          counter++
        }
      } finally {
        lock.unlock()
      }
    }
  }
}

fun testForThreads(numThreads: Int, lock: Lock, limit: Int = 1_000_000, executor: ExecutorService): Long {
  val counter = Counter(lock, limit)
  val start = System.currentTimeMillis()
  val barrier = CyclicBarrier(numThreads + 1)
  for (i in 0 until numThreads) {
    executor.submit {
      counter.reachLimit()
      barrier.await()
      (lock as HCLHLock).let {
        it.currNode.get()?.isSuccessorMustWait = false
      }
    }
  }
  barrier.await()
  val end = System.currentTimeMillis()
  val totalTime = end - start
  assert(counter.counter == limit)
  return totalTime
}

fun createPlot(data: Map<String, Any>): Plot {
  var plot = letsPlot(data) { x = "numThreads"; y = "time"; color = "name" }
  plot += geomPoint { color = "name" }
  plot += geomSmooth(method = "loess", span = 0.5, size = 1.0, ymin = 0) { color = "name"; group = "name" }
  return plot
}

fun main(args: Array<String>) {

  val lockList = listOf<(Int) -> Lock>(
    { ALock(it) },
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
  )
  val lockNames = listOf(
    "ALock",
    "BackoffLock",
    "CLHLock",
    "CompositeLock",
    "CompositeFastPathLock",
    "HBOLock",
    "HCLHLock",
    "MCSLock",
    "TASLock",
    "TTASLock",
    "TOLock"
  )
  val lockNameFactoryMap = lockNames.zip(lockList).toMap()
  val parser = ArgParser("Locks Experiments")
  val minThreads =
    parser.option(ArgType.Int, fullName = "min", shortName = "m", description = "Min number of threads").default(1)
  val maxThreads =
    parser.option(ArgType.Int, fullName = "max", shortName = "M", description = "Max number of threads").default(8)
  val step = parser.option(ArgType.Int, fullName = "step", shortName = "s", description = "Step for threads").default(1)
  val limit = parser.option(ArgType.Int, fullName = "limit", shortName = "l", description = "Limit for counter")
    .default(1_000_000)
  val numTrials =
    parser.option(ArgType.Int, fullName = "trials", shortName = "t", description = "Number of trials").default(10)
  val locks = parser.option(
    ArgType.Choice<String>(lockNames, { it }, { it }),
    fullName = "locks",
    shortName = "L",
    description = "Locks to test"
  ).delimiter(",").default(lockNames)
  val output =
    parser.option(ArgType.String, fullName = "output", shortName = "o", description = "Output file").default("plot")
  val outputFormat = parser.option(
    ArgType.Choice<String>(listOf("png", "html", "svg", "jpeg", "tiff"), { it }, { it }),
    fullName = "format",
    shortName = "f",
    description = "Output format"
  ).default("html")
  parser.parse(args)
  val data = mutableMapOf<String, MutableList<Any>>()
  for (numThreads in minThreads.value..maxThreads.value step step.value) {
    val executor = Executors.newFixedThreadPool(numThreads)
    for (name in locks.value) {
      repeat(numTrials.value) {
        ThreadID.reset()
        val lock = lockNameFactoryMap[name]!!.invoke(numThreads)
        val result = testForThreads(numThreads, lock, limit.value, executor)
        data.getOrPut("time") { mutableListOf() }.add(result)
        data.getOrPut("numThreads") { mutableListOf() }.add(numThreads)
        data.getOrPut("name") { mutableListOf() }.add(name)
        println("Test for $numThreads threads with $name takes $result milliseconds!")
      }
    }
    executor.shutdownNow()
  }
  println("Finish processing!")
  val fig = createPlot(data)
  ggsave(fig, "${output.value}.${outputFormat.value}")
  println("Finish plotting!")
}